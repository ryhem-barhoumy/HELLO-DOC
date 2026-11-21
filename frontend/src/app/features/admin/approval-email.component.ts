import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, catchError, of, switchMap, takeWhile, timer } from 'rxjs';
import { ApiService } from '../../core/services/api.service';

export interface ApprovalEmailStatus {
  status: 'NOT_SENT'|'QUEUED'|'SENT'|'FAILED'|'DISABLED';
  message: string;
  attemptedAt?: string;
  sentAt?: string;
}

@Component({
  selector: 'app-approval-email', standalone: true, imports: [CommonModule],
  template: `
    <section class="mail-status" [class.warning]="delivery?.status==='FAILED'||delivery?.status==='DISABLED'" role="status" aria-live="polite">
      <strong>Courriel d’approbation</strong>
      <p>{{ delivery?.message || 'Vérification de l’envoi du courriel…' }}</p>
      <p *ngIf="delivery?.status==='FAILED'">Le compte médecin reste approuvé et actif. Vous pouvez relancer uniquement le courriel.</p>
      <p *ngIf="trackingError">{{ trackingError }}</p>
      <small *ngIf="delivery?.sentAt">Transmis le {{ delivery?.sentAt | date:'short' }}. Si le destinataire ne le voit pas, vérifiez aussi les courriers indésirables.</small>
      <div class="mail-actions">
        <button type="button" *ngIf="canRetry" [disabled]="busy" (click)="retry()">{{ busy?'Envoi demandé…':'Envoyer le courriel' }}</button>
        <button type="button" *ngIf="trackingError || delivery?.status==='QUEUED'" [disabled]="busy" (click)="refresh()">Actualiser l’état</button>
      </div>
    </section>`,
  styles: [`:host{display:block;margin:14px 0}.mail-status{padding:16px;border:1px solid #cfe4db;border-radius:10px;background:#f0f8f4;color:#234d40}.mail-status.warning{background:#fff8e9;border-color:#e4ce9b;color:#70501a}.mail-status p{margin:7px 0;line-height:1.5}.mail-status small{display:block}.mail-actions{display:flex;gap:10px;flex-wrap:wrap}.mail-actions button{margin-top:10px;min-height:40px;border:1px solid currentColor;border-radius:7px;background:white;color:inherit;padding:8px 13px;cursor:pointer}.mail-actions button:disabled{opacity:.65;cursor:wait}.mail-actions button:focus-visible{outline:3px solid #0a8674;outline-offset:3px}`]
})
export class ApprovalEmailComponent implements OnChanges {
  @Input({required:true}) applicationId='';
  private readonly api=inject(ApiService);
  private readonly destroyRef=inject(DestroyRef);
  private polling?: Subscription;
  delivery: ApprovalEmailStatus|null=null;
  trackingError='';
  busy=false;
  get canRetry(){return !!this.delivery && (['NOT_SENT','FAILED','DISABLED'].includes(this.delivery.status)||(this.delivery.status==='QUEUED'&&this.polling?.closed));}
  ngOnChanges(){this.delivery=null;this.busy=false;this.refresh();}
  refresh(){
    if(!this.applicationId)return;
    this.polling?.unsubscribe();this.trackingError='';
    this.polling=timer(0,1500).pipe(
      switchMap(()=>this.api.get<ApprovalEmailStatus>(`/admin/doctors/${this.applicationId}/approval-email`).pipe(catchError(()=>{
        this.trackingError='Impossible de vérifier l’envoi pour le moment. L’approbation du compte reste enregistrée.';return of(null);
      }))),
      takeWhile((result,index)=>result?.status==='QUEUED'&&index<19,true),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result=>{if(result)this.delivery=result;});
  }
  retry(){
    if(this.busy||!this.applicationId)return;
    this.busy=true;this.trackingError='';
    this.api.post<ApprovalEmailStatus>(`/admin/doctors/${this.applicationId}/approval-email`).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:result=>{this.busy=false;this.delivery=result;if(result.status==='QUEUED')this.refresh();},
      error:error=>{this.busy=false;this.trackingError=error.message||'Impossible de relancer le courriel. Le compte reste approuvé.';}
    });
  }
}
