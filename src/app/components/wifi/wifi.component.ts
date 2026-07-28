import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DispositivoWifi } from '../../services/dispositivo-wifi';

@Component({
  selector: 'app-wifi',
  templateUrl: './wifi.component.html',
  styleUrls: ['./wifi.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class WifiComponent {
  public wifiService = inject(DispositivoWifi);
  ipAddress = '192.168.4.1';

  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/preparar-dispositivo']);
  }

  async conectar() {
    const ok = await this.wifiService.conectar(this.ipAddress);
    if (ok) {
      this.router.navigate(['/nombre']);
    }
  }
}
