import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';

@Component({
  selector: 'app-wifi',
  templateUrl: './wifi.component.html',
  styleUrls: ['./wifi.component.scss'],
  standalone: true,
  imports: [IonicModule]
})
export class WifiComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/detectado']);
  }

  conectar(ssid: string) {
    this.router.navigate(['/password'], { queryParams: { ssid } });
  }
}
