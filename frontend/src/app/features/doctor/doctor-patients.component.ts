import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { Page } from '../../core/models/api.models';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
@Component({selector:'app-doctor-patients',standalone:true,imports:[CommonModule,FormsModule,RouterLink,CabinetDatePipe],template:`
<p class="notice notice-info">Cette liste comprend les patients avec lesquels vous avez terminé au moins une consultation. Chaque dossier présente votre propre suivi.</p>
<form class="filter-bar workspace-filters" (ngSubmit)="search()"><label for="patient-query">Rechercher un patient<input id="patient-query" [(ngModel)]="query" name="query" type="search" placeholder="Nom du patient" maxlength="100"></label><button type="submit" class="button button-primary">Rechercher</button><button type="button" class="button button-secondary" [disabled]="!query && !appliedQuery" (click)="resetSearch()">Réinitialiser</button></form>
<div class="notice notice-error" role="alert" *ngIf="error">{{error}} <button type="button" (click)="load(page)" class="text-button">Réessayer</button></div>
<div class="workspace-loading" role="status" *ngIf="loading"><span class="sr-only">Chargement des patients…</span><i aria-hidden="true"></i><i aria-hidden="true"></i><i aria-hidden="true"></i></div>
<div *ngIf="!loading&&!error"><div class="table-scroll" role="region" aria-label="Patients suivis" tabindex="0" *ngIf="rows.length"><table class="product-table"><thead><tr><th scope="col">Patient</th><th scope="col">Coordonnées</th><th scope="col">Dernière consultation</th><th scope="col">Consultations</th><th scope="col"><span class="sr-only">Action</span></th></tr></thead><tbody><tr *ngFor="let p of rows"><th scope="row">{{p.fullName}}</th><td><span class="table-line">{{p.email}}</span><span class="muted table-line">{{p.phone||'—'}}</span></td><td>{{p.lastVisit|cabinetDate}}</td><td>{{p.completedCount}}</td><td><a class="button button-secondary" [routerLink]="['/medecin/patients',p.id]">Ouvrir le dossier</a></td></tr></tbody></table></div>
<div class="empty-panel" *ngIf="!rows.length"><h2>{{appliedQuery?'Aucun patient correspondant':'Votre suivi commence ici'}}</h2><p>{{appliedQuery?'Essayez un autre nom.':'Les patients apparaîtront ici après leur première consultation terminée avec vous.'}}</p><a class="button button-secondary" routerLink="/medecin/rendez-vous" *ngIf="!appliedQuery">Voir mes rendez-vous</a></div>
<nav class="pagination" *ngIf="totalPages>1" aria-label="Pages de patients"><button [disabled]="page===0" (click)="load(page-1)">Précédent</button><span>{{total}} patients · Page {{page+1}} sur {{totalPages}}</span><button [disabled]="page+1>=totalPages" (click)="load(page+1)">Suivant</button></nav></div>`})
export class DoctorPatientsComponent {
 private api=inject(ApiService);private destroy=inject(DestroyRef);private request?:{unsubscribe():void};query='';appliedQuery='';rows:any[]=[];loading=false;error='';page=0;total=0;totalPages=0;
 constructor(){this.load(0);}
 search(): void { this.appliedQuery = this.query.trim(); this.load(0); }
 resetSearch(): void { this.query = ''; this.search(); }
 load(page: number): void {
  this.request?.unsubscribe();
  this.page = page;
  this.loading = true;
  this.error = '';
  this.request = this.api.get<Page<any>>('/doctors/me/patients', { q: this.appliedQuery, page, size: 20 })
    .pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: result => {
        this.rows = result.content;
        this.total = result.totalElements;
        this.totalPages = result.totalPages;
        this.loading = false;
      },
      error: error => { this.loading = false; this.error = error.message; }
    });
 }
}
