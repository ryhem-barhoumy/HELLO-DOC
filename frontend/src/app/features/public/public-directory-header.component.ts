import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-public-directory-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header>
      <a class="brand" routerLink="/" aria-label="HelloDoc, accueil"><span class="brand-mark" aria-hidden="true">+</span> hello<span>doc</span></a>
      <nav aria-label="Navigation publique">
        <a routerLink="/medecins" routerLinkActive="current" ariaCurrentWhenActive="page">Trouver un médecin</a>
        <a class="button button-secondary" routerLink="/connexion">Se connecter</a>
      </nav>
    </header>`,
  styles: [`
    header { min-height: 68px; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; background: white; border-bottom: 1px solid var(--line); padding: 10px max(20px, calc((100vw - 1160px) / 2)); }
    nav { display: flex; flex-wrap: wrap; align-items: center; gap: clamp(10px, 3vw, 24px); font-size: 14px; }
    .current { color: var(--green-dark); font-weight: 700; }
    @media (max-width: 420px) { header { justify-content: space-between; } nav > a:first-child { display: none; } }
  `]
})
export class PublicDirectoryHeaderComponent {}
