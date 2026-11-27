import { unsavedDoctorEditorGuard } from './core/guards/unsaved-doctor-editor.guard';
import { Routes } from '@angular/router';
import { adminGuard, roleGuard } from './core/guards/auth.guard';
import { Role } from './core/models/api.models';
import { unsavedNoteGuard } from './core/guards/unsaved-note.guard';
import { unsavedConsultationWorkspaceGuard } from './core/guards/unsaved-consultation-workspace.guard';
import { consultationAccessGuard } from './core/guards/consultation-access.guard';

const workspace = () => import('./shared/care-workspace.component').then(m => m.CareWorkspaceComponent);
const portal = (role: Role) => ({ loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate: [roleGuard], data: { roles: [role] } });
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/public/landing.component').then(m => m.LandingComponent), title: 'Hello Doc — La santé, plus proche' },
  { path: 'medecins', loadComponent: () => import('./features/public/doctor-directory.component').then(m => m.DoctorDirectoryComponent), title: 'Trouver un médecin | Hello Doc' },
  { path: 'medecins/:doctorId', loadComponent: () => import('./features/patient/doctor-detail.component').then(m => m.DoctorDetailComponent), data: { publicDirectory: true }, title: 'Profil médecin | Hello Doc' },
  { path: 'connexion', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent), title: 'Connexion | Hello Doc' },
  { path: 'mot-de-passe-oublie', loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent), title: 'Mot de passe oublié | Hello Doc' },
  { path: 'inscription/patient', loadComponent: () => import('./features/auth/registration.component').then(m => m.RegistrationComponent), title: 'Inscription patient | Hello Doc', data: { mode: 'patient' } },
  { path: 'inscription/medecin', loadComponent: () => import('./features/auth/registration.component').then(m => m.RegistrationComponent), title: 'Inscription médecin | Hello Doc', data: { mode: 'doctor' } },
  { path: 'patient/parametres', redirectTo: 'patient/profil', pathMatch: 'full' },
  {
    path: 'patient',
    loadComponent: workspace,
    canActivate: [roleGuard],
    canActivateChild: [roleGuard],
    data: { roles: ['PATIENT'] },
    children: [
      { path: '', pathMatch: 'full', ...portal('PATIENT'), title: 'Espace patient | Hello Doc' },
      { path: 'dossier-medical', loadComponent: () => import('./features/patient/patient-medical-record.component').then(m => m.PatientMedicalRecordComponent), data: { roles: ['PATIENT'], patientNav: 'dossier-medical' }, title: 'Mon dossier médical | Hello Doc' },
      { path: 'medecins/:doctorId', loadComponent: () => import('./features/patient/doctor-detail.component').then(m => m.DoctorDetailComponent), data: { roles: ['PATIENT'], patientNav: 'medecins' }, title: 'Profil médecin | Hello Doc' },
      { path: 'reserver/:doctorId', loadComponent: () => import('./features/patient/booking.component').then(m => m.BookingComponent), data: { roles: ['PATIENT'], patientNav: 'medecins' }, title: 'Réserver | Hello Doc' },
      { path: 'rendez-vous/:appointmentId/teleconsultation', loadComponent: () => import('./features/appointments/teleconsultation.component').then(m => m.TeleconsultationComponent), canActivate: [consultationAccessGuard], data: { roles: ['PATIENT'], patientNav: 'rendez-vous' }, title: 'Téléconsultation | Hello Doc' },
      { path: 'avis/:appointmentId', loadComponent: () => import('./features/patient/review.component').then(m => m.ReviewComponent), data: { roles: ['PATIENT'], patientNav: 'historique' }, title: 'Donner un avis | Hello Doc' },
      { path: 'rendez-vous/:appointmentId/reprogrammer', loadComponent: () => import('./features/appointments/reschedule.component').then(m => m.RescheduleComponent), data: { roles: ['PATIENT'], patientNav: 'rendez-vous' }, title: 'Reprogrammer un rendez-vous | Hello Doc' },
      { path: 'rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), data: { roles: ['PATIENT'], patientNav: 'rendez-vous' }, title: 'Rendez-vous | Hello Doc' },
      { path: ':area', ...portal('PATIENT') }
    ]
  },
  { path:'medecin/parametres',redirectTo:'medecin/profil',pathMatch:'full' },
  {
    path: 'medecin',
    loadComponent: workspace,
    canActivate: [roleGuard],
    canActivateChild: [roleGuard],
    data: { roles: ['DOCTOR'] },
    children: [
      { path: '', pathMatch: 'full', ...portal('DOCTOR'), canDeactivate:[unsavedDoctorEditorGuard], title:'Tableau de bord | Hello Doc' },
      { path:'rendez-vous/:appointmentId/teleconsultation',loadComponent:()=>import('./features/appointments/teleconsultation.component').then(m=>m.TeleconsultationComponent),canActivate:[roleGuard,consultationAccessGuard],canDeactivate:[unsavedConsultationWorkspaceGuard],data:{roles:['DOCTOR']},title:'Téléconsultation | Hello Doc' },
      { path: 'rendez-vous/:appointmentId/consultation', loadComponent: () => import('./features/doctor/consultation-note.component').then(m => m.ConsultationNoteComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, canDeactivate:[unsavedNoteGuard], title:'Fiche de consultation | Hello Doc' },
      { path: 'rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Rendez-vous | Hello Doc' },
      { path: 'patients/:patientId', loadComponent: () => import('./features/doctor/patient-dossier.component').then(m => m.PatientDossierComponent), canActivate:[roleGuard], data:{roles:['DOCTOR']}, title:'Dossier patient | Hello Doc' },
      { path: ':area', ...portal('DOCTOR'), canDeactivate:[unsavedDoctorEditorGuard] }
    ]
  },
  { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' },
  { path: 'admin/dashboard', loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Administration | Hello Doc' },
  { path: 'admin/medecins/:doctorId', loadComponent: () => import('./features/admin/admin-doctor-detail.component').then(m => m.AdminDoctorDetailComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Détail médecin | Hello Doc' },
  { path: 'admin/rendez-vous/:appointmentId', loadComponent: () => import('./features/appointments/appointment-detail.component').then(m => m.AppointmentDetailComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Rendez-vous | Hello Doc' },
  { path: 'admin/:area', loadComponent: () => import('./features/portal/portal.component').then(m => m.PortalComponent), canActivate:[adminGuard], data:{roles:['ADMIN']}, title:'Administration | Hello Doc' },
  { path: 'access-denied', loadComponent: () => import('./features/public/access-denied.component').then(m => m.AccessDeniedComponent), title: 'Accès refusé | Hello Doc' },
  { path: '**', redirectTo: '' }
];
