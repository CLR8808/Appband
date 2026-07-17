import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-preparar-dispositivo',
  templateUrl: './preparar-dispositivo.component.html',
  styleUrls: ['./preparar-dispositivo.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class PrepararDispositivoComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/tabs/inicio']);
  }

  next() {
    this.router.navigate(['/bluetooth']);
  }
}
