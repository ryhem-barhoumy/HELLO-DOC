import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  const unsafe = !['GET','HEAD','OPTIONS'].includes(request.method.toUpperCase());
  if (!unsafe || !request.url.startsWith(environment.apiBaseUrl) || typeof document === 'undefined') return next(request);
  const token = document.cookie.split(';').map(item=>item.trim()).find(item=>item.startsWith('XSRF-TOKEN='))?.slice('XSRF-TOKEN='.length);
  return next(token ? request.clone({setHeaders:{'X-XSRF-TOKEN':decodeURIComponent(token)}}) : request);
};

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => next(request).pipe(
  catchError((error: HttpErrorResponse) => {
    const message = error.status === 0 ? 'Le service Hello Doc est momentanément indisponible. Réessayez plus tard.'
      : error.status === 401 ? (request.url.endsWith('/auth/login') ? 'L’adresse e-mail ou le mot de passe est incorrect.' : error.error?.message ?? 'Votre session a expiré. Connectez-vous à nouveau.')
      : error.error?.message ?? 'Une erreur est survenue. Vérifiez les informations puis réessayez.';
    return throwError(() => ({ status: error.status, message, fieldErrors: error.error?.fieldErrors }));
  })
);
