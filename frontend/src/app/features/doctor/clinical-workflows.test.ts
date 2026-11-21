import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { Subject, of } from 'rxjs';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ApiService } from '../../core/services/api.service';
import { ConsultationNoteComponent } from './consultation-note.component';
import { RescheduleComponent } from '../appointments/reschedule.component';
import { cabinetDate } from '../../shared/cabinet-date.pipe';
import { WorkspaceHeaderComponent } from '../../shared/workspace-header.component';
import { AuthService } from '../../core/services/auth.service';
import { SessionState } from '../../core/services/session-state.service';
import { unsavedNoteGuard } from '../../core/guards/unsaved-note.guard';

const appointment={id:'visit-1',status:'COMPLETED',startsAt:'2030-01-16T08:00:00Z',endsAt:'2030-01-16T08:30:00Z',serviceName:'Consultation',patientName:'Patient de test'};
function fixture(kind:'note'|'reschedule',existing:any=null){
 const reads:Array<{path:string;query:any;stream:Subject<any>}>=[];const writes:Array<{body:any;stream:Subject<any>}>=[];
 const api={get:(path:string,query?:any)=>{if(path.endsWith('/history'))return of([]);if(path.endsWith('/consultation-note'))return of(existing);if(!path.endsWith('/reschedule-options'))return of(appointment);const stream=new Subject();reads.push({path,query,stream});return stream;},put:(_path:string,body:any)=>{const stream=new Subject();writes.push({body,stream});return stream;}};
 const injector=createEnvironmentInjector([{provide:ApiService,useValue:api},{provide:FormBuilder,useValue:new FormBuilder()},{provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({appointmentId:appointment.id})}}}],null as any);
 const component=runInInjectionContext(injector,()=>kind==='note'?new ConsultationNoteComponent():new RescheduleComponent());
 return {component,reads,writes,close:()=>injector.destroy()};
}
test('clinical form requires a meaningful summary and does not write empty medical records',()=>{
 const f=fixture('note');try{const c=f.component as ConsultationNoteComponent;c.save();c.form.controls.resume.setValue('   ');c.save();assert.equal(f.writes.length,0);assert.equal(c.ready,true);}finally{f.close();}
});
test('first save uses no version, blocks duplicates, and accepts the persisted revision',()=>{
 const f=fixture('note');try{const c=f.component as ConsultationNoteComponent;c.form.controls.resume.setValue('Éléments du suivi');c.form.markAsDirty();c.save();c.save();assert.equal(f.writes.length,1);assert.equal(f.writes[0].body.version,null);assert.equal(f.writes[0].body.nextCheck,null);f.writes[0].stream.next({version:0,updatedAt:'2030-01-16T09:00:00Z'});assert.equal(c.version,0);assert.equal(c.form.pristine,true);assert.equal(c.busy,false);}finally{f.close();}
});
test('version conflict retains typed clinical content and prevents blind overwrites',()=>{
 const f=fixture('note',{resume:'Ancienne version',version:2});try{const c=f.component as ConsultationNoteComponent;c.form.controls.resume.setValue('Texte à conserver');c.save();assert.equal(f.writes[0].body.version,2);f.writes[0].stream.error({status:409,message:'Une autre version existe.'});assert.equal(c.form.controls.resume.value,'Texte à conserver');assert.equal(c.conflict,true);c.save();assert.equal(f.writes.length,1);}finally{f.close();}
});
test('network failure keeps clinical input and allows retry with the same version',()=>{
 const f=fixture('note',{resume:'Version enregistrée',version:4});try{const c=f.component as ConsultationNoteComponent;c.form.controls.resume.setValue('Texte conservé');c.save();f.writes[0].stream.error({status:0,message:'Hors connexion'});assert.equal(c.conflict,false);c.save();assert.equal(f.writes.length,2);assert.equal(f.writes[1].body.resume,'Texte conservé');assert.equal(f.writes[1].body.version,4);}finally{f.close();}
});
test('reschedule cancels obsolete date requests and clears selected time immediately',()=>{
 const f=fixture('reschedule');try{const c=f.component as RescheduleComponent;c.today='2030-01-01';c.ngOnInit();const old=f.reads[0];c.form.controls.date.setValue('2030-01-17');c.loadOptions();f.reads[1].stream.next([{startsAt:'2030-01-17T09:00:00Z'}]);c.form.controls.startAt.setValue('2030-01-17T09:00:00Z');old.stream.next([{startsAt:'2030-01-16T10:00:00Z'}]);assert.equal(c.selectedSlot?.startsAt,'2030-01-17T09:00:00Z');c.form.controls.date.setValue('2030-01-18');c.loadOptions();assert.equal(c.form.controls.startAt.value,'');assert.equal(c.slots.length,0);}finally{f.close();}
});
test('reschedule duplicate submissions send once and report pending confirmation',()=>{
 const f=fixture('reschedule');try{const c=f.component as RescheduleComponent;c.today='2030-01-01';c.ngOnInit();f.reads[0].stream.next([{startsAt:'2030-01-16T09:00:00Z'},{startsAt:'2030-01-16T09:00:00Z'}]);assert.equal(c.slots.length,1);c.form.controls.startAt.setValue(c.slots[0].startsAt);c.submit();c.submit();assert.equal(f.writes.length,1);f.writes[0].stream.next({...appointment,status:'PENDING'});assert.match(c.success,/attente de confirmation/);c.submit();assert.equal(f.writes.length,1);}finally{f.close();}
});
test('reschedule conflict refreshes slots without clearing the patient reason',()=>{
 const f=fixture('reschedule');try{const c=f.component as RescheduleComponent;c.today='2030-01-01';c.ngOnInit();f.reads[0].stream.next([{startsAt:'2030-01-16T09:00:00Z'}]);c.form.controls.startAt.setValue(c.slots[0].startsAt);c.form.controls.reason.setValue('Indisponibilité personnelle');c.submit();f.writes[0].stream.error({status:409});assert.equal(c.form.controls.reason.value,'Indisponibilité personnelle');assert.equal(c.form.controls.startAt.value,'');assert.equal(f.reads.length,2);assert.equal(c.busy,false);}finally{f.close();}
});
test('cabinet formatting respects Tunis midnight, calendar dates and invalid values',()=>{
 assert.match(cabinetDate('2030-01-15T23:30:00Z','time'),/00:30/);assert.match(cabinetDate('2030-01-15T23:30:00Z','date'),/16/);assert.match(cabinetDate('2030-01-15','date'),/15/);assert.equal(cabinetDate('invalid'),'—');assert.equal(cabinetDate(null),'—');
});
test('workspace navigation keeps stable link identities between render passes',()=>{
 let role='DOCTOR';const injector=createEnvironmentInjector([{provide:AuthService,useValue:{user:()=>({role})}}],null as any);
 try{const header=runInInjectionContext(injector,()=>new WorkspaceHeaderComponent());const first=header.links;header.open=true;assert.equal(header.links,first);assert.ok(first.some(l=>l.path==='/medecin/patients'));role='ADMIN';assert.notEqual(header.links,first);assert.equal(header.links,header.links);header.close();assert.equal(header.open,false);}finally{injector.destroy();}
});
test('unsaved clinical content warns before navigation but cannot block logout cleanup',()=>{
 let confirmations=0;(globalThis as any).window={confirm:()=>{confirmations++;return false;}};
 const injector=createEnvironmentInjector([SessionState],null as any);
 try{const state=injector.get(SessionState);const component={busy:false,form:{dirty:true}};const check=()=>runInInjectionContext(injector,()=>unsavedNoteGuard(component,null as any,null as any,null as any));
  state.user.set({id:'doctor',firstName:'Docteur',lastName:'Test',email:'doctor@qa.example',role:'DOCTOR',status:'ACTIVE'});assert.equal(check(),false);assert.equal(confirmations,1);
  state.user.set(null);assert.equal(check(),true);assert.equal(confirmations,1);
 }finally{injector.destroy();}
});
