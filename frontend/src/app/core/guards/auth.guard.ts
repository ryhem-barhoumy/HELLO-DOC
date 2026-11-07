import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/api.models';

export const roleGuard: CanActivateFn = route => {
  const auth = inject(AuthService); const router = inject(Router);
  const allowed = route.data['roles'] as Role[];
  const user = auth.user();
  if (user) return allowed.includes(user.role) && (user.role !== 'DOCTOR' || user.status === 'APPROVED') ? true : router.parseUrl('/access-denied');
  return auth.me().pipe(map(session => allowed.includes(session.role) && (session.role !== 'DOCTOR' || session.status === 'APPROVED') ? true : router.parseUrl('/access-denied')),
    catchError(() => of(router.parseUrl('/connexion'))));
};
