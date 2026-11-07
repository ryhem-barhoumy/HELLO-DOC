import { Injectable, inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { ApiService } from './api.service';
import { Appointment, AppointmentRequest, DoctorSummary, Page, RegisterDoctorRequest, RegisterPatientRequest, ReviewRequest } from '../models/api.models';

@Injectable({ providedIn: 'root' }) export class PatientService {
  private api = inject(ApiService);
  register(body: RegisterPatientRequest) { return this.api.get<unknown>('/auth/csrf').pipe(switchMap(()=>this.api.post('/patients/registrations', body))); }
  profile() { return this.api.get('/patients/me'); }
  updateProfile(body: unknown) { return this.api.put('/patients/me', body); }
  doctors(query: Record<string, string | number | undefined>) { return this.api.get<Page<DoctorSummary>>('/doctors', query); }
  appointments(query?: Record<string, string | number | undefined>) { return this.api.get<Page<Appointment>>('/appointments/mine', query); }
}
@Injectable({ providedIn: 'root' }) export class DoctorService {
  private api = inject(ApiService);
  register(body: RegisterDoctorRequest) { return this.api.get<unknown>('/auth/csrf').pipe(switchMap(()=>this.api.post('/doctors/registrations', body))); }
  profile() { return this.api.get('/doctors/me'); }
  appointments(query?: Record<string, string | number | undefined>) { return this.api.get<Page<Appointment>>('/doctors/me/appointments', query); }
  availability() { return this.api.get('/doctors/me/availability'); }
  absences() { return this.api.get('/doctors/me/absences'); }
}
@Injectable({ providedIn: 'root' }) export class AppointmentService {
  private api = inject(ApiService);
  create(body: AppointmentRequest) { return this.api.post<Appointment>('/appointments', body); }
  get(id: string) { return this.api.get<Appointment>(`/appointments/${id}`); }
  cancel(id: string, reason?: string) { return this.api.post(`/appointments/${id}/cancellation`, { reason }); }
  review(id: string, body: ReviewRequest) { return this.api.post(`/appointments/${id}/review`, body); }
}
@Injectable({ providedIn: 'root' }) export class AdminService {
  private api = inject(ApiService);
  dashboard() { return this.api.get('/admin/dashboard'); }
  doctors(query?: Record<string, string | number | undefined>) { return this.api.get('/admin/doctors', query); }
  patients(query?: Record<string, string | number | undefined>) { return this.api.get('/admin/patients', query); }
  appointments(query?: Record<string, string | number | undefined>) { return this.api.get('/admin/appointments', query); }
  decideDoctor(id: string, decision: 'approve' | 'reject', reason?: string) { return this.api.post(`/admin/doctors/${id}/${decision}`, { reason }); }
  setAccountState(role: 'doctors' | 'patients', id: string, active: boolean) { return this.api.patch(`/admin/${role}/${id}/status`, { status: active ? 'ACTIVE' : 'INACTIVE' }); }
  notifications() { return this.api.get('/admin/notifications'); }
  activity(query?: Record<string, string | number | undefined>) { return this.api.get('/admin/activity-logs', query); }
}
@Injectable({ providedIn: 'root' }) export class NotificationService {
  private api = inject(ApiService);
  list() { return this.api.get('/notifications'); }
  markRead(id: string) { return this.api.patch(`/notifications/${id}/read`); }
}
@Injectable({ providedIn: 'root' }) export class ActivityLogService {
  private api = inject(ApiService);
  list(query?: Record<string, string | number | undefined>) { return this.api.get('/admin/activity-logs', query); }
}
