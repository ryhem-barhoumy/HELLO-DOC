import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
@Component({selector:'app-workspace-footer',standalone:true,imports:[RouterLink],template:`
<footer class="workspace-footer"><div><a class="footer-brand" [routerLink]="base"><span aria-hidden="true">+</span> HelloDoc</a><p>{{doctor?'Votre cabinet, simplement.':'Votre santé, simplement.'}}</p></div><nav aria-label="Liens de pied de page"><a [routerLink]="base+'/rendez-vous'">{{doctor?'Mes consultations':'Mes rendez-vous'}}</a><a [routerLink]="base+'/profil'">Mon compte</a></nav><small>© {{year}} HelloDoc</small></footer>`})
export class WorkspaceFooterComponent {
 private auth=inject(AuthService);year=new Date().getFullYear();
 get doctor(){return this.auth.user()?.role==='DOCTOR';}
 get base(){return this.auth.user()?.role==='ADMIN'?'/admin':this.doctor?'/medecin':'/patient';}
}
