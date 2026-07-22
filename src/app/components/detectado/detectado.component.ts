import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { Bluetooth } from '../../services/bluetooth';

@Component({
  selector: 'app-detectado',
  templateUrl: './detectado.component.html',
  styleUrls: ['./detectado.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class DetectadoComponent {
  public bluetoothService = inject(Bluetooth);

  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/buscando']);
  }

  conectar() {
    this.router.navigate(['/wifi']);
  }
}

