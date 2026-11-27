import { Component, DestroyRef, ElementRef, HostListener, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { EMPTY, catchError, filter, merge, switchMap, timer } from 'rxjs';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { NotificationCounter } from '../core/services/notification-counter.service';
import { AvatarComponent } from './avatar.component';
import { IconComponent } from './icon.component';
import { TeleconsultationReminderComponent } from './teleconsultation-reminder.component';
import { WorkspaceFooterComponent } from './workspace-footer.component';

const patientLinks = [
  { path: '', label: 'Accueil' }, { path: 'medecins', label: 'Trouver un médecin' },
  { path: 'rendez-vous', label: 'Mes rendez-vous' }, { path: 'dossier-medical', label: 'Mon dossier médical' },
  { path: 'historique', label: 'Suivi et historique' }
];
const doctorLinks = [
  { path: '', label: 'Tableau de bord' }, { path: 'rendez-vous', label: 'Rendez-vous' },
  { path: 'disponibilites', label: 'Disponibilités' }, { path: 'patients', label: 'Mes patients' }
];

@Component({
  selector: 'app-care-workspace',
  imports: [RouterLink, RouterOutlet, AvatarComponent, IconComponent, TeleconsultationReminderComponent, WorkspaceFooterComponent],
  providers: [NotificationCounter],
  templateUrl: './care-workspace.component.html',
  styleUrl: './care-workspace.component.css'
})
export class CareWorkspaceComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private api = inject(ApiService);
  private destroy = inject(DestroyRef);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly auth = inject(AuthService);
  readonly doctor = this.route.snapshot.data['roles'][0] === 'DOCTOR';
  readonly base = this.doctor ? '/medecin' : '/patient';
  readonly links = this.doctor ? doctorLinks : patientLinks;
  readonly unread = signal<number | null>(null);
  readonly practiceLinks = [
    { path: 'calendrier', label: 'Agenda' }, { path: 'absences', label: 'Absences' }, { path: 'avis', label: 'Avis patients' }
  ];
  section = '';
  active = '';
  detail = '';
  menu: 'navigation' | 'profile' | null = null;
  busy = false;
  error = '';

  @ViewChild('navigationButton') navigationButton?: ElementRef<HTMLButtonElement>;
  @ViewChild('profileButton') profileButton?: ElementRef<HTMLButtonElement>;
  @ViewChild(RouterOutlet) outlet?: RouterOutlet;

  get name() {
    const user = this.auth.user();
    return user ? `${user.firstName} ${user.lastName}` : 'Mon compte';
  }
  get label() {
    return [...this.links, ...this.practiceLinks].find(link => link.path === this.section)?.label
      ?? (this.section === 'notifications' ? 'Notifications' : 'Mon compte');
  }
  get showPracticeLinks() {
    return this.doctor && ['', 'rendez-vous', 'disponibilites', 'calendrier', 'absences', 'avis'].includes(this.section);
  }

  constructor() {
    const navigation = this.router.events.pipe(filter(event => event instanceof NavigationEnd));
    navigation.pipe(takeUntilDestroyed(this.destroy)).subscribe(() => {
      const child = this.route.firstChild?.snapshot;
      const section = child?.data['patientNav'] ?? child?.paramMap.get('area') ?? child?.url[0]?.path ?? '';
      this.section = section === 'dashboard' ? '' : section;
      if (this.section === 'rendez-vous' && child?.queryParamMap.get('origine') === 'historique') this.section = 'historique';
      this.active = this.section === 'calendrier' ? 'rendez-vous' : this.section === 'absences' ? 'disponibilites'
        : this.doctor && this.section === 'avis' ? '' : this.section;
      this.detail = (child?.url.length ?? 0) > 1 ? (child?.title ?? '').replace(' | Hello Doc', '') : '';
      this.menu = null;
    });
    merge(timer(0, 60_000), navigation, inject(NotificationCounter).changed).pipe(
      switchMap(() => this.api.get<{ count: number }>('/notifications/unread-count').pipe(catchError(() => EMPTY))),
      takeUntilDestroyed(this.destroy)
    ).subscribe(result => this.unread.set(result.count));
  }

  toggle(kind: 'navigation' | 'profile') {
    this.menu = this.menu === kind ? null : kind;
    if (this.menu === 'navigation') {
      requestAnimationFrame(() => {
        if (this.menu !== 'navigation') return;
        const navigation = this.host.nativeElement.querySelector('#care-navigation');
        (navigation?.querySelector<HTMLElement>('[aria-current="page"]') ?? navigation?.querySelector<HTMLElement>('a'))?.focus();
      });
    }
  }

  @HostListener('document:keydown.escape')
  closeMenu() {
    if (!this.menu) return;
    const trigger = this.menu === 'navigation' ? this.navigationButton : this.profileButton;
    this.menu = null;
    trigger?.nativeElement.focus();
  }

  @HostListener('document:click', ['$event'])
  dismissOutside(event: MouseEvent) {
    if (this.menu && event.target instanceof Element && !this.host.nativeElement.querySelector('.care-nav')?.contains(event.target)) this.menu = null;
  }

  logout() {
    if (this.busy) return;
    const page = this.outlet?.isActivated ? this.outlet.component as {
      editorSaving?: boolean; clinicalSaveInProgress?: boolean; busy?: boolean;
      hasUnsavedDoctorChanges?(): boolean; hasUnsavedClinicalChanges?(): boolean; hasUnsavedChanges?(): boolean;
    } : null;
    if (page?.editorSaving || page?.clinicalSaveInProgress || page?.busy) return;
    const dirty = page?.hasUnsavedDoctorChanges?.() || page?.hasUnsavedClinicalChanges?.() || page?.hasUnsavedChanges?.();
    if (dirty && !window.confirm('Vous avez des modifications non enregistrées. Se déconnecter quand même ?')) return;
    this.busy = true;
    this.error = '';
    this.auth.logout().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      error: () => { this.busy = false; this.menu = null; this.error = 'La déconnexion a échoué. Réessayez.'; }
    });
  }
}
