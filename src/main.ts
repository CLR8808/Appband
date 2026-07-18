import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { RouteReuseStrategy, provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { IonicRouteStrategy, provideIonicAngular } from '@ionic/angular/standalone';
import { provideFirebaseApp, initializeApp } from '@angular/fire/app';
import { provideAuth, getAuth } from '@angular/fire/auth';
import { provideFirestore, getFirestore } from '@angular/fire/firestore';
import { addIcons } from 'ionicons';
import { 
  homeOutline, home, calendarOutline, calendar, locationOutline, location, 
  personOutline, person, chevronForward, chevronBack, notificationsOutline, 
  add, pencil, settingsOutline, bluetoothOutline, headsetOutline, logOutOutline, 
  documentTextOutline, mapOutline, watchOutline, bluetooth, wifi, batteryHalf, 
  happy, mailOutline, callOutline, time, timeOutline,
  lockClosedOutline, eyeOutline, eyeOffOutline, checkmarkCircleOutline, checkmarkCircle,
  checkmark, logoApple, logoGoogle, logoFacebook
} from 'ionicons/icons';

import { routes } from './app/app.routes';
import { AppComponent } from './app/app.component';
import { environment } from './environments/environment';

addIcons({
  'home-outline': homeOutline,
  'home': home,
  'calendar-outline': calendarOutline,
  'calendar': calendar,
  'location-outline': locationOutline,
  'location': location,
  'person-outline': personOutline,
  'person': person,
  'chevron-forward': chevronForward,
  'chevron-back': chevronBack,
  'notifications-outline': notificationsOutline,
  'add': add,
  'pencil': pencil,
  'settings-outline': settingsOutline,
  'bluetooth-outline': bluetoothOutline,
  'headset-outline': headsetOutline,
  'log-out-outline': logOutOutline,
  'document-text-outline': documentTextOutline,
  'map-outline': mapOutline,
  'watch-outline': watchOutline,
  'bluetooth': bluetooth,
  'wifi': wifi,
  'battery-half': batteryHalf,
  'happy': happy,
  'mail-outline': mailOutline,
  'call-outline': callOutline,
  'time': time,
  'time-outline': timeOutline,
  'lock-closed-outline': lockClosedOutline,
  'eye-outline': eyeOutline,
  'eye-off-outline': eyeOffOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
  'checkmark-circle': checkmarkCircle,
  'checkmark': checkmark,
  'logo-apple': logoApple,
  'logo-google': logoGoogle,
  'logo-facebook': logoFacebook
});

bootstrapApplication(AppComponent, {
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    provideHttpClient(),
    provideIonicAngular(),
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideFirebaseApp(() => initializeApp(environment.firebaseConfig)),
    provideAuth(() => getAuth()),
    provideFirestore(() => getFirestore()),
  ],
});
