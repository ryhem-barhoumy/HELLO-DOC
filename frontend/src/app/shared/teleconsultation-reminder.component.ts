import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, exhaustMap, interval, map, of, startWith } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { Appointment, Page } from '../core/models/api.models';

type ReminderStage = 'approaching' | 'available';
interface Reminder { appointmentId: string; stage: ReminderStage; startsAt: string; }

@Component({
  selector: 'app-teleconsultation-reminder',
  standalone: true,
  imports: [CommonModule, RouterLink],
  styleUrl: './teleconsultation-reminder.component.css',
  template: '<aside class="consultation-reminder" *ngIf="reminder as item" role="status" aria-live="polite"><button class="reminder-dismiss" type="button" aria-label="Fermer le rappel" (click)="dismiss()">×</button><strong>{{ title }}</strong><p>{{ message }}</p><a [routerLink]="roomLink(item.appointmentId)">Ouvrir la salle de téléconsultation</a></aside>'
})
export class TeleconsultationReminderComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly destroy = inject(DestroyRef);
  private readonly seen = new Set<string>();
  private readonly userId = this.auth.user()?.id ?? 'session';
  private appointments: Appointment[] = [];
  reminder: Reminder | null = null;

  get title(): string { return this.reminder?.stage === 'approaching' ? 'Téléconsultation à venir' : 'Salle disponible'; }
  get message(): string {
    const current = this.reminder;
    if (!current) return '';
    return current.stage === 'approaching'
      ? 'Votre téléconsultation approche. La communication sera disponible à ' + this.opensAt(current.startsAt) + '.'
      : 'Votre salle de téléconsultation est disponible.';
  }

  ngOnInit(): void {
    const role = this.auth.user()?.role;
    if (role !== 'DOCTOR' && role !== 'PATIENT' && role !== 'USER') return;

    interval(30_000).pipe(
      startWith(0),
      exhaustMap(() => this.loadAppointments(role).pipe(catchError(() => of([] as Appointment[])))),
      takeUntilDestroyed(this.destroy)
    ).subscribe(rows => {
      this.appointments = rows;
      this.evaluate();
    });

    interval(15_000).pipe(takeUntilDestroyed(this.destroy)).subscribe(() => this.evaluate());
  }

  roomLink(appointmentId: string): string[] {
    return this.auth.user()?.role === 'DOCTOR'
      ? ['/medecin/rendez-vous', appointmentId, 'teleconsultation']
      : ['/patient/rendez-vous', appointmentId, 'teleconsultation'];
  }

  opensAt(startsAt: string): string {
    const open = new Date(Date.parse(startsAt) - 15 * 60_000);
    return new Intl.DateTimeFormat('fr-FR', { timeZone: 'Africa/Tunis', hour: '2-digit', minute: '2-digit' }).format(open);
  }

  dismiss(): void { this.reminder = null; }

  private loadAppointments(role: string) {
    if (role === 'DOCTOR') {
      const now = Date.now();
      return this.api.get<Page<Appointment>>('/doctors/me/appointments', {
        from: new Date(now - 30 * 60_000).toISOString(),
        to: new Date(now + 24 * 60 * 60_000).toISOString(),
        page: 0,
        size: 100
      }).pipe(map(page => page.content ?? []));
    }
    return this.api.get<Page<Appointment>>('/appointments/mine', { page: 0, size: 100 })
      .pipe(map(page => page.content ?? []));
  }

  private evaluate(): void {
    const now = Date.now();
    if (this.reminder && Date.parse(this.reminder.startsAt) + 30 * 60_000 < now) this.reminder = null;

    const candidates = this.appointments
      .filter(a => a.status === 'CONFIRMED' && a.consultationMode === 'VIDEO' && !!a.id && Number.isFinite(Date.parse(a.startsAt)))
      .sort((a, b) => Date.parse(a.startsAt) - Date.parse(b.startsAt));

    for (const appointment of candidates) {
      const starts = Date.parse(appointment.startsAt);
      const open = starts - 15 * 60_000;
      const stage: ReminderStage | null =
        now >= open && now <= starts + 30 * 60_000 ? 'available' :
        now >= open - 5 * 60_000 && now < open ? 'approaching' : null;
      if (!stage) continue;

      if (this.reminder?.appointmentId === appointment.id) {
        if (this.reminder.stage === 'approaching' && stage === 'available') this.publish(appointment, stage);
        return;
      }
      if (this.reminder) return;
      if (this.hasSeen(appointment.id, stage)) continue;
      this.publish(appointment, stage);
      return;
    }
  }

  private hasSeen(id: string, stage: ReminderStage): boolean {
    const key = this.storageKey(id, stage);
    if (this.seen.has(key)) return true;
    try { return sessionStorage.getItem(key) === '1'; } catch { return false; }
  }

  private publish(appointment: Appointment, stage: ReminderStage): void {
    const key = this.storageKey(appointment.id, stage);
    this.seen.add(key);
    try { sessionStorage.setItem(key, '1'); } catch { /* In-memory suppression still applies for this instance. */ }
    this.reminder = { appointmentId: appointment.id, startsAt: appointment.startsAt, stage };
  }

  private storageKey(id: string, stage: ReminderStage): string {
    return 'hellodoc-consultation-reminder:' + this.userId + ':' + id + ':' + stage;
  }
}
