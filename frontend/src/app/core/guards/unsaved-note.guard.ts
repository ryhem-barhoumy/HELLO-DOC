import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { SessionState } from '../services/session-state.service';
export const unsavedNoteGuard:CanDeactivateFn<{busy:boolean;form:{dirty:boolean}}>=component=>!inject(SessionState).user()||(!component.busy&&(!component.form.dirty||window.confirm('Cette fiche contient des modifications non enregistrées. Quitter sans les enregistrer ?')));
