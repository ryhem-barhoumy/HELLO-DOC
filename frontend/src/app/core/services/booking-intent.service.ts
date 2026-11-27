import { Injectable, inject } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { SessionUser } from '../models/api.models';

export interface BookingSelection {
  doctorId: string;
  date?: string;
  startAt?: string;
}

const storageKey = 'hellodoc.booking-intent';
const durationMs = 30 * 60 * 1000;
const doctorIdPattern = /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;
type BookingIntent = BookingSelection & { expiresAt: number };

export function isPatientSession(user: SessionUser | null): boolean {
  return user?.status === 'ACTIVE' && (user.role === 'PATIENT' || user.role === 'USER');
}

@Injectable({ providedIn: 'root' })
export class BookingIntentService {
  private router = inject(Router);
  private memory: BookingIntent | null = null;

  remember(selection: BookingSelection): void {
    const intent = { ...selection, expiresAt: Date.now() + durationMs };
    this.memory = intent;
    try { sessionStorage.setItem(storageKey, JSON.stringify(intent)); }
    catch { /* Retain the selection in memory when storage is unavailable. */ }
  }

  get pending(): boolean { return this.restore() !== null; }

  target(selection: BookingSelection): UrlTree {
    return this.router.createUrlTree(['/patient/reserver', selection.doctorId], {
      queryParams: { date: selection.date, startAt: selection.startAt }
    });
  }

  destination(user: SessionUser): UrlTree | null {
    const intent = this.restore();
    if (!intent) return null;
    this.clear();
    return isPatientSession(user) ? this.target(intent) : null;
  }

  clear(): void {
    this.memory = null;
    try { sessionStorage.removeItem(storageKey); }
    catch { /* The in-memory selection was cleared. */ }
  }

  private restore(): BookingIntent | null {
    let intent = this.memory;
    if (!intent) {
      try { intent = JSON.parse(sessionStorage.getItem(storageKey) ?? 'null'); }
      catch { return null; }
    }
    if (intent && typeof intent.expiresAt === 'number' && intent.expiresAt > Date.now()
      && typeof intent.doctorId === 'string' && doctorIdPattern.test(intent.doctorId)) {
      const date = typeof intent.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(intent.date) ? intent.date : undefined;
      const startAt = date && typeof intent.startAt === 'string' && Number.isFinite(Date.parse(intent.startAt)) ? intent.startAt : undefined;
      return { doctorId: intent.doctorId, expiresAt: intent.expiresAt, date, startAt };
    }
    this.clear();
    return null;
  }
}
