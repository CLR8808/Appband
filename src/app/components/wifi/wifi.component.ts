import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { Wifi, WifiNetwork } from '../../services/wifi';

@Component({
  selector: 'app-wifi',
  templateUrl: './wifi.component.html',
  styleUrls: ['./wifi.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule],
})
export class WifiComponent implements OnInit, OnDestroy {
  public wifiService = inject(Wifi);

  constructor(private router: Router) {}

  ngOnInit() {
    this.iniciarEscaneo();
  }

  ngOnDestroy() {
    this.wifiService.stopScan();
  }

  async iniciarEscaneo() {
    await this.wifiService.startScan(2000);
  }

  goBack() {
    this.router.navigate(['/detectado']);
  }

  conectar(network: WifiNetwork) {
    this.wifiService.selectNetwork(network);
    this.router.navigate(['/password'], { queryParams: { ssid: network.ssid } });
  }

  conectarNombreManual() {
    const ssid = prompt('Ingresa el nombre (SSID) de la red Wi-Fi:');
    if (ssid && ssid.trim()) {
      const manualNet: WifiNetwork = {
        ssid: ssid.trim(),
        rssi: -60,
        signalLevel: 'buena',
        security: 'WPA2',
        frequency: '2.4 GHz'
      };
      this.wifiService.selectNetwork(manualNet);
      this.router.navigate(['/password'], { queryParams: { ssid: manualNet.ssid } });
    }
  }

  getSignalIcon(network: WifiNetwork): string {
    switch (network.signalLevel) {
      case 'excelente':
      case 'buena':
        return 'wifi';
      case 'regular':
      case 'débil':
        return 'wifi-outline';
      default:
        return 'wifi';
    }
  }
}
