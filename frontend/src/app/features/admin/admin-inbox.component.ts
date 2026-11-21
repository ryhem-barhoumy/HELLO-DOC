import { Component, DestroyRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
@Component({selector:'app-admin-inbox',standalone:true,imports:[CommonModule,RouterLink],template:`
<p class="muted">Les candidatures et les courriels d’approbation qui demandent votre attention.</p><p class="loading-panel" role="status" *ngIf="loading">Recherche des éléments à traiter…</p><p role="alert" class="notice notice-error" *ngIf="error">{{error}} <button class="text-button" (click)="load(page)">Réessayer</button></p>
<ng-container *ngIf="data&&!loading"><section class="surface section-heading"><div><span class="eyebrow">CANDIDATURES MÉDECINS</span><h2>{{data.pendingApprovals}} demande{{data.pendingApprovals===1?'':'s'}} en attente</h2><p class="muted">Vérifiez les coordonnées et le justificatif professionnel avant de prendre une décision.</p></div><a class="button button-primary" routerLink="/admin/approbations">Examiner les candidatures</a></section>
<section class="dossier-history"><h2>Courriels d’approbation à renvoyer</h2><p class="muted">Le compte est déjà approuvé. Un échec de courriel n’annule pas cette décision.</p><article class="surface section-heading" *ngFor="let row of data.failedEmails.content"><div><h3>{{row.fullName}}</h3><p class="muted">{{row.email}}</p></div><a class="button button-secondary" [routerLink]="['/admin/medecins',row.id]">Vérifier et renvoyer le courriel</a></article><p class="empty-panel" *ngIf="!data.failedEmails.totalElements">Aucun courriel d’approbation en échec.</p><nav class="pagination" aria-label="Courriels en échec" *ngIf="data.failedEmails.totalPages>1"><button [disabled]="page===0" (click)="load(page-1)">Précédent</button><span>Page {{page+1}} sur {{data.failedEmails.totalPages}}</span><button [disabled]="page+1>=data.failedEmails.totalPages" (click)="load(page+1)">Suivant</button></nav></section></ng-container>`})
export class AdminInboxComponent {
 private api=inject(ApiService);private destroy=inject(DestroyRef);private request?:{unsubscribe():void};data:any=null;loading=false;error='';page=0;constructor(){this.load(0);}
 load(page:number){this.request?.unsubscribe();this.page=page;this.loading=true;this.error='';this.request=this.api.get('/admin/inbox',{page,size:20}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:d=>{this.data=d;this.loading=false;},error:e=>{this.loading=false;this.error=e.message;}});}
}
