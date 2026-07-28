import { Injectable, signal } from '@angular/core';

export interface WifiNetwork {
  ssid: string;
  rssi: number; // dBm p.ej. -45, -62, -78
  signalLevel: 'excelente' | 'buena' | 'regular' | 'débil';
  security: 'WPA2' | 'WPA3' | 'WPA2/WPA3' | 'Abierta';
  frequency: '2.4 GHz' | '5 GHz';
  isCurrent?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class Wifi {
  public networks = signal<WifiNetwork[]>([]);
  public isScanning = signal<boolean>(false);
  public selectedNetwork = signal<WifiNetwork | null>(null);
  public error = signal<string | null>(null);

  private scanTimeout: any = null;

  constructor() {}

  /**
   * Inicia el escaneo en tiempo real de redes Wi-Fi del área
   */
  async startScan(durationMs: number = 2000): Promise<void> {
    this.isScanning.set(true);
    this.error.set(null);

    if (this.scanTimeout) clearTimeout(this.scanTimeout);

    this.scanTimeout = setTimeout(() => {
      const baseNetworks: WifiNetwork[] = [
        {
          ssid: 'Casa_Familia_5G',
          rssi: -45,
          signalLevel: 'excelente',
          security: 'WPA3',
          frequency: '5 GHz',
          isCurrent: true,
        },
        {
          ssid: 'Megacable_DDB55_2.5G',
          rssi: -62,
          signalLevel: 'buena',
          security: 'WPA2',
          frequency: '2.4 GHz',
        },
        {
          ssid: 'Megacable_DDB55_5G',
          rssi: -68,
          signalLevel: 'buena',
          security: 'WPA2/WPA3',
          frequency: '5 GHz',
        },
        {
          ssid: 'INFINITUM_E820_5G',
          rssi: -75,
          signalLevel: 'regular',
          security: 'WPA2',
          frequency: '5 GHz',
        },
        {
          ssid: 'Totalplay-99A1',
          rssi: -84,
          signalLevel: 'débil',
          security: 'WPA2',
          frequency: '2.4 GHz',
        },
      ];

      // Pequeñas fluctuaciones de señal para simular escaneo vivo
      const updated = baseNetworks.map((net) => {
        const delta = Math.floor(Math.random() * 7) - 3;
        const newRssi = Math.min(-35, Math.max(-92, net.rssi + delta));
        return {
          ...net,
          rssi: newRssi,
          signalLevel: this.calculateSignalLevel(newRssi),
        };
      });

      this.networks.set(updated);
      this.isScanning.set(false);
    }, durationMs);
  }

  stopScan(): void {
    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
      this.scanTimeout = null;
    }
    this.isScanning.set(false);
  }

  selectNetwork(network: WifiNetwork): void {
    this.selectedNetwork.set(network);
  }

  private calculateSignalLevel(rssi: number): 'excelente' | 'buena' | 'regular' | 'débil' {
    if (rssi >= -52) return 'excelente';
    if (rssi >= -67) return 'buena';
    if (rssi >= -80) return 'regular';
    return 'débil';
  }
}
