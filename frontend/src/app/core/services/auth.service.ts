import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, switchMap, tap } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest, SessionUser } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService); private router = inject(Router);
  readonly user = signal<SessionUser | null>(null);
  login(body: LoginRequest): Observable<SessionUser> { return this.api.get<unknown>('/auth/csrf').pipe(switchMap(()=>this.api.post<SessionUser>('/auth/login', body)),tap(user => this.user.set(user))); }
  me(): Observable<SessionUser> { return this.api.get<SessionUser>('/auth/me').pipe(tap(user => this.user.set(user))); }
  logout(): Observable<void> { return this.api.post<void>('/auth/logout').pipe(tap(() => { this.user.set(null); void this.router.navigateByUrl('/'); })); }
  clear(): void { this.user.set(null); }
}
