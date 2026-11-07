import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/api.models';

export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService); const router = inject(Router);
  const allowed = route.data['roles'] as Role[];
  const user = auth.user();
  const permits = (role: Role, status: string) => (allowed.includes(role) || (role === 'USER' && allowed.includes('PATIENT'))) && (role !== 'DOCTOR' || status === 'APPROVED');
  if (user) return permits(user.role, user.status) ? true : router.parseUrl('/access-denied');
  return auth.me().pipe(map(session => permits(session.role, session.status) ? true : router.parseUrl('/access-denied')),
    catchError(() => of(router.parseUrl('/connexion'))));
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService); const router = inject(Router);
  const user = auth.user();
  if (user) return user.role === 'ADMIN' ? true : router.parseUrl('/access-denied');
  return auth.me().pipe(map(session => session.role === 'ADMIN' ? true : router.parseUrl('/access-denied')),
    catchError(() => of(router.parseUrl('/connexion'))));
};
