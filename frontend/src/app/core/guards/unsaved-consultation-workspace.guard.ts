import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { SessionState } from '../services/session-state.service';

export interface ConsultationWorkspaceHost {
  clinicalSaveInProgress: boolean;
  hasUnsavedClinicalChanges(): boolean;
}

export const unsavedConsultationWorkspaceGuard: CanDeactivateFn<ConsultationWorkspaceHost> = component => {
  if (!inject(SessionState).user()) return true;
  if (component.clinicalSaveInProgress) return false;
  return !component.hasUnsavedClinicalChanges() || window.confirm('La fiche clinique ou l’ordonnance contient des modifications non enregistrées. Quitter sans les enregistrer ?');
};
