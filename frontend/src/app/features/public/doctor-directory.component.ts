import { Component, DestroyRef, OnInit, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { DoctorSummary } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { PatientService } from '../../core/services/domain.services';
import { AvatarComponent } from '../../shared/avatar.component';
import { BookingPromptComponent } from '../../shared/booking-prompt.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { IconComponent } from '../../shared/icon.component';
import { PublicDirectoryHeaderComponent } from './public-directory-header.component';

@Component({
  selector: 'app-doctor-directory',
  standalone: true,
  imports: [RouterLink, ReactiveFormsModule, AvatarComponent, CabinetDatePipe, IconComponent, BookingPromptComponent, PublicDirectoryHeaderComponent],
  templateUrl: './doctor-directory.component.html',
  styleUrl: './doctor-directory.component.css'
})
export class DoctorDirectoryComponent implements OnInit {
  readonly embedded = input(false);
  private api = inject(ApiService);
  private patients = inject(PatientService);
  private destroy = inject(DestroyRef);
  readonly filters = inject(FormBuilder).nonNullable.group({ q: '', specialty: '', city: '', availableOn: '' });
  readonly today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Tunis' }).format(new Date());
  private applied = this.filters.getRawValue();
  private request?: Subscription;
  doctors: DoctorSummary[] = [];
  specialties: { id: string; name: string }[] = [];
  loading = true;
  error = '';
  page = 0;
  totalElements = 0;
  totalPages = 0;

  ngOnInit(): void {
    this.api.get<typeof this.specialties>('/specialties').pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: rows => this.specialties = rows,
      error: () => { /* The free-text specialty input remains usable. */ }
    });
    this.load(0);
  }

  search(): void {
    this.applied = this.filters.getRawValue();
    this.load(0);
  }

  reset(): void {
    this.filters.reset();
    this.search();
  }

  load(page: number): void {
    this.request?.unsubscribe();
    this.loading = true;
    this.error = '';
    this.page = page;
    const { q, specialty, city, availableOn } = this.applied;
    this.request = this.patients.doctors({ q: q.trim(), specialty: specialty.trim(), city: city.trim(), availableOn, page, size: 12 })
      .pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: result => {
          this.doctors = result.content;
          this.totalElements = result.totalElements;
          this.totalPages = result.totalPages;
          this.loading = false;
        },
        error: () => {
          this.error = 'Impossible de charger les médecins. Réessayez dans un instant.';
          this.loading = false;
        }
      });
  }

  name(doctor: DoctorSummary): string { return doctor.fullName || `${doctor.firstName} ${doctor.lastName}`; }
}
