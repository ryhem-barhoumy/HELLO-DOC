import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { Subject, of, throwError } from 'rxjs';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ApprovalEmailComponent } from './approval-email.component';
import { PortalComponent } from '../portal/portal.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

function setup() {
  (globalThis as any).window={innerWidth:1280,matchMedia:()=>({matches:false})};
  const posts: Array<{path:string;stream:Subject<any>}> = [];
  const gets:string[]=[];
  let response:any=of({status:'SENT',message:'Courriel transmis.'});
  const injector=createEnvironmentInjector([
    {provide:FormBuilder,useValue:new FormBuilder()},
    {provide:ActivatedRoute,useValue:{snapshot:{data:{roles:['ADMIN']},paramMap:convertToParamMap({area:'approbations'})}}},
    {provide:Router,useValue:{navigate:()=>Promise.resolve(true)}},
    {provide:Title,useValue:{setTitle:()=>{}}},
    {provide:AuthService,useValue:{user:()=>({firstName:'Admin',lastName:'Test',role:'ADMIN'})}},
    {provide:ApiService,useValue:{
      get:(path:string)=>{gets.push(path);return response;},
      post:(path:string)=>{const stream=new Subject();posts.push({path,stream});return stream;}
    }}
  ],null as any);
  return {injector,posts,gets,setResponse:(value:any)=>response=value,close:()=>injector.destroy()};
}
const pause=()=>new Promise(resolve=>setTimeout(resolve,30));

test('double approval is blocked and successful activation does not trigger a failing reload',()=>{
  const f=setup();try {
    const portal=runInInjectionContext(f.injector,()=>new PortalComponent());
    const record={id:'application-1',fullName:'Médecin Test'};
    portal.records=[record];portal.totalElements=1;portal.error='Ancienne erreur';
    portal.decide(record,true);portal.decide(record,true);
    assert.equal(f.posts.length,1);assert.equal(portal.busy,true);assert.equal(portal.error,'');
    f.posts[0].stream.next({status:'APPROVED',approvalEmailStatus:'QUEUED'});
    assert.equal(portal.busy,false);assert.equal(portal.error,'');assert.equal(portal.records.length,0);
    assert.match(portal.notice,/compte médecin est actif/);assert.equal(portal.approvalOutcome?.applicationId,record.id);
    assert.equal(f.gets.length,0);
  } finally {f.close();}
});
test('approval refusal from server retains the candidate and allows a retry',()=>{
  const f=setup();try {
    const portal=runInInjectionContext(f.injector,()=>new PortalComponent());const record={id:'application-1'};portal.records=[record];
    portal.decide(record,true);f.posts[0].stream.error({message:'Justificatif absent'});
    assert.equal(portal.busy,false);assert.equal(portal.error,'Justificatif absent');assert.equal(portal.records.length,1);assert.equal(portal.approvalOutcome,null);
    portal.decide(record,true);assert.equal(f.posts.length,2);
  } finally {f.close();}
});
test('lost approval response does not falsely announce that approval failed',()=>{
  const f=setup();try {
    const portal=runInInjectionContext(f.injector,()=>new PortalComponent());portal.records=[{id:'application-1'}];
    portal.decide(portal.records[0],true);f.posts[0].stream.error({status:0,message:'Connection lost'});
    assert.equal(portal.errorTitle,'Décision à vérifier');assert.match(portal.error,/décision peut déjà être enregistrée/);assert.equal(portal.busy,false);
  } finally {f.close();}
});
test('SMTP failure is shown as a mail warning with retry, without changing approval',async()=>{
  const f=setup();try {
    f.setResponse(of({status:'FAILED',message:'Authentification SMTP refusée.'}));
    const mail=runInInjectionContext(f.injector,()=>new ApprovalEmailComponent());mail.applicationId='approved-1';mail.ngOnChanges();await pause();
    assert.equal(mail.delivery?.status,'FAILED');assert.equal(mail.canRetry,true);assert.equal(mail.trackingError,'');
    mail.retry();mail.retry();assert.equal(f.posts.length,1);assert.equal(f.posts[0].path,'/admin/doctors/approved-1/approval-email');
    f.setResponse(of({status:'SENT',message:'Courriel transmis.'}));f.posts[0].stream.next({status:'QUEUED',message:'Envoi en cours'});await pause();
    assert.equal(mail.delivery?.status,'SENT');assert.equal(mail.canRetry,false);assert.equal(mail.busy,false);
  } finally {f.close();}
});
test('status network failure never announces an email as sent',async()=>{
  const f=setup();try {
    f.setResponse(throwError(()=>({status:0,message:'Réseau indisponible'})));
    const mail=runInInjectionContext(f.injector,()=>new ApprovalEmailComponent());mail.applicationId='approved-1';mail.ngOnChanges();await pause();
    assert.equal(mail.delivery,null);assert.match(mail.trackingError,/approbation du compte reste enregistrée/);assert.equal(mail.canRetry,false);
  } finally {f.close();}
});
