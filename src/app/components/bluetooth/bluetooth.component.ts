import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-bluetooth',
  templateUrl: './bluetooth.component.html',
  styleUrls: ['./bluetooth.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class BluetoothComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/preparar-dispositivo']);
  }

  permitir() {
    this.router.navigate(['/buscando']);
  }
}
