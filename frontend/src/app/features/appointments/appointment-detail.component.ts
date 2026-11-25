import { WorkspaceFooterComponent } from '../../shared/workspace-footer.component';
import { WorkspaceHeaderComponent } from '../../shared/workspace-header.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { appointmentPermissions } from '../../core/models/appointment-permissions';

@Component({selector:'app-appointment-detail',standalone:true,imports:[WorkspaceFooterComponent,CommonModule,RouterLink,WorkspaceHeaderComponent,CabinetDatePipe],templateUrl:'./appointment-detail.component.html'})
export class AppointmentDetailComponent implements OnInit {
 private route=inject(ActivatedRoute);private api=inject(ApiService);private router=inject(Router);appointmentId=this.route.snapshot.paramMap.get('appointmentId')??'';role=this.route.snapshot.data['roles']?.[0]??'PATIENT';appointment:any=null;loading=true;busy=false;error='';
 get duration(){if(!this.appointment)return 0;return Math.round((Date.parse(this.appointment.endsAt)-Date.parse(this.appointment.startsAt))/60000);}
 get consultationMode(){return this.appointment?.consultationMode??'IN_PERSON';}
 get canOpenVideo(){return this.role==='DOCTOR'&&this.appointment?.status==='CONFIRMED'&&this.consultationMode==='VIDEO';}
 get backLink(){return this.role==='DOCTOR'?'/medecin/rendez-vous':this.role==='ADMIN'?'/admin/rendez-vous':'/patient/rendez-vous';}get permissions(){return appointmentPermissions(this.role,this.appointment);}get canCancel(){return this.permissions.canCancel;}get canMarkNoShow(){return this.permissions.canMarkNoShow;}get statusClass(){return ['CONFIRMED','COMPLETED'].includes(this.appointment?.status)?'positive':this.appointment?.status==='PENDING'?'pending':'negative';}
 label(status:string){return ({TO_RESCHEDULE:'À reprogrammer',PENDING:'En attente',CONFIRMED:'Confirmé',COMPLETED:'Terminé',REJECTED:'Refusé',CANCELLED:'Annulé',NO_SHOW:'Absent'} as Record<string,string>)[status]??status;}
 ngOnInit(){this.load();}load(){this.loading=true;this.error='';this.api.get<any>(`${this.role==='ADMIN'?'/admin':''}/appointments/${this.appointmentId}`).subscribe({next:a=>{this.appointment=a;this.loading=false;},error:e=>{this.error=e.message;this.loading=false;}});}
 openTeleconsultation(){if(this.role!=='DOCTOR'||this.appointment?.status!=='CONFIRMED'||this.consultationMode!=='VIDEO'||this.busy)return;void this.router.navigate(['/medecin/rendez-vous',this.appointmentId,'teleconsultation']);}
 cancel(){
  if(this.busy||this.loading||!this.permissions.canCancel||!window.confirm('Confirmer l’annulation de ce rendez-vous ?'))return;
  this.busy=true;this.error='';
  this.api.post(`/appointments/${this.appointmentId}/cancellation`,{reason:this.role==='DOCTOR'?'Annulation par le médecin':'Annulation demandée par le patient'}).subscribe({next:()=>{this.busy=false;this.load();},error:e=>{this.busy=false;this.error=e.message;}});
 }
 changeStatus(status:string){
  const p=this.permissions;
  const allowed=status==='CONFIRMED'?p.canConfirm:status==='REJECTED'?p.canReject:status==='COMPLETED'?p.canMarkCompleted:status==='NO_SHOW'?p.canMarkNoShow:false;
  if(this.busy||this.loading||!allowed)return;
  if(['REJECTED','NO_SHOW','COMPLETED'].includes(status)&&!window.confirm(status==='REJECTED'?'Refuser cette demande de rendez-vous ?':status==='NO_SHOW'?'Confirmer que le patient ne s’est pas présenté ?':'Confirmer que cette consultation est terminée ?'))return;
  this.busy=true;this.error='';
  this.api.patch(`/appointments/${this.appointmentId}/status`,{status}).subscribe({next:()=>{this.busy=false;this.load();},error:e=>{this.busy=false;this.error=e.message;}});
 }
}
