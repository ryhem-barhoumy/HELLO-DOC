import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../core/services/domain.services';

interface Specialty { id: string; name: string; }

@Component({
  selector: 'app-admin-specialties',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
<section class="specialty-management" aria-label="Gestion des spécialités médicales">
  <div class="specialty-toolbar">
    <p>Gérez le catalogue de spécialités utilisé dans les inscriptions et la recherche de médecins.</p>
    <button class="button button-primary" type="button" (click)="startCreate()" [disabled]="saving">Ajouter une spécialité <span>+</span></button>
  </div>
  <form class="workspace-form specialty-form" *ngIf="formVisible" [formGroup]="form" (ngSubmit)="save()" novalidate>
    <h2>{{ editingId ? 'Modifier la spécialité' : 'Nouvelle spécialité' }}</h2>
    <p>Le nom est obligatoire et doit être unique, sans distinction de majuscules ou d’accents.</p>
    <label for="specialty-name">Nom de la spécialité</label>
    <input id="specialty-name" type="text" formControlName="name" maxlength="120" autocomplete="off" [attr.aria-invalid]="form.controls.name.touched && form.controls.name.invalid">
    <small class="field-note" *ngIf="form.controls.name.touched && form.controls.name.hasError('required')">Saisissez le nom de la spécialité.</small>
    <small class="field-note" *ngIf="form.controls.name.touched && form.controls.name.hasError('maxlength')">Le nom ne peut pas dépasser 120 caractères.</small>
    <div class="notice notice-error" role="alert" *ngIf="formError">{{ formError }}</div>
    <div class="specialty-form-actions"><button class="button button-primary" type="submit" [disabled]="saving">{{ saving ? 'Enregistrement…' : (editingId ? 'Enregistrer les changements' : 'Enregistrer') }}</button><button class="button button-secondary" type="button" (click)="cancelEdit()" [disabled]="saving">Annuler</button></div>
  </form>
  <div class="notice notice-info" role="status" *ngIf="success">{{ success }}</div>
  <div class="notice notice-error" role="alert" *ngIf="error"><div><b>Opération impossible</b><small>{{ error }}</small></div></div>
  <div class="loading-panel" *ngIf="loading"><span class="spinner"></span> Chargement des spécialités…</div>
  <div class="specialty-list-heading" *ngIf="!loading && specialties.length"><h2>Spécialités enregistrées</h2><span>{{ specialties.length }} au total</span></div>
  <div class="record-grid specialty-grid" *ngIf="!loading && specialties.length">
    <article class="record-card specialty-card" *ngFor="let specialty of specialties; trackBy: trackById"><div class="record-card-head"><span class="record-avatar">✚</span><span class="record-subline">Catalogue médical</span></div><h3>{{ specialty.name }}</h3><div class="record-actions"><button class="small-action" type="button" (click)="startEdit(specialty)" [disabled]="saving">Modifier</button><button class="small-action specialty-delete" type="button" (click)="remove(specialty)" [disabled]="saving">Supprimer</button></div></article>
  </div>
  <div class="empty-panel" *ngIf="!loading && !specialties.length"><div class="empty-symbol">✚</div><h2>Aucune spécialité enregistrée</h2><p>Ajoutez une spécialité pour la rendre disponible dans le catalogue.</p><button class="button button-secondary" type="button" (click)="startCreate()">Ajouter une spécialité</button></div>
</section>
  `,
  styles: [`.specialty-management{display:grid;gap:14px}.specialty-toolbar{display:flex;justify-content:space-between;align-items:center;gap:16px;background:#fff;border:1px solid var(--line);border-radius:14px;padding:14px 18px}.specialty-toolbar p,.specialty-form>p{margin:0;color:#75837e;font-size:12px}.specialty-form{gap:10px}.specialty-form>label{font-size:12px;font-weight:700;color:#53645d}.specialty-form>input{height:42px!important;font-size:13px!important}.specialty-form-actions{display:flex;gap:9px;flex-wrap:wrap}.specialty-form-actions .button{justify-self:start}.specialty-list-heading{display:flex;align-items:center;justify-content:space-between;margin:3px 0}.specialty-list-heading h2{font:700 15px Manrope;color:var(--ink);margin:0}.specialty-list-heading span{font-size:11px;color:#75837e}.specialty-grid{grid-template-columns:repeat(auto-fill,minmax(230px,1fr))}.specialty-card{min-width:0}.specialty-delete{color:#a53c36}.specialty-card .record-actions{margin-top:14px}@media(max-width:680px){.specialty-toolbar{align-items:stretch;flex-direction:column}.specialty-toolbar .button{align-self:flex-start}.specialty-grid{grid-template-columns:1fr}}`]
})
export class AdminSpecialtiesComponent {
  private readonly fb = inject(FormBuilder);
  private readonly admin = inject(AdminService);
  specialties: Specialty[] = []; loading = false; saving = false; formVisible = false; editingId: string | null = null; error = ''; formError = ''; success = '';
  form = this.fb.nonNullable.group({ name: ['', [Validators.required, Validators.maxLength(120)]] });
  constructor() { this.load(); }
  trackById(_index: number, item: Specialty) { return item.id; }
  load() { this.loading = true; this.error = ''; this.admin.specialties().subscribe({ next: rows => { this.specialties = rows; this.loading = false; }, error: e => { this.loading = false; this.error = e.message || 'Impossible de charger les spécialités.'; } }); }
  startCreate() { this.editingId = null; this.form.reset({ name: '' }); this.formError = ''; this.success = ''; this.formVisible = true; }
  startEdit(specialty: Specialty) { this.editingId = specialty.id; this.form.reset({ name: specialty.name }); this.formError = ''; this.success = ''; this.formVisible = true; }
  cancelEdit() { this.formVisible = false; this.editingId = null; this.formError = ''; this.form.reset({ name: '' }); }
  save() { this.form.markAllAsTouched(); this.formError = ''; this.error = ''; this.success = ''; if (this.form.invalid) return; const name = this.form.controls.name.value.trim().replace(/\s+/g, ' '); if (!name) { this.form.controls.name.setErrors({ required: true }); return; } this.saving = true; const request = this.editingId ? this.admin.updateSpecialty(this.editingId, name) : this.admin.createSpecialty(name); request.subscribe({ next: () => { const changed = !!this.editingId; this.saving = false; this.success = changed ? 'Spécialité modifiée.' : 'Spécialité ajoutée.'; this.cancelEdit(); this.load(); }, error: e => { this.saving = false; this.formError = e.message || 'Impossible d’enregistrer cette spécialité.'; } }); }
  remove(specialty: Specialty) { if (!window.confirm(`Supprimer la spécialité « ${specialty.name} » ? Cette action est définitive.`)) return; this.saving = true; this.error = ''; this.success = ''; this.admin.deleteSpecialty(specialty.id).subscribe({ next: () => { this.saving = false; this.success = 'Spécialité supprimée.'; if (this.editingId === specialty.id) this.cancelEdit(); this.load(); }, error: e => { this.saving = false; this.error = e.message || 'Impossible de supprimer cette spécialité.'; } }); }
}
