export type Role = 'ADMIN' | 'DOCTOR' | 'PATIENT' | 'USER';
export type AccountStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ACTIVE' | 'INACTIVE';
export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'REJECTED' | 'CANCELLED' | 'NO_SHOW' | 'TO_RESCHEDULE';
export type ConsultationMode = 'IN_PERSON' | 'VIDEO';
export interface SessionUser { id: string; firstName: string; lastName: string; email: string; role: Role; status: AccountStatus; photoUrl?:string|null; }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface DoctorSummary { id: string; firstName: string; lastName: string; fullName?: string; serviceTitle?: string; specialty: string; city: string; location?: string; rating?: number; status: AccountStatus; videoConsultationEnabled?:boolean; }
export interface TimeSlot { id: string; startsAt: string; endsAt: string; status: 'AVAILABLE' | 'BOOKED' | 'BLOCKED'; }
export interface Appointment { id: string; startsAt: string; endsAt: string; status: AppointmentStatus; serviceName: string; servicePrice?: string; patientName?: string; doctorName?: string; location?: string; reason?: string; cancellationReason?: string; statusHistory?: { oldStatus?: AppointmentStatus; newStatus: AppointmentStatus; changedAt: string; actorName: string; reason?: string }[]; review?: ReviewRequest; consultationMode?:ConsultationMode; }
export interface AppointmentRequest { expectedServicePrice?: string; doctorId: string; serviceName: string; startAt: string; reason?: string; consultationMode?:ConsultationMode; }
export interface ReviewRequest { rating: number; recommend: 'YES' | 'NO'; comment?: string; }
export interface ConsultationNote { id?:string; resume: string; observation: string; treatment: string; nextCheck: string|null; recommendedDelay?:string|null; followUpInstructions?:string|null; privateNote: string; version:number; createdAt:string; updatedAt:string; status?:'DRAFT'|'FINALIZED'; }
export interface ApiError { status: number; message: string; code?: string; fieldErrors?: Record<string, string>; }
export interface LoginRequest { email: string; password: string; }
export interface MedicalService { id?: string; name: string; price: string; }
export interface RegisterPatientRequest { firstName: string; lastName: string; email: string; phone: string; address: string; password: string; consent: boolean; }
export interface RegisterDoctorRequest extends RegisterPatientRequest { specialty: string; licenseNumber: string; city: string; }
