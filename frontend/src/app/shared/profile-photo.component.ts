import { Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../core/services/api.service';
import { AuthService } from '../core/services/auth.service';
import { AvatarComponent } from './avatar.component';
@Component({selector:'app-profile-photo',standalone:true,imports:[CommonModule,AvatarComponent],template:`
<section class="profile-photo-card" aria-labelledby="photo-title">
 <app-avatar [url]="preview||url" [name]="name" [size]="104"/>
 <div class="photo-copy"><span class="eyebrow">VOTRE IDENTITÉ</span><h2 id="photo-title">Photo de profil</h2><p>{{doctor?'Une photo permet aux patients de vous reconnaître dans la recherche et sur votre fiche.':'Personnalisez votre espace avec une photo. Elle reste associée à votre compte.'}}</p><small>JPEG ou PNG · 5 Mo maximum</small>
 <div class="record-actions"><label class="button button-secondary photo-file-picker" [class.is-disabled]="busy"><span>{{preview?'Choisir une autre photo':url?'Changer la photo':'Ajouter une photo'}}</span><input type="file" accept="image/jpeg,image/png" (change)="choose($event)" [disabled]="busy" aria-label="Choisir une photo de profil"></label><button *ngIf="preview" type="button" class="button button-primary" [disabled]="busy" (click)="save()">{{busy?'Enregistrement…':'Enregistrer la photo'}}</button><button *ngIf="preview" type="button" class="text-button" [disabled]="busy" (click)="discard()">Annuler</button><button *ngIf="url&&!preview" type="button" class="text-button" [disabled]="busy" (click)="remove()">Retirer la photo</button></div>
 <p *ngIf="error" role="alert" class="field-error">{{error}}</p><p *ngIf="message" role="status" class="photo-success">{{message}}</p><p *ngIf="busy&&!photo" role="status" class="field-note">Préparation de votre photo…</p></div>
</section>`})
export class ProfilePhotoComponent {
 @Input() url:string|null=null;@Input() name='';@Input() doctor=false;@Output() photoChanged=new EventEmitter<string|null>();
 private api=inject(ApiService);private auth=inject(AuthService);private destroy=inject(DestroyRef);private alive=true;
 preview='';photo:Blob|null=null;busy=false;error='';message='';
 constructor(){this.destroy.onDestroy(()=>{this.alive=false;this.discard();});}
 discard(){if(this.preview)URL.revokeObjectURL(this.preview);this.preview='';this.photo=null;}
 async choose(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];input.value='';if(!file||this.busy)return;this.error='';this.message='';if(!['image/jpeg','image/png'].includes(file.type)){this.error='Choisissez une photo JPEG ou PNG.';return;}if(file.size>5*1024*1024){this.error='La photo ne doit pas dépasser 5 Mo.';return;}this.busy=true;
  try{const image=await createImageBitmap(file);try{if(image.width*image.height>20_000_000)throw new Error('Image trop grande');const scale=Math.min(1,640/Math.max(image.width,image.height));const canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(image.width*scale));canvas.height=Math.max(1,Math.round(image.height*scale));const context=canvas.getContext('2d');if(!context)throw new Error('Image indisponible');context.fillStyle='#ffffff';context.fillRect(0,0,canvas.width,canvas.height);context.drawImage(image,0,0,canvas.width,canvas.height);const blob=await new Promise<Blob>((resolve,reject)=>canvas.toBlob(value=>value?resolve(value):reject(new Error('Image indisponible')),'image/jpeg',.9));if(this.alive){this.discard();this.photo=blob;this.preview=URL.createObjectURL(blob);}}finally{image.close();}}
  catch{if(this.alive)this.error='Cette photo ne peut pas être lue. Choisissez un fichier JPEG ou PNG de 20 mégapixels maximum.';}finally{this.busy=false;}
 }
 save(){if(this.busy||!this.photo)return;const body=new FormData();body.append('photo',this.photo,'profile.jpg');this.busy=true;this.error='';this.api.post<{photoUrl:string}>('/account/me/photo',body).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:result=>{this.busy=false;this.url=result.photoUrl;this.discard();this.updated(result.photoUrl);this.message='Votre photo a été enregistrée.';},error:e=>{this.busy=false;this.error=e.message;}});}
 remove(){if(this.busy||!this.url)return;this.busy=true;this.error='';this.api.delete('/account/me/photo').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{this.busy=false;this.url=null;this.updated(null);this.message='Votre photo a été retirée.';},error:e=>{this.busy=false;this.error=e.message;}});}
 private updated(url:string|null){this.photoChanged.emit(url);this.auth.user.update(user=>user?{...user,photoUrl:url}:null);}
}
