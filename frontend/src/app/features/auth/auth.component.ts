import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PatientService, DoctorService } from '../../core/services/domain.services';
import { RegisterDoctorRequest } from '../../core/models/api.models';

@Component({
  selector: 'app-auth', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
<main class="auth-layout"><aside class="auth-aside"><a class="brand brand-white" routerLink="/"><span class="brand-mark">+</span> hello<span>doc</span></a><div><span class="eyebrow eyebrow-light">PRENDRE SOIN, ENSEMBLE</span><h1>La santé plus proche,<br>à chaque étape.</h1><p>Un espace simple et sécurisé pour organiser vos rendez-vous médicaux.</p><div class="auth-illustration"><span>✚</span><i></i><i></i></div></div><small>© Hello Doc · Votre partenaire santé</small></aside>
<section class="auth-main"><a class="mobile-brand brand" routerLink="/"><span class="brand-mark">+</span> hello<span>doc</span></a><div class="auth-card"><a class="back-link" routerLink="/">← Retour à l’accueil</a><span class="eyebrow">{{ mode === 'login' ? 'HEUREUX DE VOUS REVOIR' : 'REJOIGNEZ HELLO DOC' }}</span><h2>{{ mode === 'login' ? 'Connectez-vous' : mode === 'doctor' ? 'Créer votre espace médecin' : 'Créer votre compte patient' }}</h2><p class="muted">{{ mode === 'login' ? 'Accédez à votre espace personnel.' : mode === 'doctor' ? 'Renseignez votre activité. Votre compte restera en attente de validation.' : 'Quelques informations pour prendre rendez-vous.' }}</p>
<form [formGroup]="form" (ngSubmit)="submit()"><div class="form-grid" [class.single]="mode === 'login'">
<label [hidden]="mode === 'login'">Prénom<input formControlName="firstName" autocomplete="given-name" placeholder="Votre prénom"></label><label [hidden]="mode === 'login'">Nom<input formControlName="lastName" autocomplete="family-name" placeholder="Votre nom"></label>
<label class="full">Adresse e-mail<input type="email" formControlName="email" autocomplete="email" placeholder="nom@exemple.com"><small *ngIf="form.controls.email.touched && form.controls.email.invalid">Saisissez une adresse e-mail valide.</small></label>
<label [hidden]="mode === 'login'">Téléphone<input formControlName="phone" autocomplete="tel" placeholder="+216 …"></label><label [hidden]="mode === 'login'">Adresse<input formControlName="address" autocomplete="street-address" placeholder="Adresse complète"></label>
<ng-container *ngIf="mode === 'doctor'"><label class="full">Nom affiché sur votre profil<input formControlName="serviceTitle" placeholder="Ex. Dr. Sami Ben Ali"></label><label>Spécialité<select formControlName="specialty"><option value="">Choisir une spécialité</option><option *ngFor="let s of specialties">{{s}}</option></select></label><label>Numéro professionnel<input formControlName="licenseNumber" placeholder="Numéro d’inscription"></label><label>Ville<input formControlName="city" placeholder="Votre ville"></label><label>Durée du rendez-vous (minutes)<input type="number" formControlName="durationMinutes" min="5" step="5"></label><label>Début de travail<input type="time" formControlName="workStart"></label><label>Fin de travail<input type="time" formControlName="workEnd"></label><label>Pause entre rendez-vous (minutes)<input type="number" formControlName="pauseMinutes" min="0" step="5"></label><label>Lieu du cabinet<input formControlName="location" placeholder="Adresse du cabinet"></label>
<div class="full"><span class="form-label">Jours de travail</span><div class="days-picker"><label *ngFor="let day of days"><input type="checkbox" [checked]="workingDays.includes(day.value)" (change)="toggleDay(day.value,$any($event.target).checked)">{{day.label}}</label></div></div>
<div class="full services-form" formArrayName="services"><span class="form-label">Services proposés et tarifs</span><div class="service-form-row" *ngFor="let group of services.controls;let i=index" [formGroupName]="i"><input formControlName="name" placeholder="Nom du service"><input type="number" min="0" step="1" formControlName="price" placeholder="Tarif en DT"><button type="button" class="remove-service" (click)="removeService(i)" [disabled]="services.length===1" aria-label="Supprimer ce service">×</button></div><button type="button" class="add-service" (click)="addService()">+ Ajouter un service</button></div>
<label class="full">Description de votre activité<textarea rows="3" formControlName="description" maxlength="2000" placeholder="Présentez votre activité et vos services"></textarea></label></ng-container>
<label class="full">Mot de passe<input type="password" formControlName="password" [attr.autocomplete]="mode === 'login' ? 'current-password' : 'new-password'" placeholder="8 caractères minimum"><small *ngIf="form.controls.password.touched && form.controls.password.invalid">Le mot de passe doit contenir au moins 8 caractères.</small></label>
<label [hidden]="mode === 'login'" class="full">Confirmer le mot de passe<input type="password" formControlName="passwordConfirm" autocomplete="new-password" placeholder="Saisissez à nouveau le mot de passe"></label>
<label [hidden]="mode === 'login'" class="full check-label"><input type="checkbox" formControlName="consent"> J’accepte le traitement de mes informations pour la gestion de mon compte.</label></div>
<p class="form-error" *ngIf="error">{{error}}</p><p class="form-success" *ngIf="success">{{success}}</p><button class="button button-primary submit-button" [disabled]="busy">{{busy?'Envoi…':mode==='login'?'Se connecter':'Créer mon compte'}} <span>→</span></button></form>
<p class="auth-switch">{{mode==='login'?'Vous n’avez pas de compte ?':'Vous avez déjà un compte ?'}} <a [routerLink]="mode==='login'?'/inscription/patient':'/connexion'">{{mode==='login'?'Créer un compte':'Se connecter'}}</a></p><p class="auth-switch" *ngIf="mode==='login'">Vous êtes médecin ? <a routerLink="/inscription/medecin">Créer votre profil professionnel</a></p><p class="auth-switch" *ngIf="mode==='doctor'">L’accès aux fonctions médecin sera activé après validation de votre inscription.</p></div></section></main>`
})
export class AuthComponent {
  private fb=inject(FormBuilder);private route=inject(ActivatedRoute);private router=inject(Router);private auth=inject(AuthService);private patients=inject(PatientService);private doctors=inject(DoctorService);
  mode=this.route.snapshot.data['mode'] as string??'login';busy=false;error='';success='';workingDays=['1','2','3','4','5','6'];
  specialties=['Médecin généraliste','Dentiste / Orthodontiste','Ophtalmologue','Gynécologue','Pédiatre','ORL','Dermatologue','Neurologue','Cardiologue'];
  days=[{value:'1',label:'Lun'},{value:'2',label:'Mar'},{value:'3',label:'Mer'},{value:'4',label:'Jeu'},{value:'5',label:'Ven'},{value:'6',label:'Sam'},{value:'0',label:'Dim'}];
  form=this.fb.group({firstName:[''],lastName:[''],email:['',[Validators.required,Validators.email]],phone:[''],address:[''],password:['',[Validators.required,Validators.minLength(8)]],passwordConfirm:[''],consent:[false],serviceTitle:[''],specialty:[''],licenseNumber:[''],durationMinutes:[30],workStart:['08:00'],workEnd:['17:00'],pauseMinutes:[0],location:[''],city:[''],description:[''],services:this.fb.array([this.newService()])});
  get services(){return this.form.controls.services as FormArray;}
  private newService(){return this.fb.group({name:[''],price:[null as number|null]});}
  addService(){this.services.push(this.newService());}
  removeService(index:number){if(this.services.length>1)this.services.removeAt(index);}
  toggleDay(value:string,checked:boolean){this.workingDays=checked?[...new Set([...this.workingDays,value])]:this.workingDays.filter(d=>d!==value);}
  submit(){this.error='';this.success='';this.form.markAllAsTouched();if(this.form.invalid){this.error='Vérifiez les champs obligatoires et leur format.';return;}
    const v=this.form.getRawValue();
    if(this.mode!=='login'&&(!v.firstName?.trim()||!v.lastName?.trim()||!v.address?.trim()||(this.mode==='doctor'&&!v.phone?.trim()))){this.error='Le nom, le prénom et l’adresse sont obligatoires; le médecin doit aussi renseigner son téléphone.';return;}
    if(this.mode!=='login'&&v.password!==v.passwordConfirm){this.error='Les deux mots de passe ne correspondent pas.';return;}
    if(this.mode!=='login'&&!v.consent){this.error='Veuillez accepter le traitement de vos informations pour créer le compte.';return;}
    if(this.mode==='doctor'){
      if(!v.serviceTitle?.trim()||!v.specialty||!v.licenseNumber?.trim()||!v.city?.trim()||!v.location?.trim()||!v.description?.trim()){this.error='Complétez les informations professionnelles obligatoires.';return;}
      if(!v.workStart||!v.workEnd||v.workEnd<=v.workStart){this.error='L’heure de fin doit être après l’heure de début.';return;}
      if(!this.workingDays.length){this.error='Sélectionnez au moins un jour de travail.';return;}
      const services=v.services.filter(s=>s.name?.trim()&&s.price!==null&&Number(s.price)>=0).map(s=>({name:s.name!.trim(),price:`${s.price} DT`}));
      if(!services.length){this.error='Ajoutez au moins un service avec un tarif valide.';return;}
    }
    this.busy=true;const done=()=>this.busy=false;
    if(this.mode==='login')this.auth.login({email:v.email!,password:v.password!}).subscribe({next:user=>{done();void this.router.navigateByUrl(user.role==='ADMIN'?'/admin':user.role==='DOCTOR'?'/medecin':'/patient');},error:e=>{done();this.error=e.message;}});
    else if(this.mode==='doctor'){
      const services=v.services.filter(s=>s.name?.trim()&&s.price!==null&&Number(s.price)>=0).map(s=>({name:s.name!.trim(),price:`${s.price} DT`}));
      const request:RegisterDoctorRequest={firstName:v.firstName!,lastName:v.lastName!,email:v.email!,phone:v.phone!,address:v.address!,password:v.password!,consent:true,specialty:v.specialty!,licenseNumber:v.licenseNumber!,serviceTitle:v.serviceTitle!,durationMinutes:Number(v.durationMinutes),workStart:v.workStart!,workEnd:v.workEnd!,workingDays:this.workingDays,pauseMinutes:Number(v.pauseMinutes),location:v.location!,city:v.city!,description:v.description!,services};
      this.doctors.register(request).subscribe({next:()=>{done();this.success='Demande envoyée. Votre compte restera en attente de validation par l’équipe Hello Doc.';},error:e=>{done();this.error=e.message;}});
    }else this.patients.register({firstName:v.firstName!,lastName:v.lastName!,email:v.email!,phone:v.phone!,address:v.address!,password:v.password!,consent:true}).subscribe({next:()=>{done();this.success='Compte créé. Vous pouvez maintenant vous connecter.';},error:e=>{done();this.error=e.message;}});
  }
}
