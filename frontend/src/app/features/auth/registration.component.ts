import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PatientService, DoctorService } from '../../core/services/domain.services';
import { ApiService } from '../../core/services/api.service';
import { DigitsOnlyDirective } from '../../shared/digits-only.directive';

@Component({
  selector: 'app-registration', standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DigitsOnlyDirective],
  templateUrl: './registration.component.html'
})
export class RegistrationComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private patients = inject(PatientService);
  private doctors = inject(DoctorService);
  private api = inject(ApiService);
  mode: 'patient' | 'doctor' = this.route.snapshot.data['mode'] === 'doctor' ? 'doctor' : 'patient';
  busy = false; error = ''; success = ''; specialties: string[] = [];
  specialtiesLoading = false; specialtiesError = ''; doctorDocument: File | null = null; documentError = '';
  form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100), Validators.pattern(/.*\S.*/)]],
    lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100), Validators.pattern(/.*\S.*/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254), Validators.pattern(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)]],
    phone: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
    address: ['', [Validators.required, Validators.maxLength(300), Validators.pattern(/.*\S.*/)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    passwordConfirm: ['', Validators.required], consent: [false, Validators.requiredTrue],
    specialty: [''], licenseNumber: [''], city: ['']
  });
  ngOnInit(): void {
    if (this.mode === 'doctor') {
      for (const key of ['specialty', 'licenseNumber', 'city'] as const) {
        this.form.controls[key].addValidators([Validators.required, Validators.maxLength(key === 'licenseNumber' ? 100 : 120), Validators.pattern(/.*\S.*/)]);
        this.form.controls[key].updateValueAndValidity();
      }
      this.loadSpecialties();
    }
  }
  message(name: keyof typeof this.form.controls): string {
    const c = this.form.controls[name];
    if (!c.touched || !c.errors) return '';
    if (c.errors['server']) return c.errors['server'];
    if (c.errors['required']) return name === 'consent' ? 'Votre accord est nécessaire pour créer le compte.' : 'Ce champ est obligatoire.';
    if (name === 'email') return 'Saisissez une adresse e-mail valide, par exemple nom@exemple.com.';
    if (name === 'phone') return 'Saisissez entre 8 et 15 chiffres, sans lettres, espaces ni signe +.';
    if (c.errors['minlength']) return name === 'password' ? 'Utilisez au moins 8 caractères.' : 'Utilisez au moins 2 caractères.';
    if (c.errors['maxlength']) return 'Cette valeur est trop longue.';
    return 'Renseignez une valeur valide.';
  }
  loadSpecialties(): void {
    this.specialtiesLoading = true; this.specialtiesError = '';
    this.api.get<Array<{ name: string }>>('/specialties').subscribe({
      next: rows => { this.specialties = rows.map(row => row.name); this.specialtiesLoading = false; },
      error: () => { this.specialtiesLoading = false; this.specialtiesError = 'Impossible de charger les spécialités. Réessayez dans un instant.'; }
    });
  }
  selectDocument(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.documentError = ''; this.doctorDocument = null;
    if (!file) return;
    if (!/\.(pdf|jpe?g|png)$/i.test(file.name)) this.documentError = 'Choisissez un fichier PDF, JPG ou PNG.';
    else if (!file.size || file.size > 5 * 1024 * 1024) this.documentError = 'Choisissez un fichier non vide de 5 Mo maximum.';
    else this.doctorDocument = file;
    if (this.documentError) input.value = '';
  }
  submit(): void {
    if (this.busy || this.success) return;
    this.error = ''; this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (this.form.invalid) { this.error = 'Vérifiez les champs indiqués avant de continuer.'; return; }
    if (value.password !== value.passwordConfirm) { this.form.controls.passwordConfirm.setErrors({server: 'Les deux mots de passe doivent être identiques.'}); return; }
    if (this.mode === 'doctor' && !this.doctorDocument) { this.documentError = 'Joignez votre attestation d’inscription à l’Ordre avec cachet.'; return; }
    const body = { firstName: value.firstName.trim(), lastName: value.lastName.trim(), email: value.email.trim(),
      phone: value.phone, address: value.address.trim(), password: value.password, consent: value.consent };
    this.busy = true;
    const request = this.mode === 'doctor'
      ? this.doctors.register({...body, specialty: value.specialty, licenseNumber: value.licenseNumber.trim(), city: value.city.trim()}, this.doctorDocument!)
      : this.patients.register(body);
    request.subscribe({
      next: () => { this.busy = false; this.success = this.mode === 'doctor'
        ? 'Votre demande a été envoyée. Après validation, vous pourrez vous connecter et configurer vos services, tarifs et horaires.'
        : 'Votre compte est créé. Vous pouvez maintenant vous connecter.'; this.form.controls.password.reset(); this.form.controls.passwordConfirm.reset(); },
      error: e => { this.busy = false; this.error = e.message;
        for (const [name, message] of Object.entries(e.fieldErrors ?? {})) {
          const c = this.form.get(name); if (c) { c.setErrors({server: message}); c.markAsTouched(); }
        }
      }
    });
  }
}
