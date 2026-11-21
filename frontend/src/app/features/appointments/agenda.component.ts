import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, expand, reduce, Subscription } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { Appointment } from '../../core/models/api.models';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
interface AgendaPage { content:Appointment[]; number:number; totalPages:number; last:boolean; }
@Component({selector:'app-agenda',standalone:true,imports:[CommonModule,FormsModule,RouterLink,CabinetDatePipe],templateUrl:'./agenda.component.html'})
export class AgendaComponent implements OnInit {
 private api=inject(ApiService);private subscription?:Subscription;private destroy=inject(DestroyRef);
 cursor=this.localDate(this.todayKey());view:'day'|'week'='week';query='';status='';loading=false;error='';appointments:Appointment[]=[];
 readonly statuses=[{value:'PENDING',label:'En attente'},{value:'CONFIRMED',label:'Confirmé'},{value:'TO_RESCHEDULE',label:'À reprogrammer'},{value:'COMPLETED',label:'Terminé'},{value:'CANCELLED',label:'Annulé'},{value:'REJECTED',label:'Refusé'},{value:'NO_SHOW',label:'Patient absent'}];
 constructor(){this.destroy.onDestroy(()=>this.subscription?.unsubscribe());}
 ngOnInit(){this.load();}
 private todayKey(){return new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis'}).format(new Date());}
 private localDate(key:string){return new Date(`${key}T12:00:00`);}
 key(date:Date){return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;}
 private plus(date:Date,days:number){const value=new Date(date);value.setDate(value.getDate()+days);return value;}
 get start(){return this.view==='week'?this.plus(this.cursor,-((this.cursor.getDay()+6)%7)):this.cursor;}
 get end(){return this.plus(this.start,this.view==='week'?7:1);}
 get days(){return Array.from({length:this.view==='week'?7:1},(_,i)=>this.plus(this.start,i));}
 get label(){const last=this.plus(this.end,-1);const format=(date:Date)=>new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'long',year:'numeric'}).format(date);return this.view==='week'?`${format(this.start)} – ${format(last)}`:new Intl.DateTimeFormat('fr-FR',{weekday:'long',day:'numeric',month:'long',year:'numeric'}).format(this.cursor);}
 get filtered(){const q=this.query.trim().toLocaleLowerCase('fr');return this.appointments.filter(a=>(!this.status||a.status===this.status)&&(!q||`${a.patientName??''} ${a.serviceName}`.toLocaleLowerCase('fr').includes(q)));}
 get pendingCount(){return this.appointments.filter(a=>a.status==='PENDING').length;}
 get confirmedCount(){return this.appointments.filter(a=>a.status==='CONFIRMED').length;}
 events(day:Date){const key=this.key(day);return this.filtered.filter(a=>new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis'}).format(new Date(a.startsAt))===key);}
 trackDay=(_index:number,day:Date)=>this.key(day);
 trackEvent=(_index:number,event:Appointment)=>event.id;
 isToday(day:Date){return this.key(day)===this.todayKey();}
 statusLabel(value:string){return this.statuses.find(s=>s.value===value)?.label??value;}
 selectView(view:'day'|'week'){if(this.view===view)return;this.view=view;this.load();}
 openDay(day:Date){this.cursor=day;this.view='day';this.load();}
 move(offset:number){this.cursor=this.plus(this.cursor,offset*(this.view==='week'?7:1));this.load();}
 today(){this.cursor=this.localDate(this.todayKey());this.load();}
 chooseDay(key:string){if(/^\d{4}-\d{2}-\d{2}$/.test(key)){this.cursor=this.localDate(key);this.load();}}
 resetFilters(){this.query='';this.status='';}
 load(){this.subscription?.unsubscribe();this.loading=true;this.error='';this.appointments=[];const query={from:`${this.key(this.start)}T00:00:00+01:00`,to:`${this.key(this.end)}T00:00:00+01:00`,size:100};const page=(number:number)=>this.api.get<AgendaPage>('/doctors/me/appointments',{...query,page:number});
  this.subscription=page(0).pipe(expand(result=>result.last||result.number+1>=result.totalPages?EMPTY:page(result.number+1)),reduce((all,result)=>[...all,...result.content],[] as Appointment[])).subscribe({next:appointments=>{this.appointments=appointments.sort((a,b)=>Date.parse(a.startsAt)-Date.parse(b.startsAt));this.loading=false;},error:e=>{this.error=e.message;this.loading=false;}});
 }
}
