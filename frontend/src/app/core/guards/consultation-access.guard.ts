import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { ApiService } from '../services/api.service';

export const consultationAccessGuard: CanActivateFn = route => {
  const api = inject(ApiService);
  const router = inject(Router);
  const role = (route.data['roles'] as string[] | undefined)?.[0];
  const appointmentId = route.paramMap.get('appointmentId');

  if (!appointmentId || (role !== 'DOCTOR' && role !== 'PATIENT')) {
    return router.parseUrl('/access-denied');
  }

  return api.get<any>(`/appointments/${appointmentId}`).pipe(
    map(appointment => {
      const eligibleStatus = appointment.status === 'CONFIRMED' || appointment.status === 'COMPLETED';
      const eligibleMode = role === 'DOCTOR'
        ? ['VIDEO', 'IN_PERSON'].includes(appointment.consultationMode)
        : appointment.consultationMode === 'VIDEO';
      if (eligibleStatus && eligibleMode) return true;
      return router.createUrlTree([role === 'DOCTOR' ? '/medecin' : '/patient', 'rendez-vous', appointmentId]);
    }),
    catchError(() => of(router.parseUrl('/access-denied')))
  );
};
