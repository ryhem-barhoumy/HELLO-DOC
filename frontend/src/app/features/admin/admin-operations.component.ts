import { Component, DestroyRef, Input, OnChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { Page } from '../../core/models/api.models';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
@Component({selector:'app-admin-operations',standalone:true,imports:[CommonModule,FormsModule,RouterLink,CabinetDatePipe],template:`
<p class="notice notice-info">{{area==='activite'?'Le journal consigne les décisions prises depuis son activation. Il ne contient pas de notes médicales.':'Vue administrative des rendez-vous. Les motifs et les fiches de consultation restent dans l’espace du médecin concerné.'}}</p>
<form class="filter-bar workspace-filters" *ngIf="area==='rendez-vous'" (ngSubmit)="load(0)" novalidate>
 <label>Patient ou médecin<input type="search" [(ngModel)]="query" name="query" maxlength="100"></label>
 <label>Statut<select [(ngModel)]="status" name="status"><option value="">Tous les statuts</option><option *ngFor="let s of statuses" [value]="s[0]">{{s[1]}}</option></select></label>
 <label>Du<input type="date" [(ngModel)]="from" name="from"></label>
 <label>Au<input type="date" [(ngModel)]="to" name="to" [attr.min]="from || null" [attr.aria-invalid]="invalidPeriod" [attr.aria-describedby]="invalidPeriod ? 'admin-period-error' : null"></label>
 <button type="submit" class="button button-primary">Filtrer</button>
 <button type="button" class="button button-secondary" [disabled]="!(query||status||from||to)" (click)="resetFilters()">Réinitialiser</button>
</form>
<p class="notice notice-error" id="admin-period-error" role="alert" *ngIf="invalidPeriod">La date de fin doit être égale ou postérieure à la date de début.</p>
<p class="notice notice-error" role="alert" *ngIf="error">{{error}} <button type="button" (click)="load(page)" class="text-button">Réessayer</button></p>
<div class="workspace-loading" role="status" *ngIf="loading"><span class="sr-only">Chargement des résultats…</span><i aria-hidden="true"></i><i aria-hidden="true"></i><i aria-hidden="true"></i></div>
<ng-container *ngIf="!loading&&!error&&!invalidPeriod"><div class="table-scroll" role="region" [attr.aria-label]="area==='activite'?'Journal administratif':'Rendez-vous de la plateforme'" tabindex="0" *ngIf="rows.length"><table class="product-table" *ngIf="area==='rendez-vous';else activity"><thead><tr><th scope="col">Date au cabinet</th><th scope="col">Patient</th><th scope="col">Médecin / service</th><th scope="col">Statut</th><th scope="col">Tarif</th><th scope="col"><span class="sr-only">Action</span></th></tr></thead><tbody><tr *ngFor="let row of rows"><td>{{row.startsAt|cabinetDate:'date'}}<span class="table-line muted">{{row.startsAt|cabinetDate:'time'}} – {{row.endsAt|cabinetDate:'time'}}</span></td><td>{{row.patientName}}</td><td>{{row.doctorName}}<span class="table-line muted">{{row.serviceName}}</span></td><td><span class="status-badge" [ngClass]="statusClass(row.status)">{{label(row.status)}}</span></td><td>{{row.servicePrice||'—'}}</td><td><a [routerLink]="['/admin/rendez-vous',row.id]" class="small-action">Détails</a></td></tr></tbody></table><ng-template #activity><table class="product-table"><thead><tr><th scope="col">Date</th><th scope="col">Action</th><th scope="col">Auteur</th><th scope="col">Référence</th></tr></thead><tbody><tr *ngFor="let row of rows"><td>{{row.occurredAt|cabinetDate:'datetime'}}</td><td>{{row.description}}</td><td>{{row.actorName}}</td><td><small>{{row.targetId}}</small></td></tr></tbody></table></ng-template></div>
<div class="empty-panel" *ngIf="!rows.length"><h2>{{area==='activite'?'Aucune action consignée':'Aucun rendez-vous correspondant'}}</h2><p>{{area==='activite'?'Les prochaines décisions administratives apparaîtront ici.':'Modifiez les filtres pour élargir la recherche.'}}</p></div><nav class="pagination" *ngIf="totalPages>1" aria-label="Pages de résultats"><button [disabled]="page===0" (click)="load(page-1)">Précédent</button><span>{{total}} résultats · Page {{page+1}} sur {{totalPages}}</span><button [disabled]="page+1>=totalPages" (click)="load(page+1)">Suivant</button></nav></ng-container>`})
export class AdminOperationsComponent implements OnChanges {
 @Input() area='rendez-vous';private api=inject(ApiService);private destroy=inject(DestroyRef);private request?:{unsubscribe():void};query='';status='';from='';to='';rows:any[]=[];page=0;totalPages=0;total=0;loading=false;error='';invalidPeriod=false;
 readonly statuses=[['PENDING','En attente'],['CONFIRMED','Confirmé'],['COMPLETED','Terminé'],['CANCELLED','Annulé'],['REJECTED','Refusé'],['TO_RESCHEDULE','À reprogrammer'],['NO_SHOW','Absent']];
 label(status:string){return this.statuses.find(s=>s[0]===status)?.[1]??status;}
 statusClass(status: string): string {
  if (status === 'CONFIRMED' || status === 'COMPLETED') return 'positive';
  if (status === 'PENDING' || status === 'TO_RESCHEDULE') return 'pending';
  return 'negative';
 }
 ngOnChanges(){this.load(0);}
 resetFilters(): void {
  this.query = this.status = this.from = this.to = '';
  this.load(0);
 }
 load(page: number): void {
  this.request?.unsubscribe();
  this.error = '';
  this.invalidPeriod = this.area === 'rendez-vous' && !!this.from && !!this.to && this.to < this.from;
  this.loading = !this.invalidPeriod;
  if (this.invalidPeriod) return;

  this.page = page;
  const viewingAppointments = this.area === 'rendez-vous';
  const filters = viewingAppointments ? this.appointmentFilters() : {};
  this.request = this.api.get<Page<any>>(viewingAppointments ? '/admin/appointments' : '/admin/activity-logs',
    { page, size: 20, ...filters }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
    next: result => {
      this.rows = result.content;
      this.total = result.totalElements;
      this.totalPages = result.totalPages;
      this.loading = false;
    },
    error: error => { this.loading = false; this.error = error.message; }
  });
 }

 private appointmentFilters() {
  const from = this.from ? new Date(`${this.from}T00:00:00+01:00`).toISOString() : undefined;
  const to = this.to ? new Date(Date.parse(`${this.to}T00:00:00+01:00`) + 86_400_000).toISOString() : undefined;
  return { q: this.query.trim(), status: this.status, from, to };
 }
}
