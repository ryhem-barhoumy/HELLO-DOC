import { Routes } from '@angular/router';
import { adminGuard, roleGuard } from './core/guards/auth.guard';
import { Role } from './core/models/api.models';

const portal = (role: Role) => ({ loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate: [roleGuard], data: { roles: [role] } });
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/public/landing.component').then(m => m.LandingComponent), title: 'Hello Doc — La santé, plus proche' },
  { path: 'connexion', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent), title: 'Connexion | Hello Doc' },
  { path: 'mot-de-passe-oublie', loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent), title: 'Mot de passe oublié | Hello Doc' },
  { path: 'inscription/patient', loadComponent: () => import('./features/auth/registration.component').then(m => m.RegistrationComponent), title: 'Inscription patient | Hello Doc', data: { mode: 'patient' } },
  { path: 'inscription/medecin', loadComponent: () => import('./features/auth/registration.component').then(m => m.RegistrationComponent), title: 'Inscription médecin | Hello Doc', data: { mode: 'doctor' } },
  { path: 'patient', ...portal('PATIENT') }, { path: 'patient/:area', ...portal('PATIENT') },
  { path: 'patient/medecins/:doctorId', loadComponent: () => import('./features/patient/doctor-detail.component').then(m => m.DoctorDetailComponent), canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Profil médecin | Hello Doc' },
  { path: 'patient/reserver/:doctorId', loadComponent: () => import('./features/patient/booking.component').then(m => m.BookingComponent), canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Réserver | Hello Doc' },
  { path: 'patient/avis/:appointmentId', loadComponent: () => import('./features/patient/review.component').then(m => m.ReviewComponent), canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Donner un avis | Hello Doc' },
  { path: 'patient/rendez-vous/:appointmentId/reprogrammer', loadComponent: () => import('./features/appointments/reschedule.component').then(m => m.RescheduleComponent), canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Reprogrammer un rendez-vous | Hello Doc' },
  { path: 'patient/rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), canActivate:[roleGuard], data:{roles:['PATIENT']}, title:'Rendez-vous | Hello Doc' },
  { path: 'medecin', ...portal('DOCTOR') }, { path: 'medecin/:area', ...portal('DOCTOR') },
  { path: 'medecin/rendez-vous/:appointmentId/consultation', loadComponent: () => import('./features/doctor/consultation-note.component').then(m => m.ConsultationNoteComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Fiche de consultation | Hello Doc' },
  { path: 'medecin/rendez-vous/:appointmentId/reprogrammer', loadComponent: () => import('./features/appointments/reschedule.component').then(m => m.RescheduleComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Reprogrammer un rendez-vous | Hello Doc' },
  { path: 'medecin/rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Rendez-vous | Hello Doc' },
  { path: 'medecin/patients/:patientId', loadComponent: () => import('./features/doctor/patient-dossier.component').then(m => m.PatientDossierComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Dossier patient | Hello Doc' },
  { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' },
  { path: 'admin/dashboard', loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Administration | Hello Doc' },
  { path: 'admin/medecins/:doctorId', loadComponent: () => import('./features/admin/admin-doctor-detail.component').then(m => m.AdminDoctorDetailComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Détail médecin | Hello Doc' },
  { path: 'admin/rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), canActivate:[adminGuard], title:'Rendez-vous | Hello Doc' },
  { path: 'admin/:area', loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Administration | Hello Doc' },
  { path: 'access-denied', loadComponent: () => import('./features/public/access-denied.component').then(m => m.AccessDeniedComponent), title: 'Accès refusé | Hello Doc' },
  { path: '**', redirectTo: '' }
];
