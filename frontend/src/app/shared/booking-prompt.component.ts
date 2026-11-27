import { Component, DestroyRef, ElementRef, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { SessionUser } from '../core/models/api.models';
import { AuthService } from '../core/services/auth.service';
import { BookingIntentService, BookingSelection, isPatientSession } from '../core/services/booking-intent.service';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-booking-prompt',
  standalone: true,
  imports: [RouterLink, IconComponent],
  template: `
    <button class="button button-primary" type="button" [disabled]="checking() || disabled()" (click)="book()">
      {{ checking() ? 'Vérification…' : 'Prendre rendez-vous' }}
    </button>
    <dialog #dialog [attr.aria-label]="'Réserver avec ' + doctorName()">
      <div class="prompt-symbol"><app-icon name="rendez-vous" /></div>
      <h2>Réserver votre consultation</h2>
      @if (accountRestricted) {
        <p>La réservation est disponible depuis un compte patient actif.</p>
      } @else {
        <p>Pour réserver une consultation, connectez-vous ou créez votre compte patient.</p>
        <div class="prompt-actions">
          <a class="button button-primary" routerLink="/inscription/patient" (click)="remember()">Créer un compte</a>
          <a class="button button-secondary" routerLink="/connexion" (click)="remember()">Se connecter</a>
        </div>
      }
      <button class="continue" type="button" (click)="dialog.close()">Continuer la navigation</button>
    </dialog>`,
  styles: [`
    :host { display: inline-block; }
    :host > button { min-height: 42px; white-space: nowrap; }
    dialog { width: min(440px, calc(100vw - 32px)); padding: 26px; border: 1px solid var(--line); border-radius: 18px; color: var(--ink); box-shadow: 0 18px 55px #0f382833; }
    dialog::backdrop { background: #13282278; }
    .prompt-symbol { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 12px; background: var(--mint); color: var(--green-dark); }
    h2 { margin: 14px 0 8px; font-size: 1.35rem; }
    p { margin: 0; color: #4b665d; line-height: 1.55; }
    .prompt-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 18px; }
    .continue { min-height: 44px; border: 0; background: transparent; color: var(--green-dark); font-weight: 650; text-decoration: underline; padding-inline: 0; margin-top: 8px; }
    @media (max-width: 420px) { .prompt-actions > a { flex: 1 1 100%; justify-content: center; } }
  `]
})
export class BookingPromptComponent {
  private dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private auth = inject(AuthService);
  private intent = inject(BookingIntentService);
  private router = inject(Router);
  private destroy = inject(DestroyRef);
  readonly doctorName = input.required<string>();
  readonly selection = input.required<BookingSelection>();
  readonly disabled = input(false);
  readonly checking = signal(false);
  accountRestricted = false;

  book(): void {
    const user = this.auth.user();
    if (user) {
      this.open(user);
      return;
    }
    this.checking.set(true);
    this.auth.me().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: session => this.open(session),
      error: () => this.open(null)
    });
  }

  remember(): void { this.intent.remember(this.selection()); }

  private open(user: SessionUser | null): void {
    this.checking.set(false);
    if (isPatientSession(user)) {
      this.intent.clear();
      void this.router.navigateByUrl(this.intent.target(this.selection()));
      return;
    }
    this.accountRestricted = user !== null;
    this.dialog().nativeElement.showModal();
  }
}
