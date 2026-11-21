import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../environments/environment';

export function photoSource(path:string|null|undefined):string|null {
 if(!path||!path.startsWith('/api/'))return null;
 return environment.apiBaseUrl.startsWith('http')?environment.apiBaseUrl.replace(/\/api\/?$/,'')+path:path;
}
@Component({selector:'app-avatar',standalone:true,imports:[CommonModule],template:`
<span class="person-avatar" [style.width.px]="size" [style.height.px]="size" [style.font-size.px]="size*.32">
 <img *ngIf="source&&!failed;else initials" [src]="source" [alt]="'Photo de '+name" (error)="failed=true" loading="lazy" decoding="async">
 <ng-template #initials><span role="img" [attr.aria-label]="name||'Profil'">{{letters}}</span></ng-template>
</span>`})
export class AvatarComponent implements OnChanges {
 @Input() url:string|null|undefined=null;@Input() name='';@Input() size=44;failed=false;
 get source(){return this.url?.startsWith('blob:')?this.url:photoSource(this.url);}
 get letters(){const names=this.name.trim().split(/\s+/).filter(Boolean);return names.length?(names[0][0]+(names.length>1?names[names.length-1][0]:'')).toUpperCase():'HD';}
 ngOnChanges(){this.failed=false;}
}
