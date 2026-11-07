import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({selector:'app-access-denied',standalone:true,imports:[RouterLink],template:`<main class="public-page"><header class="site-header"><a class="brand" routerLink="/"><span class="brand-mark">+</span> hello<span>doc</span></a></header><section class="empty-panel access-denied"><div class="empty-symbol">!</div><h1>Accès non autorisé</h1><p>Votre compte n’a pas le rôle ou le statut nécessaire pour ouvrir cet espace. Les comptes médecin en attente doivent être approuvés avant d’accéder aux fonctions professionnelles.</p><a routerLink="/" class="button button-primary">Retour à l’accueil</a></section></main>`})
export class AccessDeniedComponent{}
