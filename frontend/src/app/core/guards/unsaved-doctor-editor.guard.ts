import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { SessionState } from '../services/session-state.service';
export interface DoctorEditorHost { editorSaving: boolean; hasUnsavedDoctorChanges(): boolean; }
export const unsavedDoctorEditorGuard:CanDeactivateFn<DoctorEditorHost>=component=>{
 if(!inject(SessionState).user())return true;
 if(component.editorSaving)return false;
 return !component.hasUnsavedDoctorChanges()||window.confirm('Vous avez des modifications non enregistrées. Quitter cette page sans les enregistrer ?');
};
