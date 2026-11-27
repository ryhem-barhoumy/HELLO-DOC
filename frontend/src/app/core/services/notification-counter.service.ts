import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable()
export class NotificationCounter {
  readonly changed = new Subject<void>();
}
