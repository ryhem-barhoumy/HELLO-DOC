import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { apiErrorInterceptor, csrfInterceptor } from './app/core/interceptors/api-error.interceptor';

registerLocaleData(localeFr);
bootstrapApplication(AppComponent, {
  providers: [{provide:LOCALE_ID,useValue:'fr'},provideRouter(routes), provideHttpClient(withInterceptors([csrfInterceptor, apiErrorInterceptor]))]
}).catch(error => console.error(error));
