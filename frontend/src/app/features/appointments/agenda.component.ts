import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EMPTY, expand, reduce, Subscription } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { Appointment } from '../../core/models/api.models';

interface AgendaPage { content: Appointment[]; number: number; totalPages: number; last: boolean; }
@Component({
  selector: 'app-agenda', standalone: true, imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './agenda.component.html'
})
export class AgendaComponent implements OnInit {
  private api = inject(ApiService);
  private subscription?: Subscription;
  private destroy = inject(DestroyRef);
  cursor = this.localDate(new Intl.DateTimeFormat('en-CA', { timeZone: 'Africa/Tunis' }).format(new Date()));
  view: 'day' | 'week' | 'month' = 'week';
  query = ''; status = ''; loading = false; error = ''; appointments: Appointment[] = [];
  statuses = [{value:'PENDING',label:'En attente'}, {value:'CONFIRMED',label:'Confirmé'},
    {value:'TO_RESCHEDULE',label:'À reprogrammer'}, {value:'COMPLETED',label:'Terminé'},
    {value:'CANCELLED',label:'Annulé'}, {value:'REJECTED',label:'Refusé'}, {value:'NO_SHOW',label:'Patient absent'}];
  weekdays = ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'];
  constructor() { this.destroy.onDestroy(() => this.subscription?.unsubscribe()); }
  ngOnInit(): void { this.load(); }
  private localDate(key: string): Date { return new Date(`${key}T12:00:00`); }
  key(date: Date): string { return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`; }
  private plus(date: Date, days: number): Date { const value = new Date(date); value.setDate(value.getDate()+days); return value; }
  get start(): Date {
    if (this.view === 'month') return new Date(this.cursor.getFullYear(), this.cursor.getMonth(), 1, 12);
    return this.view === 'week' ? this.plus(this.cursor, -((this.cursor.getDay()+6)%7)) : this.cursor;
  }
  get end(): Date { return this.view === 'month' ? new Date(this.cursor.getFullYear(), this.cursor.getMonth()+1, 1, 12) : this.plus(this.start, this.view === 'week' ? 7 : 1); }
  get days(): Date[] {
    const result: Date[] = []; for (let day = this.start; day < this.end; day = this.plus(day,1)) result.push(day); return result;
  }
  get blanks(): number[] { return Array.from({length:(this.start.getDay()+6)%7}, (_,i)=>i); }
  get label(): string {
    const format = (date: Date) => new Intl.DateTimeFormat('fr-FR',{day:'numeric',month:'long'}).format(date);
    if (this.view === 'month') return new Intl.DateTimeFormat('fr-FR',{month:'long',year:'numeric'}).format(this.cursor);
    return this.view === 'week' ? `${format(this.start)} – ${format(this.plus(this.end,-1))} ${this.end.getFullYear()}` : `${format(this.cursor)} ${this.cursor.getFullYear()}`;
  }
  get filtered(): Appointment[] {
    const q = this.query.trim().toLocaleLowerCase('fr');
    return this.appointments.filter(a => (!this.status || a.status === this.status) &&
      (!q || `${a.patientName ?? ''} ${a.serviceName}`.toLocaleLowerCase('fr').includes(q)));
  }
  events(date: Date): Appointment[] {
    const key = this.key(date);
    return this.filtered.filter(a => new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis'}).format(new Date(a.startsAt)) === key);
  }
  isToday(date: Date): boolean { return this.key(date) === new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis'}).format(new Date()); }
  statusLabel(value: string): string { return this.statuses.find(s=>s.value===value)?.label ?? value; }
  selectView(view: 'day' | 'week' | 'month'): void { this.view = view; this.load(); }
  move(offset: number): void {
    this.cursor = this.view === 'month' ? new Date(this.cursor.getFullYear(),this.cursor.getMonth()+offset,1,12) : this.plus(this.cursor,offset*(this.view==='week'?7:1)); this.load();
  }
  today(): void { this.cursor = this.localDate(new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Tunis'}).format(new Date())); this.load(); }
  chooseDay(key: string): void { if (key) { this.cursor = this.localDate(key); this.load(); } }
  load(): void {
    this.subscription?.unsubscribe(); this.loading = true; this.error = ''; this.appointments = [];
    const query = {from:`${this.key(this.start)}T00:00:00+01:00`,to:`${this.key(this.end)}T00:00:00+01:00`,size:100};
    const page = (number: number) => this.api.get<AgendaPage>('/doctors/me/appointments', {...query,page:number});
    this.subscription = page(0).pipe(
      expand(result => result.last || result.number+1 >= result.totalPages ? EMPTY : page(result.number+1)),
      reduce((all, result) => [...all,...result.content], [] as Appointment[])
    ).subscribe({next: appointments => { this.appointments = appointments; this.loading = false; },
      error: e => { this.error = e.message; this.loading = false; }});
  }
}
