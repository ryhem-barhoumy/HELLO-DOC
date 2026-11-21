import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
@Component({selector:'app-password-field',standalone:true,
 providers:[{provide:NG_VALUE_ACCESSOR,useExisting:forwardRef(()=>PasswordFieldComponent),multi:true}],
 template:`<span class="editor-password"><input [id]="inputId" [type]="visible?'text':'password'" [value]="value" [disabled]="disabled" [attr.autocomplete]="autocomplete" required aria-required="true" [attr.maxlength]="autocomplete==='new-password'?72:null" [attr.aria-describedby]="describedBy||null" [attr.aria-invalid]="invalid" (input)="input($event)" (blur)="touch()">
 <button type="button" [disabled]="disabled" [attr.aria-label]="(visible?'Masquer ':'Afficher ')+label" [attr.aria-pressed]="visible" (click)="visible=!visible"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path [attr.d]="visible?'M3 3 21 21':null"/></svg></button></span>`})
export class PasswordFieldComponent implements ControlValueAccessor {
 @Input() inputId='';@Input() label='le mot de passe';@Input() autocomplete='current-password';@Input() describedBy='';@Input() invalid=false;
 value='';visible=false;disabled=false;touch:()=>void=()=>{};private change:(value:string)=>void=()=>{};
 writeValue(value:string|null){this.value=value??'';if(!this.value)this.visible=false;}
 registerOnChange(fn:(value:string)=>void){this.change=fn;}
 registerOnTouched(fn:()=>void){this.touch=fn;}
 setDisabledState(disabled:boolean){this.disabled=disabled;}
 input(event:Event){this.value=(event.target as HTMLInputElement).value;this.change(this.value);}
}
