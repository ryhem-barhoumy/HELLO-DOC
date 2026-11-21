import { WorkspaceFooterComponent } from '../../shared/workspace-footer.component';
import { Component, DestroyRef, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { Appointment, ConsultationNote } from '../../core/models/api.models';
import { WorkspaceHeaderComponent } from '../../shared/workspace-header.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';

@Component({selector:'app-consultation-note',standalone:true,imports:[WorkspaceFooterComponent,CommonModule,ReactiveFormsModule,RouterLink,WorkspaceHeaderComponent,CabinetDatePipe],template:`
<app-workspace-header/>
<main class="product-page clinical-page">
 <a class="text-link" [routerLink]="['/medecin/rendez-vous',id]">← Retour au rendez-vous</a>
 <div class="page-intro"><span class="eyebrow">SUIVI DU PATIENT</span><h1>Fiche de consultation</h1><p class="muted">Consignez les éléments utiles au suivi. Cette fiche est réservée au médecin de la consultation.</p></div>
 <div class="loading-panel" role="status" *ngIf="loading">Chargement de la consultation…</div>
 <div class="notice notice-error" role="alert" *ngIf="error">{{error}} <button type="button" class="text-button" *ngIf="!ready" (click)="load()">Réessayer</button></div>
 <div class="notice notice-info" role="status" *ngIf="success">{{success}}</div>
 <ng-container *ngIf="ready&&appointment">
 <section class="context-strip" aria-label="Consultation concernée"><div><small>Patient</small><strong>{{appointment.patientName}}</strong></div><div><small>Consultation</small><strong>{{appointment.serviceName}}</strong></div><div><small>Date au cabinet</small><strong>{{appointment.startsAt|cabinetDate:'datetime'}}</strong></div><span class="status-badge positive">Terminée</span></section>
 <div class="clinical-layout">
 <form class="surface clinical-form" [formGroup]="form" (ngSubmit)="save()" [attr.aria-busy]="busy">
  <div class="section-heading"><h2>Compte rendu</h2><span class="muted">* Champ obligatoire</span></div>
  <label for="note-resume">Résumé de la consultation *</label><textarea [readOnly]="busy" id="note-resume" rows="4" formControlName="resume" maxlength="4000" [attr.aria-invalid]="form.controls.resume.touched&&form.controls.resume.invalid" aria-describedby="resume-help"></textarea>
  <small id="resume-help" [class.field-error]="form.controls.resume.touched&&form.controls.resume.invalid">Un résumé est nécessaire. 4 000 caractères maximum.</small>
  <label for="note-observation">Observations</label><textarea [readOnly]="busy" id="note-observation" rows="5" formControlName="observation" maxlength="8000"></textarea>
  <label for="note-treatment">Traitement et recommandations</label><textarea [readOnly]="busy" id="note-treatment" rows="5" formControlName="treatment" maxlength="8000"></textarea>
  <label for="note-check">Prochain contrôle conseillé</label><input id="note-check" type="date" [readOnly]="busy" formControlName="nextCheck" [min]="tomorrow" aria-describedby="check-help">
  <p class="field-note" id="check-help">Si vous indiquez une date, une notification invitera le patient à choisir lui-même un rendez-vous. Aucun créneau n’est réservé automatiquement.</p>
  <div class="private-field"><label for="note-private">Note privée du médecin</label><textarea [readOnly]="busy" id="note-private" rows="3" formControlName="privateNote" maxlength="4000"></textarea><small>Cette note reste dans votre dossier de suivi et n’est pas transmise au patient.</small></div>
  <div class="form-footer"><span class="muted">{{savedAt?'Dernier enregistrement :':'Fiche non encore enregistrée'}} <ng-container *ngIf="savedAt">{{savedAt|cabinetDate:'datetime'}}</ng-container></span><button class="button button-primary" [disabled]="busy||conflict||form.invalid||!form.dirty">{{busy?'Enregistrement…':'Enregistrer la fiche'}}</button></div>
  <div *ngIf="conflict" class="notice notice-warning" role="alert"><p>Une autre version existe. Vos saisies restent dans les champs ci-dessus. Copiez-les si nécessaire avant de recharger.</p><button type="button" class="button button-secondary" (click)="reload()">Recharger la version enregistrée</button></div>
 </form>
 <aside class="surface revision-panel"><h2>Enregistrements</h2><p class="muted">Les 50 derniers enregistrements de cette fiche, sans copie du contenu médical.</p><ol class="event-timeline" *ngIf="history.length;else noHistory"><li *ngFor="let event of history"><strong>{{event.action==='CREATED'?'Fiche créée':'Fiche mise à jour'}}</strong><time>{{event.changedAt|cabinetDate:'datetime'}}</time><span>{{event.editorName}}</span></li></ol><ng-template #noHistory><p class="empty-copy">Le premier enregistrement apparaîtra ici.</p></ng-template><p class="notice notice-warning" *ngIf="historyError">{{historyError}}</p></aside>
 </div></ng-container>
</main><app-workspace-footer/>`})
export class ConsultationNoteComponent {
 private route=inject(ActivatedRoute);private api=inject(ApiService);private fb=inject(FormBuilder);private destroy=inject(DestroyRef);
 readonly id=this.route.snapshot.paramMap.get('appointmentId')??'';appointment:Appointment|null=null;error='';success='';busy=false;loading=true;ready=false;conflict=false;version:number|null=null;savedAt='';savedNextCheck='';history:any[]=[];historyError='';
 tomorrow=new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(Date.now()+86400000));
 form=this.fb.nonNullable.group({resume:['',[Validators.required,Validators.maxLength(4000),Validators.pattern(/\S/)]],observation:['',Validators.maxLength(8000)],treatment:['',Validators.maxLength(8000)],nextCheck:[''],privateNote:['',Validators.maxLength(4000)]});
 @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent){if(this.busy||this.form.dirty){event.preventDefault();event.returnValue='';}}
 constructor(){this.load();}
 load(){this.loading=true;this.error='';this.ready=false;forkJoin({appointment:this.api.get<Appointment>(`/appointments/${this.id}`),note:this.api.get<ConsultationNote|null>(`/appointments/${this.id}/consultation-note`)}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:({appointment,note})=>{this.appointment=appointment;this.form.reset({resume:note?.resume??'',observation:note?.observation??'',treatment:note?.treatment??'',nextCheck:note?.nextCheck??'',privateNote:note?.privateNote??''});this.version=note?.version??null;this.savedAt=note?.updatedAt??'';this.savedNextCheck=note?.nextCheck??'';this.conflict=false;this.ready=true;this.loading=false;this.loadHistory();},error:e=>{this.loading=false;this.error=e.message;}});}
 reload(){if(window.confirm('Remplacer les saisies affichées par la dernière version enregistrée ?'))this.load();}
 private loadHistory(){this.historyError='';this.api.get<any[]>(`/appointments/${this.id}/consultation-note/history`).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:h=>this.history=h,error:()=>this.historyError='Le journal est indisponible. La fiche reste accessible.'});}
 save(){if(this.busy||!this.ready||this.conflict)return;this.error='';this.success='';this.form.markAllAsTouched();if(this.form.invalid)return;const values=this.form.getRawValue();if(values.nextCheck&&values.nextCheck!==this.savedNextCheck&&values.nextCheck<this.tomorrow){this.error='Le prochain contrôle doit être prévu à partir de demain.';return;}this.busy=true;this.api.put<ConsultationNote>(`/appointments/${this.id}/consultation-note`,{...values,nextCheck:values.nextCheck||null,version:this.version}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:n=>{this.busy=false;this.version=n.version;this.savedAt=n.updatedAt;this.savedNextCheck=n.nextCheck??'';this.form.markAsPristine();this.success='Fiche de consultation enregistrée.';this.loadHistory();},error:e=>{this.busy=false;this.error=e.message;this.conflict=e.status===409;}});}
}
