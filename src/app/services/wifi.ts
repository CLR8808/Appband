import { Injectable, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { CapacitorWifi } from '@capgo/capacitor-wifi';

export interface WifiNetwork {
  ssid: string;
  rssi: number;
  signalLevel: 'excelente' | 'buena' | 'regular' | 'débil';
  security: 'WPA2' | 'WPA3' | 'WPA2/WPA3' | 'Abierta' | 'Desconocida';
  frequency: '2.4 GHz' | '5 GHz' | 'Desconocida';
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

  async startScan(durationMs: number = 4000): Promise<void> {
    this.isScanning.set(true);
    this.error.set(null);
    this.networks.set([]);

    if (this.scanTimeout) clearTimeout(this.scanTimeout);

    try {
      if (Capacitor.isNativePlatform()) {
        const result = await CapacitorWifi.getAvailableNetworks();
        const list = Array.isArray(result?.networks) ? result.networks : [];
        const mapped = list
          .map((item: any) => this.mapNetwork(item))
          .filter((item: WifiNetwork | null): item is WifiNetwork => !!item)
          .sort((a: WifiNetwork, b: WifiNetwork) => b.rssi - a.rssi);

        this.networks.set(mapped);
      } else {
        this.error.set('El escaneo de redes Wi‑Fi requiere ejecutarse en dispositivo o emulador Android.');
        this.networks.set([]);
      }
    } catch (err: any) {
      console.error('[Wifi] Scan error:', err);
      this.error.set(err?.message || 'No se pudo obtener la lista de redes Wi‑Fi.');
      this.networks.set([]);
    } finally {
      this.isScanning.set(false);
    }

    if (this.scanTimeout) clearTimeout(this.scanTimeout);
    this.scanTimeout = setTimeout(() => {
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

  private mapNetwork(item: any): WifiNetwork | null {
    if (!item?.ssid) return null;

    const rssi = typeof item?.rssi === 'number' ? item.rssi : -90;
    const signalLevel = this.calculateSignalLevel(rssi);
    const security = this.normalizeSecurity(item?.security);

    return {
      ssid: item.ssid,
      rssi,
      signalLevel,
      security,
      frequency: this.normalizeFrequency(item?.frequency),
      isCurrent: !!item?.isCurrent,
    };
  }

  private normalizeSecurity(security: any): WifiNetwork['security'] {
    if (typeof security === 'string') {
      const value = security.toLowerCase();
      if (value.includes('wpa3')) return 'WPA3';
      if (value.includes('wpa2')) return 'WPA2';
      if (value.includes('open')) return 'Abierta';
    }
    return 'Desconocida';
  }

  private normalizeFrequency(frequency: any): WifiNetwork['frequency'] {
    if (typeof frequency === 'number') {
      return frequency >= 5000 ? '5 GHz' : '2.4 GHz';
    }
    return 'Desconocida';
  }

  private calculateSignalLevel(rssi: number): 'excelente' | 'buena' | 'regular' | 'débil' {
    if (rssi >= -52) return 'excelente';
    if (rssi >= -67) return 'buena';
    if (rssi >= -80) return 'regular';
    return 'débil';
  }
}
