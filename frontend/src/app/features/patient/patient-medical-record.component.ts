import { Component, DestroyRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/services/api.service';
import { ConsultationMode } from '../../core/models/api.models';
import { CabinetDatePipe } from '../../shared/cabinet-date.pipe';
import { IconComponent } from '../../shared/icon.component';

interface PatientConsultation {
  appointmentId: string;
  doctorId: string;
  startsAt: string;
  serviceName: string;
  doctorName: string;
  specialty?: string;
  consultationMode?: ConsultationMode;
  note: {
    resume: string;
    observation: string;
    treatment: string;
    nextCheck: string | null;
    recommendedDelay?: string | null;
    followUpInstructions?: string | null;
    updatedAt: string;
  } | null;
  prescriptions: {
    id: string;
    prescribedAt: string;
    doctorName: string;
    recommendations: string;
    items: { medication: string; instructions: string }[];
  }[];
}

@Component({
  selector: 'app-patient-medical-record',
  standalone: true,
  imports: [RouterLink, CabinetDatePipe, IconComponent],
  templateUrl: './patient-medical-record.component.html',
  styleUrl: './patient-medical-record.component.css'
})
export class PatientMedicalRecordComponent {
  private readonly api = inject(ApiService);
  private readonly destroy = inject(DestroyRef);
  visits: PatientConsultation[] = [];
  loading = false;
  error = '';

  constructor() { this.load(); }

  load() {
    if (this.loading) return;
    this.loading = true;
    this.error = '';
    this.api.get<PatientConsultation[]>('/patients/me/consultations').pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: visits => { this.visits = visits; this.loading = false; },
      error: () => { this.error = 'Votre dossier médical est momentanément indisponible.'; this.loading = false; }
    });
  }
}
