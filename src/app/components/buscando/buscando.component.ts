import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { Bluetooth, BluetoothDeviceItem } from '../../services/bluetooth';

@Component({
  selector: 'app-buscando',
  templateUrl: './buscando.component.html',
  styleUrls: ['./buscando.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class BuscandoComponent implements OnInit, OnDestroy {
  public bluetoothService = inject(Bluetooth);

  constructor(private router: Router) {}

  ngOnInit() {
    // Iniciar escaneo real de Bluetooth al entrar a esta pantalla
    this.iniciarEscaneo();
  }

  ngOnDestroy() {
    // Detener escaneo al salir
    this.bluetoothService.stopScan();
  }

  async iniciarEscaneo() {
    await this.bluetoothService.startScan(12000);
  }

  seleccionarDispositivo(device: BluetoothDeviceItem) {
    this.bluetoothService.selectDevice(device);
    this.router.navigate(['/detectado']);
  }

  cancelar() {
    this.bluetoothService.stopScan();
    this.router.navigate(['/bluetooth']);
  }
}

