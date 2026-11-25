import { Component, DestroyRef, ElementRef, EventEmitter, OnInit, Output, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { ProfilePhotoComponent } from '../../shared/profile-photo.component';
import { DigitsOnlyDirective } from '../../shared/digits-only.directive';
import { IconComponent } from '../../shared/icon.component';
import { EditorDialogComponent } from '../../shared/editor-dialog.component';
import { PasswordFieldComponent } from '../../shared/password-field.component';
import { DoctorProfile, DoctorService, servicePrice } from './doctor-editor.models';

const nonBlank=(control:AbstractControl)=>String(control.value??'').trim()?null:{required:true};
@Component({selector:'app-doctor-account',standalone:true,
 imports:[CommonModule,ReactiveFormsModule,RouterLink,AvatarComponent,ProfilePhotoComponent,DigitsOnlyDirective,IconComponent,EditorDialogComponent,PasswordFieldComponent],
 templateUrl:'./doctor-account.component.html'})
export class DoctorAccountComponent implements OnInit {
 private api=inject(ApiService);private auth=inject(AuthService);private fb=inject(FormBuilder);private destroy=inject(DestroyRef);private element=inject<ElementRef<HTMLElement>>(ElementRef);
 @Output() profileChanged=new EventEmitter<DoctorProfile>();@ViewChild(ProfilePhotoComponent) photoEditor?:ProfilePhotoComponent;
 profile:DoctorProfile|null=null;loading=true;loadError='';error='';notice='';busy=false;videoBusy=false;videoEnabled=false;videoDraft=false;section:'personal'|'practice'|'security'='personal';
 photoOpen=false;specialties:string[]=[];specialtiesError='';services:DoctorService[]=[];serverErrors:Record<string,string>={};
 editingService:number|null=null;serviceDialog=false;deleteCandidate:number|null=null;
 readonly form=this.fb.nonNullable.group({
  firstName:['',[nonBlank,Validators.maxLength(100)]],lastName:['',[nonBlank,Validators.maxLength(100)]],
  phone:['',[Validators.pattern(/^(?:|[0-9]{8,15})$/)]],address:['',[nonBlank,Validators.maxLength(300)]],
  serviceTitle:['',[nonBlank,Validators.maxLength(160)]],specialty:['',[nonBlank,Validators.maxLength(120)]],
  location:['',[nonBlank,Validators.maxLength(300)]],description:['',[Validators.maxLength(2000)]],currentPassword:['',[Validators.required]]
 });
 readonly passwordForm=this.fb.nonNullable.group({currentPassword:['',Validators.required],newPassword:['',[Validators.required,Validators.minLength(8),Validators.maxLength(72)]],confirmPassword:['',Validators.required]},{validators:control=>control.get('newPassword')?.value===control.get('confirmPassword')?.value?null:{mismatch:true}});
 readonly serviceForm=this.fb.nonNullable.group({name:['',[nonBlank,Validators.maxLength(160)]],price:['',[nonBlank,Validators.maxLength(40)]]});
 readonly tabs=[{id:'personal',label:'Profil personnel',icon:'profil'},{id:'practice',label:'Cabinet et services',icon:'medecins'},{id:'security',label:'Sécurité',icon:'security'}] as const;
 price=servicePrice;
 get fullName(){return `${this.form.controls.firstName.value} ${this.form.controls.lastName.value}`.trim();}
 get saving(){return this.busy||this.photoEditor?.busy===true;}
 hasUnsavedChanges(){return this.form.dirty||this.passwordForm.dirty||(this.serviceDialog&&this.serviceForm.dirty)||!!this.photoEditor?.preview||this.videoDraft!==this.videoEnabled;}
 ngOnInit(){this.load();this.loadSpecialties();}
 load(){this.loading=true;this.loadError='';this.api.get<DoctorProfile>('/doctors/me').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:p=>{this.acceptProfile(p);this.loading=false;},error:e=>{this.loading=false;this.loadError=e.message;}});}
 loadSpecialties(){this.specialtiesError='';this.api.get<{name:string}[]>('/specialties').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:rows=>this.specialties=[...new Set([this.profile?.specialty,...rows.map(r=>r.name)].filter((v):v is string=>!!v))],error:()=>this.specialtiesError='La liste des spécialités n’a pas pu être chargée. Votre spécialité actuelle est conservée.'});}
 private acceptProfile(p:DoctorProfile){this.profile=p;this.services=p.services.map(s=>({...s}));this.videoEnabled=!!p.videoConsultationEnabled;this.videoDraft=this.videoEnabled;this.form.reset({...p,description:p.description??'',phone:p.phone??'',currentPassword:''});if(p.specialty&&!this.specialties.includes(p.specialty))this.specialties.unshift(p.specialty);}
 selectSection(id:'personal'|'practice'|'security'){this.section=id;this.notice='';this.error='';}
 tabKey(event:KeyboardEvent,index:number){let next=index;if(event.key==='ArrowRight')next=(index+1)%3;else if(event.key==='ArrowLeft')next=(index+2)%3;else if(event.key==='Home')next=0;else if(event.key==='End')next=2;else return;event.preventDefault();this.selectSection(this.tabs[next].id);this.element.nativeElement.querySelector<HTMLElement>(`#tab-${this.tabs[next].id}`)?.focus();}
 fieldError(name:keyof typeof this.form.controls){const c=this.form.controls[name];if(this.serverErrors[name])return this.serverErrors[name];if(!c.touched||!c.invalid)return '';return c.hasError('required')?'Ce champ est obligatoire.':c.hasError('pattern')?'Saisissez entre 8 et 15 chiffres, sans lettres.':'La valeur saisie est trop longue.';}
 passwordError(name:'currentPassword'|'newPassword'|'confirmPassword'){const c=this.passwordForm.controls[name];if(!c.touched)return '';if(c.hasError('required'))return 'Ce champ est obligatoire.';if(c.hasError('minlength')||c.hasError('maxlength'))return 'Utilisez de 8 à 72 caractères.';if(name==='confirmPassword'&&this.passwordForm.hasError('mismatch'))return 'Les deux nouveaux mots de passe ne correspondent pas.';return '';}
 editService(index:number|null){this.editingService=index;this.serviceForm.reset(index===null?{name:'',price:''}:this.services[index]);this.serviceDialog=true;}
 closeService(){if(this.serviceForm.dirty&&!window.confirm('Abandonner les modifications de ce service ?'))return;this.serviceDialog=false;}
 applyService(){this.serviceForm.markAllAsTouched();if(this.serviceForm.invalid)return;const v=this.serviceForm.getRawValue();const service={name:v.name.trim(),price:v.price.trim()};if(this.services.some((s,i)=>i!==this.editingService&&s.name.toLowerCase()===service.name.toLowerCase())){this.serviceForm.controls.name.setErrors({duplicate:true});return;}if(this.editingService===null)this.services=[...this.services,service];else this.services=this.services.map((s,i)=>i===this.editingService?service:s);this.form.markAsDirty();this.serviceDialog=false;}
 deleteService(){if(this.deleteCandidate===null)return;this.services=this.services.filter((_,i)=>i!==this.deleteCandidate);this.form.markAsDirty();this.deleteCandidate=null;}
 save(){if(this.busy||!this.profile)return;this.notice='';this.error='';this.serverErrors={};this.form.markAllAsTouched();
  if(this.form.invalid||!this.services.length){const practice=['serviceTitle','specialty','location','description'] as const;if(!this.services.length||practice.some(k=>this.form.controls[k].invalid)){this.section='practice';this.error='Complétez les informations du cabinet et ajoutez au moins un service pour enregistrer votre profil.';}else{this.section='personal';this.error='Vérifiez vos coordonnées et confirmez avec votre mot de passe actuel.';}setTimeout(()=>this.element.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());return;}
  const value=this.form.getRawValue();const body={...value,firstName:value.firstName.trim(),lastName:value.lastName.trim(),address:value.address.trim(),serviceTitle:value.serviceTitle.trim(),location:value.location.trim(),services:this.services.map(s=>({...s}))};this.busy=true;this.form.disable({emitEvent:false});
  this.api.put<DoctorProfile>('/doctors/me',body).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:p=>{this.busy=false;this.form.enable({emitEvent:false});this.acceptProfile(p);this.auth.user.update(u=>u?{...u,firstName:p.firstName,lastName:p.lastName}:null);this.profileChanged.emit(p);this.notice='Vos modifications ont été enregistrées.';},error:e=>{this.busy=false;this.form.enable({emitEvent:false});this.error=e.message;this.serverErrors=e.fieldErrors??{};}});
 }
 saveVideoSetting(){if(this.videoBusy||this.videoDraft===this.videoEnabled)return;this.videoBusy=true;this.error='';this.api.put<DoctorProfile>('/doctors/me/video-consultation',{enabled:this.videoDraft}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:p=>{this.videoBusy=false;this.profile=p;this.videoEnabled=!!p.videoConsultationEnabled;this.videoDraft=this.videoEnabled;this.profileChanged.emit(p);this.notice=this.videoEnabled?'La téléconsultation vidéo est proposée aux patients.':'La téléconsultation vidéo n’est plus proposée.';},error:e=>{this.videoBusy=false;this.error=e.message;this.videoDraft=this.videoEnabled;}});}
 changePassword(){if(this.busy||this.passwordForm.invalid)return;this.error='';this.notice='';this.passwordForm.markAllAsTouched();const value=this.passwordForm.getRawValue();this.busy=true;this.passwordForm.disable({emitEvent:false});this.api.post('/auth/password',{currentPassword:value.currentPassword,newPassword:value.newPassword}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{this.busy=false;this.passwordForm.enable({emitEvent:false});this.passwordForm.reset();this.notice='Votre mot de passe a été modifié.';},error:e=>{this.busy=false;this.passwordForm.enable({emitEvent:false});this.error=e.status===401&&e.code==='REQUEST_ERROR'?'Le mot de passe actuel est incorrect.':e.message;}});}
 photoChanged(url:string|null){if(this.profile)this.profile.photoUrl=url;}
}
