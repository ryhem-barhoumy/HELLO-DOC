import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, Subscription } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { AppointmentService } from '../../core/services/domain.services';
import { AuthService } from '../../core/services/auth.service';
import { IconComponent } from '../../shared/icon.component';
import { MedicalService, TimeSlot } from '../../core/models/api.models';

interface Doctor { fullName: string; serviceTitle: string; specialty: string; location: string; services: MedicalService[]; }
interface CalendarDay { date: string; status: string; availableSlots: number; }
interface BookingPolicy { timeZone: string; minDate: string; maxDate: string; minimumNoticeMinutes: number; }
@Component({selector:'app-booking',standalone:true,
  imports:[CommonModule,ReactiveFormsModule,RouterLink,IconComponent],
  templateUrl:'./booking.component.html'})
export class BookingComponent implements OnInit {
  private route=inject(ActivatedRoute); private api=inject(ApiService);
  private appointments=inject(AppointmentService); private fb=inject(FormBuilder);
  readonly auth=inject(AuthService); private destroy=inject(DestroyRef);
  private doctorRequest?:Subscription; private calendarRequest?:Subscription; private slotRequest?:Subscription;
  private slotGeneration=0; private calendarGeneration=0;
  doctorId=''; doctor:Doctor|null=null; policy:BookingPolicy|null=null;
  services:MedicalService[]=[]; slots:TimeSlot[]=[]; calendarDays:CalendarDay[]=[];
  loadingDoctor=true; slotsLoading=false; calendarLoading=false; busy=false;
  profileError=''; calendarError=''; slotsError=''; error=''; success='';
  isMobile=typeof window!=='undefined'&&window.matchMedia('(max-width:760px)').matches;
  @HostListener('window:resize') resize(){this.isMobile=window.matchMedia('(max-width:760px)').matches;}
  menuOpen=false; calendarOpen=false; logoutBusy=false;
  month=''; dateOffset=0;
  form=this.fb.nonNullable.group({serviceName:['',Validators.required],date:['',Validators.required],startAt:['',Validators.required],reason:['',Validators.maxLength(500)]});
  constructor(){this.destroy.onDestroy(()=>{this.doctorRequest?.unsubscribe();this.calendarRequest?.unsubscribe();this.slotRequest?.unsubscribe();});}
  ngOnInit(){
    this.form.controls.serviceName.valueChanges.pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>{this.error='';this.loadSlots();});
    this.form.controls.date.valueChanges.pipe(takeUntilDestroyed(this.destroy)).subscribe(()=>{this.error='';this.loadSlots();});
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(params=>{this.doctorId=params.get('doctorId')??'';this.loadDoctor();});
  }
  @HostListener('document:keydown.escape') closeMenus(){
    const wasCalendar=this.calendarOpen;this.menuOpen=false;this.calendarOpen=false;
    if(typeof document!=='undefined'){const account=document.querySelector<HTMLDetailsElement>('.patient-booking .account-menu');if(account?.open){account.open=false;account.querySelector('summary')?.focus();}else if(wasCalendar)document.querySelector<HTMLElement>('.calendar-toggle')?.focus();}
  }
  get selectedService(){return this.services.find(s=>s.name===this.form.controls.serviceName.value);}
  get selectedSlot(){return this.slots.find(s=>s.startsAt===this.form.controls.startAt.value);}
  get selectedDate(){return this.form.controls.date.value;}
  get canSubmit(){return !this.loadingDoctor&&!this.busy&&!this.success&&!this.slotsLoading&&!this.slotsError&&this.form.valid&&!!this.selectedService&&!!this.selectedSlot&&this.validDate(this.selectedDate);}
  get patientName(){const u=this.auth.user();return u?`${u.firstName} ${u.lastName}`:'Mon compte';}
  get monthLabel(){return this.month?new Intl.DateTimeFormat('fr-FR',{month:'long',year:'numeric'}).format(new Date(`${this.month}-01T12:00:00`)):'';}
  get leadingDays(){return this.month?Array.from({length:(new Date(`${this.month}-01T12:00:00`).getDay()+6)%7}):[];}
  get compactDays(){return this.calendarDays.filter(d=>this.policy&&d.date>=this.policy.minDate&&d.date<=this.policy.maxDate).slice(this.dateOffset,this.dateOffset+5);}
  get hasMoreDays(){return this.calendarDays.filter(d=>this.policy&&d.date>=this.policy.minDate&&d.date<=this.policy.maxDate).length>this.dateOffset+5;}
  dateText(date:string,options:Intl.DateTimeFormatOptions={weekday:'long',day:'numeric',month:'long'}){return date?new Intl.DateTimeFormat('fr-FR',options).format(new Date(`${date}T12:00:00`)):'—';}
  time(instant:string){return new Intl.DateTimeFormat('fr-FR',{timeZone:this.policy?.timeZone??'Africa/Tunis',hour:'2-digit',minute:'2-digit'}).format(new Date(instant));}
  validDate(date:string){return !!this.policy&&/^\d{4}-\d{2}-\d{2}$/.test(date)&&date>=this.policy.minDate&&date<=this.policy.maxDate;}
  dayLabel(day:CalendarDay){if(day.status==='AVAILABLE'&&this.validDate(day.date))return `${day.availableSlots} horaire${day.availableSlots>1?'s':''}`;return ({PAST:'Date passée',DAY_OFF:'Fermé',ABSENT:'Indisponible',FULL:'Complet'} as Record<string,string>)[day.status]??'Indisponible';}
  dayEnabled(day:CalendarDay){return !this.busy&&!this.success&&day.status==='AVAILABLE'&&this.validDate(day.date);}
  loadDoctor(preserve=false){
    this.doctorRequest?.unsubscribe();this.slotRequest?.unsubscribe();this.calendarRequest?.unsubscribe();this.slotGeneration++;this.calendarGeneration++;
    this.loadingDoctor=true;this.profileError='';this.slots=[];this.slotsLoading=false;this.calendarLoading=false;
    if(!preserve){this.doctor=null;this.policy=null;this.services=[];this.success='';this.form.reset({}, {emitEvent:false});}
    this.doctorRequest=forkJoin({doctor:this.api.get<Doctor>(`/doctors/${this.doctorId}`),policy:this.api.get<BookingPolicy>(`/doctors/${this.doctorId}/booking-policy`)}).subscribe({
      next:({doctor,policy})=>{this.doctor=doctor;this.policy=policy;this.services=(doctor.services??[]).filter(s=>s.name?.trim()&&s.price?.trim());this.loadingDoctor=false;
        if(!this.selectedService)this.form.controls.serviceName.setValue('',{emitEvent:false});
        const requested=preserve?this.selectedDate:this.route.snapshot.queryParamMap.get('date')??'';
        const date=this.validDate(requested)?requested:'';this.form.controls.date.setValue(date,{emitEvent:false});
        this.month=(date||policy.minDate).slice(0,7);this.loadCalendar();this.loadSlots();},
      error:e=>{this.loadingDoctor=false;this.profileError=e.message??'Impossible de charger le médecin. Réessayez.';}
    });
  }
  loadCalendar(){
    this.calendarRequest?.unsubscribe();const generation=++this.calendarGeneration;
    this.calendarLoading=true;this.calendarError='';this.calendarDays=[];
    this.calendarRequest=this.api.get<CalendarDay[]>(`/doctors/${this.doctorId}/availability-calendar`,{month:this.month}).subscribe({
      next:days=>{if(generation!==this.calendarGeneration)return;this.calendarLoading=false;this.calendarDays=days;const index=days.filter(d=>this.validDate(d.date)).findIndex(d=>d.date===this.selectedDate);this.dateOffset=index<0?0:Math.floor(index/5)*5;},
      error:e=>{if(generation!==this.calendarGeneration)return;this.calendarLoading=false;this.calendarError=e.message??'Impossible de charger les dates.';}
    });
  }
  moveMonth(offset:number){if(!this.policy||this.busy)return;const d=new Date(`${this.month}-01T12:00:00`);d.setMonth(d.getMonth()+offset);const month=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;if(month<this.policy.minDate.slice(0,7)||month>this.policy.maxDate.slice(0,7))return;this.month=month;this.loadCalendar();}
  chooseDate(day:CalendarDay){if(this.dayEnabled(day)){this.form.controls.date.setValue(day.date);const wasOpen=this.calendarOpen;this.calendarOpen=false;if(wasOpen&&typeof document!=='undefined')setTimeout(()=>document.getElementById('booking-hours')?.focus());}}
  chooseSlot(slot:TimeSlot){if(!this.busy&&!this.success)this.form.controls.startAt.setValue(slot.startsAt);}
  loadSlots(){
    this.slotRequest?.unsubscribe();const generation=++this.slotGeneration;
    this.form.controls.startAt.setValue('',{emitEvent:false});this.slots=[];this.slotsError='';this.slotsLoading=false;
    if(!this.selectedService||!this.validDate(this.selectedDate)||this.loadingDoctor)return;
    this.slotsLoading=true;
    this.slotRequest=this.api.get<TimeSlot[]>(`/doctors/${this.doctorId}/slots`,{date:this.selectedDate}).subscribe({
      next:rows=>{if(generation!==this.slotGeneration)return;const unique=new Map<number,TimeSlot>();
        for(const slot of rows){const start=Date.parse(slot.startsAt),end=Date.parse(slot.endsAt);if(slot.status==='AVAILABLE'&&Number.isFinite(start)&&end>start&&new Intl.DateTimeFormat('en-CA',{timeZone:this.policy!.timeZone}).format(new Date(start))===this.selectedDate&&!unique.has(start))unique.set(start,slot);}
        this.slots=[...unique.values()].sort((a,b)=>Date.parse(a.startsAt)-Date.parse(b.startsAt));this.slotsLoading=false;},
      error:e=>{if(generation!==this.slotGeneration)return;this.slotsLoading=false;this.slotsError=e.message??'Impossible de charger les horaires.';}
    });
  }
  submit(){
    if(!this.canSubmit)return;this.busy=true;this.error='';const value=this.form.getRawValue();
    this.appointments.create({doctorId:this.doctorId,serviceName:value.serviceName,startAt:value.startAt,reason:value.reason||undefined,expectedServicePrice:this.selectedService!.price})
      .pipe(takeUntilDestroyed(this.destroy)).subscribe({next:appointment=>{this.busy=false;this.success=appointment.status==='CONFIRMED'?'Rendez-vous confirmé par le médecin.':'Demande envoyée, en attente de confirmation du médecin';this.form.disable({emitEvent:false});if(typeof document!=='undefined')setTimeout(()=>document.querySelector<HTMLElement>('.booking-success')?.focus());},
        error:e=>{this.busy=false;this.error=e.status===409?`${e.message} Les disponibilités et le tarif vont être actualisés.`:e.message;if(e.status===409)this.loadDoctor(true);}});
  }
  logout(){if(this.logoutBusy)return;this.logoutBusy=true;this.auth.logout().subscribe({error:e=>{this.logoutBusy=false;this.error=e.message;}});}
}
