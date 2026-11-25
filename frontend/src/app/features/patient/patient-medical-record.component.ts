import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { WorkspaceHeaderComponent } from '../../shared/workspace-header.component';
import { WorkspaceFooterComponent } from '../../shared/workspace-footer.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { RouterLink } from '@angular/router';

interface PatientConsultation {
  appointmentId:string; startsAt:string; status:string; serviceName:string; doctorName:string;
  doctorId:string;note:{resume:string;observation:string;treatment:string;nextCheck:string|null;recommendedDelay?:string|null;followUpInstructions?:string|null;updatedAt:string}|null;
  prescriptions:{id:string;prescribedAt:string;doctorName:string;recommendations:string;items:{medication:string;instructions:string}[]}[];
}

@Component({selector:'app-patient-medical-record',standalone:true,imports:[CommonModule,RouterLink,WorkspaceHeaderComponent,WorkspaceFooterComponent,CabinetDatePipe],template:`
<app-workspace-header/><main class="product-page clinical-page">
 <div class="page-intro"><span class="eyebrow">MES INFORMATIONS DE SANTÉ</span><h1>Mon dossier médical</h1><p class="muted">Comptes rendus et ordonnances associés à vos consultations terminées.</p></div>
 <p class="loading-panel" role="status" *ngIf="loading">Chargement de votre dossier…</p>
 <div class="notice notice-error" role="alert" *ngIf="error">{{error}} <button class="text-button" (click)="load()">Réessayer</button></div>
 <section *ngIf="!loading&&!error&&!visits.length" class="empty-panel"><h2>Aucune consultation terminée</h2><p>Les documents médicaux partagés apparaîtront ici après vos consultations.</p></section>
 <section class="dossier-history" *ngIf="!loading&&!error&&visits.length"><article class="surface dossier-visit" *ngFor="let visit of visits">
  <div class="section-heading"><div><span class="eyebrow">{{visit.startsAt|cabinetDate:'date'}}</span><h2>{{visit.serviceName}}</h2><p class="muted">{{visit.startsAt|cabinetDate:'datetime'}} · Dr {{visit.doctorName}} · Consultation terminée</p></div><span class="status-badge positive">Compte rendu patient</span></div>
  <section class="medical-note" *ngIf="visit.note;else noNote"><h3>Compte rendu</h3><h4>Résumé</h4><p class="preserve-lines">{{visit.note.resume}}</p><ng-container *ngIf="visit.note.observation"><h4>Observations</h4><p class="preserve-lines">{{visit.note.observation}}</p></ng-container><ng-container *ngIf="visit.note.treatment"><h4>Traitement et recommandations</h4><p class="preserve-lines">{{visit.note.treatment}}</p></ng-container><section class="follow-up" *ngIf="visit.note.nextCheck||visit.note.recommendedDelay||visit.note.followUpInstructions"><h4>Contrôle recommandé</h4><p *ngIf="visit.note.nextCheck">Date conseillée : {{visit.note.nextCheck|cabinetDate}}</p><p *ngIf="visit.note.recommendedDelay">Délai recommandé : {{visit.note.recommendedDelay}}</p><p class="preserve-lines" *ngIf="visit.note.followUpInstructions">{{visit.note.followUpInstructions}}</p><a class="button button-secondary" [routerLink]="['/patient/reserver',visit.doctorId]">Prendre un rendez-vous de suivi</a><p class="field-note">Cette recommandation n’est pas un rendez-vous réservé.</p></section></section>
  <ng-template #noNote><p class="empty-copy">Aucun compte rendu n’a été partagé pour cette consultation.</p></ng-template>
  <section class="medical-note" *ngIf="visit.prescriptions.length;else noPrescription"><h3>Ordonnances</h3><article *ngFor="let rx of visit.prescriptions" class="prescription-card"><h4>Ordonnance du {{rx.prescribedAt|cabinetDate:'date'}} · Dr {{rx.doctorName}}</h4><p *ngIf="rx.recommendations">{{rx.recommendations}}</p><ul><li *ngFor="let item of rx.items"><strong>{{item.medication}}</strong> — {{item.instructions}}</li></ul></article></section><ng-template #noPrescription><p class="empty-copy">Aucune ordonnance créée pour cette consultation.</p></ng-template>
  <p class="field-note" *ngIf="visit.note">Mis à jour le {{visit.note.updatedAt|cabinetDate:'datetime'}}</p>
 </article></section>
</main><app-workspace-footer/>`})
export class PatientMedicalRecordComponent{
 private api=inject(ApiService);private destroy=inject(DestroyRef);visits:PatientConsultation[]=[];loading=true;error='';
 constructor(){this.load();}
 load(){this.loading=true;this.error='';this.api.get<PatientConsultation[]>('/patients/me/consultations').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:v=>{this.visits=v;this.loading=false;},error:e=>{this.error=e.message;this.loading=false;}});}
}
