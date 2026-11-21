import { Component, DestroyRef, ElementRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { EditorDialogComponent } from '../../shared/editor-dialog.component';
import { IconComponent } from '../../shared/icon.component';
import { AvailabilityRow, DoctorProfile, ScheduleDay, ScheduleDraft, ScheduleWindow, copySchedule, minutes, scheduleErrors, scheduleFromApi, schedulePayload } from './doctor-editor.models';

@Component({selector:'app-weekly-availability',standalone:true,imports:[CommonModule,FormsModule,RouterLink,EditorDialogComponent,IconComponent],templateUrl:'./weekly-availability.component.html'})
export class WeeklyAvailabilityComponent implements OnInit {
 private api=inject(ApiService);private destroy=inject(DestroyRef);private element=inject<ElementRef<HTMLElement>>(ElementRef);
 draft:ScheduleDraft|null=null;loading=true;busy=false;loadError='';error='';notice='';private baseline='';touched:Record<string,boolean>={};submitted=false;
 copySource:ScheduleDay|null=null;copyTargets:Record<number,boolean>={};replaceAcknowledged=false;removeCandidate:{day:ScheduleDay;window:ScheduleWindow}|null=null;
 get saving(){return this.busy;}
 get errors(){return this.draft?scheduleErrors(this.draft):{};}
 get activeDays(){return this.draft?.days.filter(day=>day.active).length??0;}
 get hasAnyError(){return Object.keys(this.errors).length>0;}
 hasUnsavedChanges(){return !!this.draft&&JSON.stringify(this.draft)!==this.baseline;}
 get selectedTargets(){return this.draft?.days.filter(day=>day!==this.copySource&&this.copyTargets[day.isoWeekday])??[];}
 get existingTargets(){return this.selectedTargets.filter(day=>day.active&&day.windows.length>0);}
 get totalHours(){let total=0;for(const day of this.draft?.days??[])if(day.active)for(const w of day.windows){const length=minutes(w.endTime)-minutes(w.startTime);if(Number.isFinite(length)&&length>0)total+=length;}return `${Math.floor(total/60)} h${total%60?' '+String(total%60).padStart(2,'0'):''}`;}
 ngOnInit(){this.load();}
 load(){this.loading=true;this.loadError='';forkJoin({rows:this.api.get<AvailabilityRow[]>('/doctors/me/availabilities'),profile:this.api.get<DoctorProfile>('/doctors/me')}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:({rows,profile})=>{this.draft=scheduleFromApi(rows,profile);this.baseline=JSON.stringify(this.draft);this.loading=false;},error:e=>{this.loading=false;this.loadError=e.message;}});}
 changed(){this.notice='';this.error='';}
 touch(key:string){this.touched[key]=true;}
 visibleError(key:string){return this.submitted||this.touched[key]?this.errors[key]??'':'';}
 toggle(day:ScheduleDay,active:boolean){day.active=active;if(active&&!day.windows.length)this.addWindow(day);this.changed();}
 addWindow(day:ScheduleDay){if(day.windows.length>=2||this.busy)return;const period=day.windows.some(w=>w.period===1)?2:1;day.windows.push({period,startTime:'',endTime:''});day.windows.sort((a,b)=>a.period-b.period);this.changed();}
 removeWindow(){if(!this.removeCandidate)return;const {day,window}=this.removeCandidate;day.windows=day.windows.filter(w=>w!==window);this.removeCandidate=null;this.changed();}
 openCopy(day:ScheduleDay){this.copySource=day;this.copyTargets={};this.replaceAcknowledged=false;}
 applyCopy(){if(!this.copySource||!this.selectedTargets.length||(this.existingTargets.length&&!this.replaceAcknowledged))return;for(const target of this.selectedTargets)copySchedule(this.copySource,target);this.copySource=null;this.changed();}
 save(){if(this.busy||!this.draft)return;this.submitted=true;this.notice='';this.error='';if(this.hasAnyError){this.error='Vérifiez les horaires signalés avant d’enregistrer.';setTimeout(()=>this.element.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());return;}this.busy=true;
  this.api.put<AvailabilityRow[]>('/doctors/me/availabilities/weekly',schedulePayload(this.draft)).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:rows=>{const current=this.draft!;const normalized=scheduleFromApi(rows,{durationMinutes:current.slotDurationMinutes,pauseMinutes:current.pauseMinutes});
   // Closed days keep their draft hours so toggling a day back on remains reversible.
   for(const day of normalized.days)if(!day.active){const old=current.days.find(d=>d.isoWeekday===day.isoWeekday);if(old)day.windows=old.windows.map(w=>({...w}));}
   this.draft=normalized;this.baseline=JSON.stringify(normalized);this.busy=false;this.submitted=false;this.touched={};this.notice='Vos disponibilités ont été enregistrées.';
  },error:e=>{this.busy=false;this.error=e.message||'L’enregistrement a échoué. Vos modifications sont conservées.';}});
 }
}
