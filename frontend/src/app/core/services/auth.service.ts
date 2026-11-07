import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, switchMap, tap, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest, SessionUser } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService); private router = inject(Router);
  readonly user = signal<SessionUser | null>(null);
  private csrf(): Observable<unknown> { return this.api.get<unknown>('/auth/csrf'); }
  login(body: LoginRequest): Observable<SessionUser> { return this.csrf().pipe(switchMap(()=>this.api.post<SessionUser>('/auth/login', body)),tap(user => this.user.set(user))); }
  me(): Observable<SessionUser> { return this.api.get<SessionUser>('/auth/me').pipe(tap(user => this.user.set(user))); }
  logout(): Observable<void> { return this.csrf().pipe(switchMap(()=>this.api.post<void>('/auth/logout')),tap(() => { this.user.set(null); void this.router.navigateByUrl('/'); }),catchError(error=>{this.user.set(null);return throwError(()=>error);})); }
  clear(): void { this.user.set(null); }
}
