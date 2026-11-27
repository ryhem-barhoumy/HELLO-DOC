import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, switchMap, tap } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest, SessionUser } from '../models/api.models';
import { SessionState } from './session-state.service';

export interface RecoveryTiming {
  expiresInSeconds: number;
  resendAfterSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(ApiService); private router = inject(Router);
  readonly user = inject(SessionState).user;
  private csrf(): Observable<unknown> { return this.api.get<unknown>('/auth/csrf'); }
  login(body: LoginRequest): Observable<SessionUser> { return this.csrf().pipe(switchMap(()=>this.api.post<SessionUser>('/auth/login', body)),tap(user => this.user.set(user))); }
  requestRecovery(email: string): Observable<RecoveryTiming> {
    return this.recoveryPost<RecoveryTiming>('request', { email });
  }
  verifyRecovery(email: string, code: string): Observable<SessionUser> {
    return this.recoveryPost<SessionUser>('verify', { email, code }).pipe(tap(user => this.user.set(user)));
  }
  resetRecoveredPassword(newPassword: string, confirmPassword: string): Observable<void> {
    return this.recoveryPost<void>('password', { newPassword, confirmPassword });
  }
  skipRecoveryPassword(): Observable<void> {
    return this.recoveryPost<void>('skip');
  }
  me(): Observable<SessionUser> { return this.api.get<SessionUser>('/auth/me').pipe(tap(user => this.user.set(user))); }
  logout(): Observable<void> { return this.csrf().pipe(switchMap(()=>this.api.post<void>('/auth/logout')),tap(() => { this.user.set(null); void this.router.navigateByUrl('/'); })); }
  clear(): void { this.user.set(null); }

  private recoveryPost<T>(action: string, body?: object): Observable<T> {
    return this.csrf().pipe(switchMap(() => this.api.post<T>(`/auth/recovery/${action}`, body)));
  }
}
