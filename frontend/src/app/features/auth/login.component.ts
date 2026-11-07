import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
<main class="auth-layout"><aside class="auth-aside"><a class="brand brand-white" routerLink="/"><span class="brand-mark">+</span> hello<span>doc</span></a><div><span class="eyebrow eyebrow-light">PRENDRE SOIN, ENSEMBLE</span><h1>La santé plus proche,<br>à chaque étape.</h1><p>Un espace simple et sécurisé pour organiser vos rendez-vous médicaux.</p><div class="auth-illustration"><span>✚</span><i></i><i></i></div></div><small>© Hello Doc · Votre partenaire santé</small></aside>
<section class="auth-main"><a class="mobile-brand brand" routerLink="/"><span class="brand-mark">+</span> hello<span>doc</span></a><div class="auth-card login-card"><a class="back-link" routerLink="/">← Retour à l’accueil</a><span class="eyebrow">HEUREUX DE VOUS REVOIR</span><h2>Connectez-vous</h2><p class="muted">Accédez à votre espace personnel.</p>
<form [formGroup]="form" (ngSubmit)="submit()" novalidate><div class="form-grid single"><label class="full">Adresse e-mail<input type="email" formControlName="email" autocomplete="username" inputmode="email" placeholder="nom@exemple.com" [attr.aria-invalid]="form.controls.email.touched && form.controls.email.invalid"><small class="auth-field-error" *ngIf="form.controls.email.touched && form.controls.email.invalid">Saisissez une adresse e-mail valide.</small></label>
<label class="full">Mot de passe<span class="auth-password-control"><input [type]="passwordVisible ? 'text' : 'password'" formControlName="password" autocomplete="current-password" placeholder="Votre mot de passe" [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid"><button class="password-toggle" type="button" (click)="passwordVisible=!passwordVisible" [attr.aria-label]="passwordVisible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'" [attr.aria-pressed]="passwordVisible">{{passwordVisible?'Masquer':'Afficher'}}</button></span><small class="auth-field-error" *ngIf="form.controls.password.touched && form.controls.password.hasError('required')">Saisissez votre mot de passe.</small><small class="auth-field-error" *ngIf="form.controls.password.touched && form.controls.password.hasError('minlength')">Le mot de passe doit contenir au moins 8 caractères.</small></label></div>
<div class="login-assist-row"><a routerLink="/mot-de-passe-oublie">Mot de passe oublié ?</a></div><p class="form-error" role="alert" *ngIf="error">{{error}}</p><button class="button button-primary submit-button" type="submit" [disabled]="busy">{{busy?'Connexion…':'Se connecter'}} <span>→</span></button></form>
<p class="auth-switch">Vous n’avez pas de compte ? <a routerLink="/inscription/patient">Créer un compte</a></p><p class="auth-switch">Vous êtes médecin ? <a routerLink="/inscription/medecin">Créer votre profil professionnel</a></p></div></section></main>`
})
export class LoginComponent {
  private fb=inject(FormBuilder); private auth=inject(AuthService); private router=inject(Router);
  passwordVisible=false; busy=false; error='';
  form=this.fb.group({email:['',[Validators.required,Validators.email]],password:['',[Validators.required,Validators.minLength(8)]]});
  submit():void { this.form.markAllAsTouched();this.error='';if(this.form.invalid)return;this.busy=true;const {email,password}=this.form.getRawValue();this.auth.login({email:email!,password:password!}).subscribe({next:user=>{this.busy=false;const target=user.role==='ADMIN'?'/admin':user.role==='DOCTOR'?'/medecin':'/patient';void this.router.navigateByUrl(target);},error:e=>{this.busy=false;this.error=e.message??'Impossible de vous connecter. Vérifiez vos identifiants puis réessayez.';}}); }
}