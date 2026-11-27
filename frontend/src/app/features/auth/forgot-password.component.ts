import { Component, DestroyRef, ElementRef, Injector, QueryList, ViewChild, ViewChildren, afterNextRender, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize, interval } from 'rxjs';
import { SessionUser } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { BookingIntentService } from '../../core/services/booking-intent.service';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, IconComponent],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private readonly auth = inject(AuthService);
  private readonly bookingIntent = inject(BookingIntentService);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly forms = inject(FormBuilder).nonNullable;

  @ViewChild('stepTitle') private stepTitle?: ElementRef<HTMLElement>;
  @ViewChildren('otpDigit') private otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  readonly emailForm = this.forms.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]]
  });
  readonly passwordForm = this.forms.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/\S/)]],
    confirmPassword: ['', Validators.required]
  });

  step: 'email' | 'code' | 'password' = 'email';
  email = '';
  code = Array<string>(6).fill('');
  error = '';
  status = '';
  busy = false;
  passwordVisible = false;
  confirmationVisible = false;
  private session?: SessionUser;
  private now = Date.now();
  private expiresAt = 0;
  private resendAt = 0;

  constructor() {
    interval(1000).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => this.now = Date.now());
  }

  get timeLeft(): number { return Math.max(0, Math.ceil((this.expiresAt - this.now) / 1000)); }
  get resendIn(): number { return Math.max(0, Math.ceil((this.resendAt - this.now) / 1000)); }
  get timer(): string {
    return `${Math.floor(this.timeLeft / 60)}:${String(this.timeLeft % 60).padStart(2, '0')}`;
  }
  get codeComplete(): boolean { return this.code.every(digit => digit !== ''); }
  get passwordTooLong(): boolean {
    return new TextEncoder().encode(this.passwordForm.controls.newPassword.value).length > 72;
  }
  get passwordsDiffer(): boolean {
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    return confirmPassword !== newPassword;
  }
  get strength(): number {
    const password = this.passwordForm.controls.newPassword.value;
    if (password.trim().length < 8) return 0;
    const variety = [/[a-z]/i, /\d/, /[^\p{L}\p{N}\s]/u].filter(pattern => pattern.test(password)).length;
    return password.length >= 14 && variety >= 2 ? 3 : password.length >= 11 || variety >= 2 ? 2 : 1;
  }
  get strengthLabel(): string {
    return ['Insuffisant', 'Faible', 'Moyen', 'Fort'][this.strength];
  }

  requestCode(resend = false): void {
    if (this.busy || (resend && this.resendIn > 0)) return;
    this.emailForm.markAllAsTouched();
    if (this.emailForm.invalid) return;

    const email = this.emailForm.controls.email.value.trim();
    this.run(this.auth.requestRecovery(email), timing => {
      this.email = email;
      this.now = Date.now();
      this.expiresAt = this.now + timing.expiresInSeconds * 1000;
      this.resendAt = this.now + timing.resendAfterSeconds * 1000;
      this.code = Array<string>(6).fill('');
      this.changeStep('code');
      if (resend) this.status = 'Nouvelle demande prise en compte.';
    });
  }

  verifyCode(): void {
    if (!this.codeComplete || !this.timeLeft || this.busy) return;
    this.run(this.auth.verifyRecovery(this.email, this.code.join('')), session => {
      this.session = session;
      this.code = Array<string>(6).fill('');
      this.changeStep('password');
    });
  }

  savePassword(): void {
    if (!this.session || this.busy) return;
    this.error = '';
    this.passwordForm.markAllAsTouched();
    if (this.passwordForm.invalid || this.passwordTooLong || this.passwordsDiffer) return;
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    this.run(this.auth.resetRecoveredPassword(newPassword, confirmPassword), () => this.enterWorkspace());
  }

  skipPassword(): void {
    if (!this.session || this.busy) return;
    this.run(this.auth.skipRecoveryPassword(), () => this.enterWorkspace());
  }

  editEmail(): void {
    this.error = '';
    this.changeStep('email');
  }

  digitInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    this.fillDigits(index, input.value);
    input.value = this.code[index];
  }

  pasteCode(index: number, event: ClipboardEvent): void {
    event.preventDefault();
    this.fillDigits(index, event.clipboardData?.getData('text') ?? '');
  }

  digitKey(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace' && !this.code[index] && index > 0) {
      event.preventDefault();
      this.code[index - 1] = '';
      this.otpInputs.get(index - 1)?.nativeElement.focus();
    }
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      this.otpInputs.get(index + (event.key === 'ArrowRight' ? 1 : -1))?.nativeElement.focus();
    }
  }

  private fillDigits(index: number, text: string): void {
    const digits = text.replace(/[^0-9]/g, '').slice(0, 6);
    if (!digits) {
      this.code[index] = '';
      return;
    }
    const start = digits.length === 6 ? 0 : index;
    [...digits].slice(0, this.code.length - start).forEach((digit, offset) => this.code[start + offset] = digit);
    const next = this.otpInputs.get(Math.min(start + digits.length, this.code.length - 1))?.nativeElement;
    next?.focus();
    next?.select();
  }

  private changeStep(step: typeof this.step): void {
    this.step = step;
    this.status = '';
    afterNextRender(() => this.stepTitle?.nativeElement.focus(), { injector: this.injector });
  }

  private run<T>(request: Observable<T>, done: (value: T) => void): void {
    this.busy = true;
    this.error = '';
    this.status = '';
    request.pipe(takeUntilDestroyed(this.destroy), finalize(() => this.busy = false)).subscribe({
      next: done,
      error: error => this.error = error.message || 'La récupération a été interrompue. Réessayez.'
    });
  }

  private enterWorkspace(): void {
    const user = this.session;
    if (!user) return;
    this.passwordForm.reset();
    const home = user.role === 'ADMIN' ? '/admin/dashboard' : user.role === 'DOCTOR' ? '/medecin' : '/patient';
    void this.router.navigateByUrl(this.bookingIntent.destination(user) ?? home);
  }
}
