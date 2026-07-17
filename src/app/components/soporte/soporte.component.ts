import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-soporte',
  templateUrl: './soporte.component.html',
  styleUrls: ['./soporte.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class SoporteComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/tabs/perfil']);
  }

  contactarSoporte() {
    window.location.href = "mailto:soporte@safeband.com?subject=Soporte%20SafeBand";
  }
}
