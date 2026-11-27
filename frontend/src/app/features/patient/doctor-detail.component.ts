import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';
import { AvailabilityDay, BookingPolicy, DoctorPublicProfile, TimeSlot } from '../../core/models/api.models';
import { ApiService } from '../../core/services/api.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { BookingPromptComponent } from '../../shared/booking-prompt.component';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { IconComponent } from '../../shared/icon.component';
import { PublicDirectoryHeaderComponent } from '../public/public-directory-header.component';

@Component({
  selector: 'app-doctor-detail',
  standalone: true,
  imports: [AvatarComponent, RouterLink, CabinetDatePipe, BookingPromptComponent, PublicDirectoryHeaderComponent, IconComponent],
  templateUrl: './doctor-detail.component.html',
  styleUrl: './doctor-detail.component.css'
})
export class DoctorDetailComponent implements OnInit {
  private destroy = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private api = inject(ApiService);
  private profileRequest?: Subscription;
  private calendarRequest?: Subscription;
  private slotsRequest?: Subscription;
  readonly publicDirectory = this.route.snapshot.data['publicDirectory'] === true;
  readonly backLink = this.publicDirectory ? '/medecins' : '/patient/medecins';
  readonly weekDays = ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'];
  doctorId = '';
  doctor: DoctorPublicProfile | null = null;
  policy: BookingPolicy | null = null;
  loading = true;
  error = '';
  calendarLoading = false;
  calendarError = '';
  calendarDays: AvailabilityDay[] = [];
  month = '';
  selectedDate = '';
  selectedSlot: TimeSlot | null = null;
  slots: TimeSlot[] = [];
  slotsLoading = false;
  slotsError = '';

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroy)).subscribe(params => {
      this.doctorId = params.get('doctorId') ?? '';
      this.loadProfile();
    });
  }

  get name(): string { return this.doctor?.fullName || `${this.doctor?.firstName ?? ''} ${this.doctor?.lastName ?? ''}`.trim(); }
  get monthLabel(): string { return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(this.monthDate()); }
  get leadingDays(): number[] { return Array.from({ length: (this.monthDate().getDay() + 6) % 7 }, (_, index) => index); }
  get bookable(): boolean { return this.doctor?.services.some(service => !!service.name?.trim() && !!service.price?.trim()) ?? false; }
  get selectedDateParam(): string | undefined { return this.selectedDate || undefined; }

  loadProfile(): void {
    this.profileRequest?.unsubscribe();
    this.calendarRequest?.unsubscribe();
    this.clearSelection();
    this.doctor = null;
    this.policy = null;
    this.loading = true;
    this.error = '';
    this.profileRequest = forkJoin({
      doctor: this.api.get<DoctorPublicProfile>(`/doctors/${this.doctorId}`),
      policy: this.api.get<BookingPolicy>(`/doctors/${this.doctorId}/booking-policy`)
    }).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: ({ doctor, policy }) => {
        this.doctor = doctor;
        this.policy = policy;
        this.month = policy.minDate.slice(0, 7);
        this.loading = false;
        this.loadCalendar();
      },
      error: () => {
        this.error = 'Ce profil est indisponible pour le moment.';
        this.loading = false;
      }
    });
  }

  changeMonth(offset: number): void {
    const date = this.monthDate();
    date.setMonth(date.getMonth() + offset);
    const month = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    if (!this.policy || month < this.policy.minDate.slice(0, 7) || month > this.policy.maxDate.slice(0, 7)) return;
    this.clearSelection();
    this.month = month;
    this.loadCalendar();
  }

  loadCalendar(): void {
    this.calendarRequest?.unsubscribe();
    this.calendarLoading = true;
    this.calendarError = '';
    this.calendarDays = [];
    this.calendarRequest = this.api.get<AvailabilityDay[]>(`/doctors/${this.doctorId}/availability-calendar`, { month: this.month })
      .pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: days => { this.calendarDays = days; this.calendarLoading = false; },
        error: () => {
          this.calendarError = 'Impossible de charger le calendrier des disponibilités.';
          this.calendarLoading = false;
        }
      });
  }

  dayAvailable(day: AvailabilityDay): boolean {
    return day.status === 'AVAILABLE' && !!this.policy && day.date >= this.policy.minDate && day.date <= this.policy.maxDate;
  }

  chooseDate(day: AvailabilityDay): void {
    if (!this.dayAvailable(day)) return;
    this.selectedDate = day.date;
    this.loadSlots();
  }

  loadSlots(): void {
    this.slotsRequest?.unsubscribe();
    this.selectedSlot = null;
    this.slots = [];
    this.slotsLoading = true;
    this.slotsError = '';
    this.slotsRequest = this.api.get<TimeSlot[]>(`/doctors/${this.doctorId}/slots`, { date: this.selectedDate })
      .pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: slots => {
          this.slots = slots.filter(slot => slot.status === 'AVAILABLE');
          this.slotsLoading = false;
        },
        error: () => {
          this.slotsError = 'Impossible de charger les horaires disponibles.';
          this.slotsLoading = false;
        }
      });
  }

  private clearSelection(): void {
    this.slotsRequest?.unsubscribe();
    this.selectedDate = '';
    this.selectedSlot = null;
    this.slots = [];
    this.slotsLoading = false;
    this.slotsError = '';
  }

  private monthDate(): Date { return new Date(`${this.month}-01T12:00:00`); }
}
