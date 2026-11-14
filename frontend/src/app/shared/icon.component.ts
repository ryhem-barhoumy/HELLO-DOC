import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-icon', standalone: true,
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path [attr.d]="path"/></svg>',
  styles: [':host{display:inline-flex;width:20px;height:20px;flex:none}svg{width:100%;height:100%}']
})
export class IconComponent {
  @Input() name = '';
  private paths: Record<string,string> = {
    '': 'M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z',
    medecins: 'M12 3v18M3 12h18M7 3h10v4h4v10h-4v4H7v-4H3V7h4Z',
    patients: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 4a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
    'rendez-vous': 'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2ZM8 15h3M8 18h6',
    calendrier: 'M8 2v4M16 2v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2ZM7 14h1M12 14h1M17 14h1M7 18h1M12 18h1',
    disponibilites: 'M12 8v4l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
    absences: 'M5 5l14 14M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
    notifications: 'M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4',
    profil: 'M20 21a8 8 0 0 0-16 0M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
    parametres: 'M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M5.6 18.4 7 17M17 7l1.4-1.4M18 12a6 6 0 1 1-12 0 6 6 0 0 1 12 0ZM14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0Z',
    avis: 'm12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z',
    approbations: 'm5 12 4 4L19 6',
    historique: 'M3 5v5h5M3 10a9 9 0 1 1 1 8M12 7v5l3 2',
    activite: 'M3 12h4l3-8 4 16 3-8h4'
  };
  get path(): string { return this.paths[this.name] ?? this.paths['']; }
}
