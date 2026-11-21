import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { Subject, of } from 'rxjs';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { appointmentPermissions } from '../../core/models/appointment-permissions';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { AppointmentDetailComponent } from './appointment-detail.component';
import { PortalComponent } from '../portal/portal.component';

const now=Date.parse('2030-01-15T08:00:00Z');
const future={startsAt:'2030-01-16T08:00:00Z',endsAt:'2030-01-16T08:30:00Z'};

test('pending request gives the doctor confirm and reject, with no cancel or reschedule',()=>{
  const actions=appointmentPermissions('DOCTOR',{...future,status:'PENDING'},now);
  assert.equal(actions.canConfirm,true);assert.equal(actions.canReject,true);
  assert.equal(actions.canCancel,false);assert.equal(actions.canReschedule,false);
  assert.equal(actions.canMarkCompleted,false);assert.equal(actions.canMarkNoShow,false);
});
test('confirmed future appointment may be cancelled by doctor but never reprogrammed by doctor',()=>{
  const actions=appointmentPermissions('DOCTOR',{...future,status:'CONFIRMED'},now);
  assert.equal(actions.canCancel,true);assert.equal(actions.canReject,false);assert.equal(actions.canConfirm,false);assert.equal(actions.canReschedule,false);
});
test('rescheduling remains available to patients across all eligible statuses',()=>{
  for(const status of ['PENDING','CONFIRMED','TO_RESCHEDULE']){
    for(const role of ['PATIENT','USER'])assert.equal(appointmentPermissions(role,{...future,status},now).canReschedule,true);
    for(const role of ['DOCTOR','ADMIN'])assert.equal(appointmentPermissions(role,{...future,status},now).canReschedule,false);
  }
  for(const status of ['CANCELLED','REJECTED','COMPLETED','NO_SHOW'])assert.equal(appointmentPermissions('PATIENT',{...future,status},now).canReschedule,false);
});
test('doctor waits for patient after an absence while patient can choose a slot or cancel',()=>{
  const record={...future,status:'TO_RESCHEDULE'};
  assert.ok(Object.values(appointmentPermissions('DOCTOR',record,now)).every(value=>value===false));
  const patient=appointmentPermissions('PATIENT',record,now);assert.equal(patient.canCancel,true);assert.equal(patient.canReschedule,true);
});
test('started appointments cannot be cancelled and administrative views have no participant actions',()=>{
  const record={status:'CONFIRMED',startsAt:'2030-01-14T08:00:00Z',endsAt:'2030-01-14T08:30:00Z'};
  const doctor=appointmentPermissions('DOCTOR',record,now);assert.equal(doctor.canCancel,false);assert.equal(doctor.canMarkCompleted,true);assert.equal(doctor.canMarkNoShow,true);
  assert.equal(appointmentPermissions('PATIENT',record,now).canCancel,false);assert.equal(appointmentPermissions('PATIENT',record,now).canReschedule,false);
  assert.ok(Object.values(appointmentPermissions('ADMIN',{...future,status:'PENDING'},now)).every(value=>value===false));
});

function setup(role:string,status:string){
  const requests:Array<{method:string;path:string;stream:Subject<any>}>=[];
  const record={id:'appointment-1',status,startsAt:new Date(Date.now()+86400000).toISOString(),endsAt:new Date(Date.now()+88200000).toISOString()};
  let confirmations=0;
  (globalThis as any).window={matchMedia:()=>({matches:false}),confirm:()=>{confirmations++;return true;}};
  const api={get:()=>of(record),post:(path:string)=>send('POST',path),patch:(path:string)=>send('PATCH',path)};
  function send(method:string,path:string){const stream=new Subject();requests.push({method,path,stream});return stream;}
  const injector=createEnvironmentInjector([
    {provide:ApiService,useValue:api},{provide:FormBuilder,useValue:new FormBuilder()},
    {provide:ActivatedRoute,useValue:{snapshot:{data:{roles:[role]},paramMap:convertToParamMap({appointmentId:record.id,area:'rendez-vous'})}}},
    {provide:Router,useValue:{navigate:()=>Promise.resolve(true)}},{provide:Title,useValue:{setTitle:()=>{}}},
    {provide:AuthService,useValue:{user:()=>({firstName:'Test',lastName:'Participant',role})}}
  ],null as any);
  const detail=runInInjectionContext(injector,()=>new AppointmentDetailComponent());detail.ngOnInit();
  return {injector,detail,record,requests,confirmations:()=>confirmations,close:()=>injector.destroy()};
}
test('detail blocks doctor cancellation of pending requests, and prevents duplicate decisions',()=>{
  const f=setup('DOCTOR','PENDING');try{
    f.detail.cancel();f.detail.changeStatus('CANCELLED');assert.equal(f.requests.length,0);assert.equal(f.confirmations(),0);
    f.detail.changeStatus('CONFIRMED');f.detail.changeStatus('REJECTED');
    assert.equal(f.requests.length,1);assert.equal(f.requests[0].method,'PATCH');assert.equal(f.detail.busy,true);
    f.requests[0].stream.error({message:'Connexion interrompue'});assert.equal(f.detail.busy,false);assert.equal(f.detail.error,'Connexion interrompue');
  }finally{f.close();}
});
test('doctor cancellation of confirmed appointment is guarded while request is running',()=>{
  const f=setup('DOCTOR','CONFIRMED');try{
    f.detail.cancel();f.detail.cancel();assert.equal(f.requests.length,1);assert.equal(f.confirmations(),1);
    assert.equal(f.requests[0].method,'POST');assert.match(f.requests[0].path,/\/cancellation$/);
    f.requests[0].stream.error({message:'Erreur réseau'});assert.equal(f.detail.busy,false);assert.equal(f.detail.appointment.status,'CONFIRMED');
  }finally{f.close();}
});
test('doctor appointment list follows the same pending action policy as detail view',()=>{
  const f=setup('DOCTOR','PENDING');try{
    const portal=runInInjectionContext(f.injector,()=>new PortalComponent());
    portal.appointmentAction(f.record,'CANCELLED');assert.equal(f.requests.length,0);
    portal.appointmentAction(f.record,'CONFIRMED');portal.appointmentAction(f.record,'REJECTED');assert.equal(f.requests.length,1);
    f.requests[0].stream.error({message:'Erreur réseau'});assert.equal(portal.busy,false);
  }finally{f.close();}
});
