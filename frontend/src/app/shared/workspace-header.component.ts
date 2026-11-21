import { AvatarComponent } from './avatar.component';
import { Component, ElementRef, HostListener, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

const adminLinks=[{path:'/admin/dashboard',label:'Vue d’ensemble'},{path:'/admin/approbations',label:'Candidatures'},{path:'/admin/rendez-vous',label:'Rendez-vous'}];
const doctorLinks=[{path:'/medecin/dashboard',label:'Tableau de bord'},{path:'/medecin/rendez-vous',label:'Rendez-vous'},{path:'/medecin/calendrier',label:'Agenda'},{path:'/medecin/disponibilites',label:'Disponibilités'},{path:'/medecin/patients',label:'Mes patients'},{path:'/medecin/notifications',label:'Notifications'}];
const patientLinks=[{path:'/patient/medecins',label:'Trouver un médecin'},{path:'/patient/rendez-vous',label:'Mes rendez-vous'},{path:'/patient/notifications',label:'Notifications'}];
@Component({selector:'app-workspace-header',standalone:true,imports:[CommonModule,RouterLink,RouterLinkActive,AvatarComponent],template:`
<header class="product-header">
 <a class="brand" [routerLink]="base"><span class="brand-mark" aria-hidden="true">+</span> hello<span>doc</span></a>
 <button #menuTrigger type="button" class="nav-toggle" [attr.aria-expanded]="open" aria-controls="workspace-navigation" (click)="open=!open">{{open?'Fermer':'Menu'}}</button>
 <nav id="workspace-navigation" aria-label="Navigation de votre espace" [class.is-open]="open">
  <a *ngFor="let link of links" [routerLink]="link.path" routerLinkActive="active" (click)="open=false">{{link.label}}</a>
  <a [routerLink]="base+'/profil'" (click)="open=false" class="workspace-account"><app-avatar [url]="auth.user()?.photoUrl" [name]="name" [size]="30"></app-avatar>Mon compte</a>
  <button type="button" [disabled]="busy" (click)="logout()">{{busy?'Déconnexion…':'Déconnexion'}}</button>
 </nav>
</header><p class="notice notice-error header-error" role="alert" *ngIf="error">{{error}}</p>`})
export class WorkspaceHeaderComponent {
 @ViewChild('menuTrigger') menuTrigger?:ElementRef<HTMLButtonElement>;
 @HostListener('document:keydown.escape') close(){if(this.open){this.open=false;this.menuTrigger?.nativeElement.focus();}}
 readonly auth=inject(AuthService);open=false;busy=false;error='';
 get name(){const user=this.auth.user();return user?user.firstName+' '+user.lastName:'Mon compte';}
 get base(){return this.auth.user()?.role==='ADMIN'?'/admin':this.auth.user()?.role==='DOCTOR'?'/medecin':'/patient';}
 get links(){return this.base==='/admin'?adminLinks:this.base==='/medecin'?doctorLinks:patientLinks;}
 logout(){if(this.busy)return;this.busy=true;this.error='';this.auth.logout().subscribe({error:()=>{this.busy=false;this.error='La déconnexion n’a pas pu être terminée. Réessayez.';}});}
}
