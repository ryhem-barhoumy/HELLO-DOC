import { Injectable, signal } from '@angular/core';
import { SessionUser } from '../models/api.models';
@Injectable({ providedIn:'root' })
export class SessionState { readonly user=signal<SessionUser|null>(null); }
