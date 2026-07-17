import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-dispositivo',
  templateUrl: './dispositivo.component.html',
  styleUrls: ['./dispositivo.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class DispositivoComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/tabs/inicio']);
  }

  irEventos() {
    this.router.navigate(['/tabs/eventos']);
  }

  irConfiguracion() {
    this.router.navigate(['/configuracion']);
  }
}
