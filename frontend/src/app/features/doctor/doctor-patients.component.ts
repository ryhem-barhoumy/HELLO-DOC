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
<form class="filter-bar" (ngSubmit)="load(0)"><label for="patient-query">Rechercher un patient<input id="patient-query" [(ngModel)]="query" name="query" type="search" placeholder="Nom du patient" maxlength="100"></label><button class="button button-primary">Rechercher</button></form>
<div class="notice notice-error" role="alert" *ngIf="error">{{error}} <button (click)="load(page)" class="text-button">Réessayer</button></div><p class="loading-panel" role="status" *ngIf="loading">Chargement des patients…</p>
<div *ngIf="!loading&&!error"><div class="table-scroll" role="region" aria-label="Patients suivis" tabindex="0" *ngIf="rows.length"><table class="product-table"><thead><tr><th scope="col">Patient</th><th scope="col">Coordonnées</th><th scope="col">Dernière consultation</th><th scope="col">Consultations</th><th scope="col"><span class="sr-only">Action</span></th></tr></thead><tbody><tr *ngFor="let p of rows"><th scope="row">{{p.fullName}}</th><td><span class="table-line">{{p.email}}</span><span class="muted table-line">{{p.phone||'—'}}</span></td><td>{{p.lastVisit|cabinetDate}}</td><td>{{p.completedCount}}</td><td><a class="button button-secondary" [routerLink]="['/medecin/patients',p.id]">Ouvrir le dossier</a></td></tr></tbody></table></div>
<div class="empty-panel" *ngIf="!rows.length"><h2>{{query?'Aucun patient correspondant':'Votre suivi commence ici'}}</h2><p>{{query?'Essayez un autre nom.':'Les patients apparaîtront ici après leur première consultation terminée avec vous.'}}</p><a class="button button-secondary" routerLink="/medecin/rendez-vous" *ngIf="!query">Voir mes rendez-vous</a></div>
<nav class="pagination" *ngIf="totalPages>1" aria-label="Pages de patients"><button [disabled]="page===0" (click)="load(page-1)">Précédent</button><span>{{total}} patients · Page {{page+1}} sur {{totalPages}}</span><button [disabled]="page+1>=totalPages" (click)="load(page+1)">Suivant</button></nav></div>`})
export class DoctorPatientsComponent {
 private api=inject(ApiService);private destroy=inject(DestroyRef);private request?:{unsubscribe():void};query='';rows:any[]=[];loading=false;error='';page=0;total=0;totalPages=0;
 constructor(){this.load(0);}
 load(page:number){this.request?.unsubscribe();this.page=page;this.loading=true;this.error='';this.request=this.api.get<Page<any>>('/doctors/me/patients',{q:this.query.trim(),page,size:20}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:p=>{this.rows=p.content;this.total=p.totalElements;this.totalPages=p.totalPages;this.loading=false;},error:e=>{this.loading=false;this.error=e.message;}});}
}
