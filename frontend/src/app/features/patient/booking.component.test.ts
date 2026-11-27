import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { BookingComponent } from './booking.component';
import { ApiService } from '../../core/services/api.service';
import { AppointmentService } from '../../core/services/domain.services';
import { AuthService } from '../../core/services/auth.service';
import { BookingIntentService } from '../../core/services/booking-intent.service';
import { SessionUser } from '../../core/models/api.models';

const day='2030-01-15';
const slot={id:'real-slot-id',startsAt:`${day}T09:00:00Z`,endsAt:`${day}T09:30:00Z`,status:'AVAILABLE'};
function setup(startAt?: string){
  const requests:Array<{path:string;query:any;stream:Subject<any>}> = [];
  const sends:Array<any> = []; const submit=new Subject<any>();
  const injector=createEnvironmentInjector([
    {provide:FormBuilder,useValue:new FormBuilder()},
    {provide:ActivatedRoute,useValue:{paramMap:new BehaviorSubject(convertToParamMap({doctorId:'doctor-1'})),snapshot:{queryParamMap:convertToParamMap({date:day,startAt})}}},
    {provide:ApiService,useValue:{get:(path:string,query:any)=>{const stream=new Subject<any>();requests.push({path,query,stream});return stream;}}},
    {provide:AppointmentService,useValue:{create:(body:any)=>{sends.push(body);return submit;}}},
    {provide:AuthService,useValue:{user:()=>({firstName:'Patient',lastName:'Test'}),logout:()=>new Subject()}}
  ],null as any);
  const component=runInInjectionContext(injector,()=>new BookingComponent());component.ngOnInit();
  function respond(path:string,value:any){const request=requests.filter(r=>r.path.endsWith(path)).at(-1)!;request.stream.next(value);request.stream.complete();}
  function profile(services=[{name:'Consultation',price:'50 DT'},{name:'Contrôle',price:'35 DT'}],videoConsultationEnabled=false){
    respond('/doctor-1',{fullName:'Médecin de test',specialty:'Médecine générale',services,videoConsultationEnabled});
    respond('/booking-policy',{timeZone:'Africa/Tunis',minDate:'2030-01-01',maxDate:'2030-03-01',minimumNoticeMinutes:90});
    respond('/availability-calendar',[{date:day,status:'AVAILABLE',availableSlots:1},{date:'2030-01-16',status:'DAY_OFF',availableSlots:0}]);
  }
  return {component,requests,sends,submit,respond,profile,close:()=>injector.destroy()};
}

test('booking defaults to in-person and prevents video when the doctor has not enabled it',()=>{
  const f=setup();try{f.profile();f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[slot]);f.component.chooseSlot(slot as any);
    assert.equal(f.component.form.controls.consultationMode.value,'IN_PERSON');assert.equal(f.component.canSubmit,true);
    f.component.form.controls.consultationMode.setValue('VIDEO');assert.equal(f.component.canSubmit,false);f.component.submit();assert.equal(f.sends.length,0);
  }finally{f.close();}
});
test('enabled doctor accepts explicit video choice and sends it in the booking request',()=>{
  const f=setup();try{f.profile(undefined,true);f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[slot]);f.component.chooseSlot(slot as any);f.component.form.controls.consultationMode.setValue('VIDEO');
    assert.equal(f.component.canSubmit,true);f.component.submit();assert.equal(f.sends.length,1);assert.equal(f.sends[0].consultationMode,'VIDEO');
  }finally{f.close();}
});

test('summary and submit depend on a real selected service and returned slot',()=>{
  const f=setup();try{f.profile();assert.equal(f.component.canSubmit,false);assert.equal(f.component.selectedService,undefined);
    f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[slot]);f.component.chooseSlot(slot as any);
    assert.equal(f.component.selectedService?.price,'50 DT');assert.equal(f.component.time(slot.startsAt),'10:00');assert.equal(f.component.canSubmit,true);
    f.component.form.controls.serviceName.setValue('Contrôle');assert.equal(f.component.selectedSlot,undefined);assert.equal(f.component.canSubmit,false);assert.equal(f.component.selectedService?.price,'35 DT');
  }finally{f.close();}
});
test('changing dates clears the hour and cancels stale responses',()=>{
  const f=setup();try{f.profile();f.component.form.controls.serviceName.setValue('Consultation');const old=f.requests.at(-1)!;
    f.component.form.controls.date.setValue('2030-01-17');const current=f.requests.at(-1)!;
    old.stream.next([slot]);assert.equal(f.component.slots.length,0);
    current.stream.next([{...slot,startsAt:'2030-01-17T09:00:00Z',endsAt:'2030-01-17T09:30:00Z'}]);assert.equal(f.component.slots.length,1);
  }finally{f.close();}
});
test('deduplicates equivalent timestamps and rejects ranges, invalid and non-free slots',()=>{
  const f=setup();try{f.profile();f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[slot,{...slot,id:'duplicate',startsAt:`${day}T10:00:00+01:00`},{...slot,status:'BOOKED'},{...slot,startsAt:'invalid'},{...slot,endsAt:slot.startsAt}]);assert.equal(f.component.slots.length,1);assert.equal(f.component.slots[0].id,'real-slot-id');}finally{f.close();}
});
test('closed days and configured booking boundaries cannot be selected',()=>{
  const f=setup();try{f.profile();assert.equal(f.component.validDate('2030-03-02'),false);assert.equal(f.component.validDate('2029-12-31'),false);
    f.component.chooseDate({date:'2030-01-16',status:'DAY_OFF',availableSlots:0});assert.equal(f.component.selectedDate,day);
  }finally{f.close();}
});
test('empty catalogue and missing tariffs disable booking',()=>{
  const f=setup();try{f.profile([{name:'Non réservable',price:''}]);assert.equal(f.component.services.length,0);assert.equal(f.component.canSubmit,false);assert.equal(f.component.loadingDoctor,false);}finally{f.close();}
});
test('network errors differ from an empty slot response and preserve the reason',()=>{
  const f=setup();try{f.profile();f.component.form.controls.reason.setValue('Motif conservé');f.component.form.controls.serviceName.setValue('Consultation');f.requests.at(-1)!.stream.error({message:'Réseau indisponible'});
    assert.equal(f.component.slotsError,'Réseau indisponible');assert.equal(f.component.canSubmit,false);f.component.loadSlots();f.respond('/slots',[]);
    assert.equal(f.component.slotsError,'');assert.equal(f.component.form.controls.reason.value,'Motif conservé');assert.equal(f.component.canSubmit,false);
  }finally{f.close();}
});
test('double submission sends once, forwards displayed price, and announces pending confirmation',()=>{
  const f=setup();try{f.profile();f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[slot]);f.component.chooseSlot(slot as any);f.component.submit();f.component.submit();assert.equal(f.sends.length,1);assert.equal(f.sends[0].expectedServicePrice,'50 DT');
    f.submit.next({status:'PENDING'});assert.equal(f.component.success,'Demande envoyée, en attente de confirmation du médecin');assert.equal(f.component.canSubmit,false);
  }finally{f.close();}
});
test('a reservation conflict preserves input and refreshes the real doctor and slots',()=>{
  const f=setup();try{f.profile();f.component.form.controls.serviceName.setValue('Consultation');f.component.form.controls.reason.setValue('Conserver');f.respond('/slots',[slot]);f.component.chooseSlot(slot as any);f.component.submit();f.submit.error({status:409,message:'Créneau pris'});
    assert.match(f.component.error,/Créneau pris/);assert.equal(f.component.form.controls.reason.value,'Conserver');assert.equal(f.component.canSubmit,false);assert.equal(f.component.loadingDoctor,true);
  }finally{f.close();}
});

test('a slot brought back from the directory is restored only from a fresh availability response',()=>{
  const f=setup(slot.startsAt);try{
    f.profile();
    assert.equal(f.component.selectedSlot,undefined);
    assert.equal(f.requests.at(-1)?.path,'/doctors/doctor-1/slots');
    f.respond('/slots',[slot]);
    assert.equal(f.component.selectedSlot?.id,slot.id);
    assert.equal(f.component.canSubmit,false);
    f.component.form.controls.serviceName.setValue('Consultation');
    assert.equal(f.component.canSubmit,false);
    f.respond('/slots',[slot]);
    assert.equal(f.component.canSubmit,true);
    assert.equal(f.sends.length,0);
  }finally{f.close();}
});

test('an occupied slot from the directory cannot be submitted and shows a contextual message',()=>{
  const f=setup(slot.startsAt);try{
    f.profile();f.respond('/slots',[]);
    assert.match(f.component.slotNotice,/n’est plus disponible/);
    f.component.form.controls.serviceName.setValue('Consultation');f.respond('/slots',[]);
    f.component.submit();
    assert.equal(f.component.selectedSlot,undefined);
    assert.equal(f.sends.length,0);
  }finally{f.close();}
});

test('a booking intent is consumed once and can only route to the matching patient booking page',()=>{
  const injector=createEnvironmentInjector([{provide:Router,useValue:{createUrlTree:(commands:any,options:any)=>({commands,options})}}],null as any);
  const intent=runInInjectionContext(injector,()=>new BookingIntentService());
  const id='ffc1ed15-c515-44e3-84a2-60d82fe61b7c';
  const patient={id:'patient',firstName:'Leila',lastName:'Test',email:'leila@example.test',role:'PATIENT',status:'ACTIVE'} satisfies SessionUser;
  try{
    intent.remember({doctorId:id,date:day,startAt:slot.startsAt});
    assert.deepEqual(intent.destination(patient),{commands:['/patient/reserver',id],options:{queryParams:{date:day,startAt:slot.startsAt}}});
    assert.equal(intent.destination(patient),null);
    intent.remember({doctorId:id});
    assert.equal(intent.destination({...patient,role:'DOCTOR'}),null);
    intent.remember({doctorId:'https://example.com'});
    assert.equal(intent.destination(patient),null);
  }finally{injector.destroy();}
});
