import { Component } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-root',
  standalone: true,
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  appUrl: SafeResourceUrl;

  constructor(private sanitizer: DomSanitizer) {
    this.appUrl = this.sanitizer.bypassSecurityTrustResourceUrl('/assets/original/interface.html');
  }
}
