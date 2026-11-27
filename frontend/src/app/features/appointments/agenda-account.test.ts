import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext, signal } from '@angular/core';
import { Subject, of } from 'rxjs';
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { AgendaComponent } from './agenda.component';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ProfilePhotoComponent } from '../../shared/profile-photo.component';
import { AvatarComponent, photoSource } from '../../shared/avatar.component';
import { routes } from '../../app.routes';
const page=(content:any[],number=0,totalPages=1)=>({content,number,totalPages,last:number+1>=totalPages});
function agendaFixture(){const reads:{query:any;stream:Subject<any>}[]=[];const injector=createEnvironmentInjector([{provide:ApiService,useValue:{get:(_path:string,query:any)=>{const stream=new Subject();reads.push({query,stream});return stream;}}}],null as any);return {agenda:runInInjectionContext(injector,()=>new AgendaComponent()),reads,close:()=>injector.destroy()};}
test('agenda has day and week ranges and navigates across a year boundary',()=>{const f=agendaFixture();try{const a=f.agenda;a.cursor=new Date('2026-12-31T12:00:00');assert.equal(a.days.length,7);assert.equal(a.key(a.start),'2026-12-28');assert.equal(a.key(a.end),'2027-01-04');assert.match(a.label,/3 janvier 2027/);a.selectView('day');assert.equal(a.days.length,1);a.move(1);assert.equal(a.key(a.cursor),'2027-01-01');}finally{f.close();}});
test('agenda ignores obsolete period responses and loads every returned page',()=>{const f=agendaFixture();try{const a=f.agenda;a.load();const obsolete=f.reads[0];a.move(1);f.reads[1].stream.next(page([{id:'a',startsAt:'2030-01-01T10:00:00Z',serviceName:'Consultation'}],0,2));f.reads[1].stream.complete();assert.equal(f.reads.length,3);f.reads[2].stream.next(page([{id:'b',startsAt:'2030-01-01T09:00:00Z',serviceName:'Consultation'}],1,2));f.reads[2].stream.complete();obsolete.stream.next(page([{id:'obsolete'}]));obsolete.stream.complete();assert.deepEqual(a.appointments.map(e=>e.id),['b','a']);assert.equal(a.loading,false);}finally{f.close();}});
test('events use the cabinet day and text/status filters do not alter the source',()=>{const f=agendaFixture();try{const a=f.agenda;a.appointments=[{id:'a',patientName:'Amel Test',serviceName:'Contrôle',status:'CONFIRMED',startsAt:'2030-01-15T23:30:00Z',endsAt:'2030-01-16T00:00:00Z'},{id:'b',patientName:'Autre patient',serviceName:'Consultation',status:'PENDING',startsAt:'2030-01-16T08:00:00Z',endsAt:'2030-01-16T08:30:00Z'}];a.query='amel';a.status='CONFIRMED';assert.equal(a.events(new Date('2030-01-16T12:00:00')).length,1);assert.equal(a.events(new Date('2030-01-15T12:00:00')).length,0);assert.equal(a.pendingCount,1);assert.equal(a.confirmedCount,1);a.resetFilters();assert.equal(a.filtered.length,2);}finally{f.close();}});
test('legacy settings URLs lead to the unified profile for both roles',()=>{assert.equal(routes.find(r=>r.path==='patient/parametres')?.redirectTo,'patient/profil');assert.equal(routes.find(r=>r.path==='medecin/parametres')?.redirectTo,'medecin/profil');});
test('the clinical record, appointment details and video room use the guarded patient shell', () => {
  const patient = routes.find(route => route.path === 'patient');
  assert.ok(patient?.loadComponent);
  assert.ok(patient.canActivateChild?.length);
  const clinical = patient.children?.find(route => route.path === 'dossier-medical');
  assert.equal(clinical?.data?.['patientNav'], 'dossier-medical');
  assert.ok(patient.children?.some(route => route.path === 'rendez-vous/:appointmentId'));
  assert.ok(patient.children?.find(route => route.path === 'rendez-vous/:appointmentId/teleconsultation')?.canActivate?.length);
});
test('clinical and schedule routes share the doctor workspace and retain their draft guards', () => {
  const doctor = routes.find(route => route.path === 'medecin');
  const patient = routes.find(route => route.path === 'patient');
  assert.equal(doctor?.loadComponent, patient?.loadComponent);
  assert.ok(doctor?.canActivateChild?.length);
  for (const path of [':area', 'rendez-vous/:appointmentId/consultation', 'rendez-vous/:appointmentId/teleconsultation']) {
    assert.ok(doctor?.children?.find(route => route.path === path)?.canDeactivate?.length);
  }
});
test('avatar preserves initials on missing images and only accepts application photo URLs',()=>{const avatar=new AvatarComponent();avatar.name='Malek Brika';assert.equal(avatar.letters,'MB');assert.equal(photoSource('https://unrelated.example/portrait.jpg'),null);assert.ok(photoSource('/api/doctors/id/photo?v=1')?.endsWith('/api/doctors/id/photo?v=1'));avatar.failed=true;avatar.ngOnChanges();assert.equal(avatar.failed,false);});
test('photo upload is guarded against double sends and updates the account after success',()=>{let sends=0;const request=new Subject<any>();const user=signal<any>({firstName:'Médecin',photoUrl:null});const injector=createEnvironmentInjector([{provide:ApiService,useValue:{post:()=>{sends++;return request;}}},{provide:AuthService,useValue:{user}}],null as any);try{const c=runInInjectionContext(injector,()=>new ProfilePhotoComponent());c.photo=new Blob(['jpeg'],{type:'image/jpeg'});c.save();c.save();assert.equal(sends,1);request.next({photoUrl:'/api/account/me/photo?v=new'});assert.equal(user().photoUrl,'/api/account/me/photo?v=new');assert.equal(c.preview,'');assert.equal(c.busy,false);}finally{injector.destroy();}});
test('photo upload failure retains the selected photo for retry',()=>{const request=new Subject();const injector=createEnvironmentInjector([{provide:ApiService,useValue:{post:()=>request}},{provide:AuthService,useValue:{user:signal(null)}}],null as any);try{const c=runInInjectionContext(injector,()=>new ProfilePhotoComponent());const photo=new Blob(['jpeg']);c.photo=photo;c.save();request.error({message:'Connexion interrompue'});assert.equal(c.photo,photo);assert.equal(c.busy,false);assert.equal(c.error,'Connexion interrompue');}finally{injector.destroy();}});
