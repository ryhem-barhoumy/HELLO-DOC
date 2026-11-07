import { Routes } from '@angular/router';
import { LandingComponent } from './features/public/landing.component';
import { AuthComponent } from './features/auth/auth.component';
import { PortalComponent } from './features/portal/portal.component';
import { roleGuard } from './core/guards/auth.guard';
import { Role } from './core/models/api.models';
import { DoctorDetailComponent } from './features/patient/doctor-detail.component';
import { BookingComponent } from './features/patient/booking.component';
import { ReviewComponent } from './features/patient/review.component';
import { ConsultationNoteComponent } from './features/doctor/consultation-note.component';
import { RescheduleComponent } from './features/appointments/reschedule.component';
import { AppointmentDetailComponent } from './features/appointments/appointment-detail.component';
import { PatientDossierComponent } from './features/doctor/patient-dossier.component';
import { AccessDeniedComponent } from './features/public/access-denied.component';

const portal = (role: Role) => ({ component: PortalComponent, canActivate: [roleGuard], data: { roles: [role] } });
export const routes: Routes = [
  { path: '', component: LandingComponent, title: 'Hello Doc — La santé, plus proche' },
  { path: 'connexion', component: AuthComponent, title: 'Connexion | Hello Doc', data: { mode: 'login' } },
  { path: 'inscription/patient', component: AuthComponent, title: 'Inscription patient | Hello Doc', data: { mode: 'patient' } },
  { path: 'inscription/medecin', component: AuthComponent, title: 'Inscription médecin | Hello Doc', data: { mode: 'doctor' } },
  { path: 'patient', ...portal('PATIENT') }, { path: 'patient/:area', ...portal('PATIENT') },
  { path: 'patient/medecins/:doctorId', component: DoctorDetailComponent, canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Profil médecin | Hello Doc' },
  { path: 'patient/reserver/:doctorId', component: BookingComponent, canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Réserver | Hello Doc' },
  { path: 'patient/avis/:appointmentId', component: ReviewComponent, canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Donner un avis | Hello Doc' },
  { path: 'patient/rendez-vous/:appointmentId', component:AppointmentDetailComponent, canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Rendez-vous | Hello Doc' },
  { path: 'patient/rendez-vous/:appointmentId/reprogrammer', component:RescheduleComponent, canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Reprogrammer un rendez-vous | Hello Doc' },
  { path: 'medecin', ...portal('DOCTOR') }, { path: 'medecin/:area', ...portal('DOCTOR') },
  { path: 'medecin/rendez-vous/:appointmentId/consultation', component:ConsultationNoteComponent, canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Fiche de consultation | Hello Doc' },
  { path: 'medecin/rendez-vous/:appointmentId/reprogrammer', component:RescheduleComponent, canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Reprogrammer un rendez-vous | Hello Doc' },
  { path: 'medecin/rendez-vous/:appointmentId', component:AppointmentDetailComponent, canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Rendez-vous | Hello Doc' },
  { path: 'medecin/patients/:patientId', component:PatientDossierComponent, canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Dossier patient | Hello Doc' },
  { path: 'admin', ...portal('ADMIN') }, { path: 'admin/:area', ...portal('ADMIN') },
  { path: 'admin/rendez-vous/:appointmentId', component:AppointmentDetailComponent, canActivate:[roleGuard], data:{roles:['ADMIN']}, title:'Rendez-vous | Hello Doc' },
  { path: 'access-denied', component: AccessDeniedComponent, title: 'Accès refusé | Hello Doc' },
  { path: '**', redirectTo: '' }
];
