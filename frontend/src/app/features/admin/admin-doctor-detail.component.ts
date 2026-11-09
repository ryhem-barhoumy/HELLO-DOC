import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { environment } from '../../../environments/environment';

interface AdminDoctor {
  id: string;
  applicationId: string;
  fullName: string;
  serviceTitle: string;
  email: string;
  phone: string;
  address: string;
  specialty: string;
  licenseNumber: string;
  city: string;
  location: string;
  description: string;
  status: string;
  hasDocument: boolean;
}

@Component({
  selector: 'app-admin-doctor-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <main class="admin-doctor-detail">
      <a class="back-link" routerLink="/admin/medecins">← Retour aux médecins</a>
      <p class="eyebrow">ESPACE ADMINISTRATION · MÉDECINS</p>
      <div *ngIf="loading" class="state-card">Chargement du profil médecin…</div>
      <div *ngIf="error" class="state-card error" role="alert">
        <span>{{ error }}</span>
        <button type="button" (click)="load()">Réessayer</button>
      </div>
      <ng-container *ngIf="doctor as d">
        <header class="detail-heading">
          <div><h1>{{ d.fullName }}</h1><p>{{ d.serviceTitle || d.specialty || 'Profil médecin' }}</p></div>
          <span class="status" [class.inactive]="d.status !== 'ACTIVE'">{{ d.status === 'ACTIVE' ? 'Actif' : 'Inactif' }}</span>
        </header>
        <section class="detail-card">
          <h2>Coordonnées</h2>
          <dl>
            <div><dt>Courriel</dt><dd>{{ d.email || '—' }}</dd></div>
            <div><dt>Téléphone</dt><dd>{{ d.phone || '—' }}</dd></div>
            <div><dt>Adresse</dt><dd>{{ d.address || '—' }}</dd></div>
            <div><dt>Spécialité</dt><dd>{{ d.specialty || '—' }}</dd></div>
            <div><dt>Ville</dt><dd>{{ d.city || '—' }}</dd></div>
            <div><dt>Adresse du cabinet</dt><dd>{{ d.location || '—' }}</dd></div>
            <div><dt>Numéro professionnel</dt><dd>{{ d.licenseNumber || '—' }}</dd></div>
          </dl>
          <div *ngIf="d.description" class="description"><h3>Présentation</h3><p>{{ d.description }}</p></div>
        </section>
        <section class="detail-card document-card">
          <div><h2>Justificatif d’exercice</h2><p>Attestation d’inscription à l’Ordre déposée lors de la candidature.</p></div>
          <a *ngIf="d.hasDocument" class="document-button" [href]="documentUrl(d)" target="_blank" rel="noopener">Ouvrir l’attestation</a>
          <span *ngIf="!d.hasDocument" class="missing-document">Aucun justificatif enregistré pour cette candidature.</span>
        </section>
      </ng-container>
    </main>
  `,
  styles: [`
    :host{display:block;min-height:100vh;background:#f5f8f7;color:#173b36;font-family:inherit}
    .admin-doctor-detail{max-width:1000px;margin:0 auto;padding:36px 28px 64px}.back-link{color:#087f6b;text-decoration:none;font-weight:600}
    .eyebrow{margin:32px 0 8px;color:#087f6b;font-size:12px;font-weight:700;letter-spacing:.12em}.detail-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:22px}
    h1{margin:0;font-size:30px}.detail-heading p,.document-card p{margin:7px 0 0;color:#6d817d}.status{border-radius:20px;padding:7px 13px;background:#e3f5ee;color:#087f6b;font-weight:700}.status.inactive{background:#fff0ed;color:#b64739}
    .detail-card,.state-card{background:#fff;border:1px solid #e1eae7;border-radius:14px;padding:24px;margin:16px 0;box-shadow:0 6px 20px #153d3510}.detail-card h2{margin:0 0 18px;font-size:18px}
    dl{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px;margin:0}dl div{min-width:0}dt{color:#748681;font-size:13px;margin-bottom:5px}dd{margin:0;font-weight:600;overflow-wrap:anywhere}.description{border-top:1px solid #edf1ef;margin-top:22px;padding-top:16px}.description h3{font-size:15px}.description p{white-space:pre-line}
    .document-card{display:flex;align-items:center;justify-content:space-between;gap:20px}.document-button,.state-card button{display:inline-block;border:0;border-radius:9px;background:#087f6b;color:#fff;padding:12px 17px;text-decoration:none;font-weight:700;white-space:nowrap;cursor:pointer}.missing-document,.error{color:#a33b32}.state-card{display:flex;gap:16px;align-items:center;justify-content:space-between}
    @media(max-width:650px){.admin-doctor-detail{padding:24px 16px}.detail-heading,.document-card{align-items:flex-start;flex-direction:column}dl{grid-template-columns:1fr}h1{font-size:25px}}
  `]
})
export class AdminDoctorDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ApiService);
  doctor: AdminDoctor | null = null;
  loading = false;
  error = '';

  ngOnInit(): void { this.load(); }

  load(): void {
    const id = this.route.snapshot.paramMap.get('doctorId');
    if (!id) { this.error = 'Identifiant médecin manquant.'; return; }
    this.loading = true;
    this.error = '';
    this.api.get<AdminDoctor>(`/admin/doctors/directory/${id}`).subscribe({
      next: doctor => { this.doctor = doctor; this.loading = false; },
      error: e => { this.error = e.message || 'Impossible de charger le profil médecin.'; this.loading = false; }
    });
  }

  documentUrl(doctor: AdminDoctor): string {
    return `${environment.apiBaseUrl}/admin/doctors/${doctor.applicationId}/document`;
  }
}
