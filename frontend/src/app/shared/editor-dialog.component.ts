import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild } from '@angular/core';
let sequence=0;
@Component({selector:'app-editor-dialog',standalone:true,template:`
 <dialog #dialog class="editor-dialog" [attr.aria-labelledby]="titleId" (cancel)="cancel($event)">
  <header class="editor-dialog-heading"><div><span class="editor-kicker">HELLODOC · MON CABINET</span><h2 [id]="titleId">{{title}}</h2></div><button type="button" class="editor-icon-button" [disabled]="busy" (click)="dismiss()" aria-label="Fermer la fenêtre">×</button></header>
  <ng-content></ng-content>
 </dialog>`})
export class EditorDialogComponent implements AfterViewInit,OnDestroy {
 @Input() title='';@Input() busy=false;@Output() dismissed=new EventEmitter<void>();
 @ViewChild('dialog',{static:true}) dialog!:ElementRef<HTMLDialogElement>;
 readonly titleId=`editor-dialog-${++sequence}`;private opener:HTMLElement|null=null;
 ngAfterViewInit(){this.opener=document.activeElement as HTMLElement;this.dialog.nativeElement.showModal();}
 cancel(event:Event){event.preventDefault();if(!this.busy)this.dismiss();}
 dismiss(){if(!this.busy)this.dismissed.emit();}
 ngOnDestroy(){if(this.dialog.nativeElement.open)this.dialog.nativeElement.close();if(this.opener?.isConnected)this.opener.focus({preventScroll:true});}
}
