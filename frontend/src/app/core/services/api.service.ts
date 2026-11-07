import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiBaseUrl;
  get<T>(path: string, query?: Record<string, string | number | undefined>): Observable<T> {
    let params = new HttpParams();
    Object.entries(query ?? {}).forEach(([key, value]) => { if (value !== undefined && value !== '') params = params.set(key, String(value)); });
    return this.http.get<T>(`${this.base}${path}`, { params, withCredentials: true });
  }
  post<T>(path: string, body?: unknown): Observable<T> { return this.http.post<T>(`${this.base}${path}`, body ?? {}, { withCredentials: true }); }
  put<T>(path: string, body: unknown): Observable<T> { return this.http.put<T>(`${this.base}${path}`, body, { withCredentials: true }); }
  patch<T>(path: string, body?: unknown): Observable<T> { return this.http.patch<T>(`${this.base}${path}`, body ?? {}, { withCredentials: true }); }
  delete<T>(path: string): Observable<T> { return this.http.delete<T>(`${this.base}${path}`, { withCredentials: true }); }
}
