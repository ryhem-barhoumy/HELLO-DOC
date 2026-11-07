export type Role = 'ADMIN' | 'DOCTOR' | 'PATIENT' | 'USER';
export type AccountStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ACTIVE' | 'INACTIVE';
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'TO_RESCHEDULE' | 'COMPLETED' | 'RESCHEDULED';
export interface SessionUser { id: string; firstName: string; lastName: string; email: string; role: Role; status: AccountStatus; }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface DoctorSummary { id: string; firstName: string; lastName: string; fullName?: string; serviceTitle?: string; specialty: string; city: string; location?: string; rating?: number; status: AccountStatus; }
export interface TimeSlot { id: string; startsAt: string; endsAt: string; status: 'AVAILABLE' | 'BOOKED' | 'BLOCKED'; }
export interface Appointment { id: string; startsAt: string; endsAt?: string; status: AppointmentStatus; serviceName?: string; price?: string; patientName?: string; doctorName?: string; reason?: string; cancellationReason?: string; review?: ReviewRequest; }
export interface AppointmentRequest { doctorId: string; availabilityId: string; startsAt: string; serviceId: string; timezone: string; reason?: string; patientName?: string; phone?: string; email?: string; address?: string; }
export interface ReviewRequest { rating: number; recommend: 'YES' | 'NO'; comment?: string; }
export interface ConsultationNote { resume?: string; observation?: string; treatment?: string; nextCheck?: string; privateNote?: string; }
export interface ApiError { status: number; message: string; code?: string; fieldErrors?: Record<string, string>; }
export interface LoginRequest { email: string; password: string; }
export interface MedicalService { id?: string; name: string; price: string; }
export interface RegisterPatientRequest { firstName: string; lastName: string; email: string; phone: string; address: string; password: string; consent: boolean; }
export interface RegisterDoctorRequest extends RegisterPatientRequest { specialty: string; licenseNumber: string; serviceTitle: string; durationMinutes: number; workStart: string; workEnd: string; workingDays: string[]; pauseMinutes: number; location: string; city: string; description: string; services: MedicalService[]; }
