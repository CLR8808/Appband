import { Injectable, signal } from '@angular/core';
import { BleClient, BleDevice } from '@capacitor-community/bluetooth-le';

export interface BluetoothDeviceItem {
  deviceId: string;
  name?: string;
  rssi?: number;
  rawDevice?: any;
}

@Injectable({
  providedIn: 'root',
})
export class Bluetooth {
  public devices = signal<BluetoothDeviceItem[]>([]);
  public isScanning = signal<boolean>(false);
  public selectedDevice = signal<BluetoothDeviceItem | null>(null);
  public error = signal<string | null>(null);

  private scanTimeout: any = null;

  constructor() {}

  /**
   * Inicializa el cliente Bluetooth LE
   */
  async initialize(): Promise<boolean> {
    try {
      await BleClient.initialize();
      return true;
    } catch (e: any) {
      console.warn('BleClient initialization warning:', e);
      return false;
    }
  }

  /**
   * Inicia la búsqueda de dispositivos Bluetooth en tiempo real.
   * Funciona tanto en entorno Nativo (Capacitor BLE) como en Web (Web Bluetooth API fallback).
   */
  async startScan(durationMs: number = 10000): Promise<void> {
    this.devices.set([]);
    this.error.set(null);
    this.isScanning.set(true);

    try {
      await this.initialize();
      
      try {
        const isEnabled = await BleClient.isEnabled();
        if (!isEnabled) {
          await BleClient.requestEnable();
        }
      } catch (err) {
        console.log('No se pudo verificar si Bluetooth está activo:', err);
      }

      await BleClient.requestLEScan(
        { allowDuplicates: false },
        (result) => {
          if (result && result.device) {
            const newItem: BluetoothDeviceItem = {
              deviceId: result.device.deviceId,
              name: result.device.name || result.localName || 'Safeband / Dispositivo BLE',
              rssi: result.rssi,
              rawDevice: result.device,
            };
            this.addDeviceUnique(newItem);
          }
        }
      );

      if (this.scanTimeout) clearTimeout(this.scanTimeout);
      this.scanTimeout = setTimeout(async () => {
        await this.stopScan();
      }, durationMs);

    } catch (err: any) {
      console.warn('Capacitor BleClient no disponible en este entorno, usando fallback Web...', err);
      await this.scanWebBluetooth();
    }
  }

  /**
   * Escaneo de dispositivos en navegador Web usando Web Bluetooth API
   */
  private async scanWebBluetooth() {
    if (typeof navigator !== 'undefined' && 'bluetooth' in navigator) {
      try {
        const navBt = (navigator as any).bluetooth;
        const device = await navBt.requestDevice({
          acceptAllDevices: true,
          optionalServices: ['generic_access']
        });

        if (device) {
          const item: BluetoothDeviceItem = {
            deviceId: device.id || 'web-ble-device',
            name: device.name || 'Safeband Cercana',
            rawDevice: device,
          };
          this.addDeviceUnique(item);
          this.selectedDevice.set(item);
        }
      } catch (webErr: any) {
        console.error('Web Bluetooth error o cancelado:', webErr);
        if (webErr.name !== 'NotFoundError') {
          this.error.set('No se pudieron buscar dispositivos Bluetooth: ' + (webErr.message || webErr));
        }
      } finally {
        this.isScanning.set(false);
      }
    } else {
      this.error.set('No se detectó el módulo nativo de Bluetooth ni soporte Web Bluetooth.');
      this.isScanning.set(false);
    }
  }

  /**
   * Detiene el escaneo activo de Bluetooth
   */
  async stopScan(): Promise<void> {
    if (this.scanTimeout) {
      clearTimeout(this.scanTimeout);
      this.scanTimeout = null;
    }
    try {
      await BleClient.stopLEScan();
    } catch (e) {
      // ignorar si no estaba escaneando
    } finally {
      this.isScanning.set(false);
    }
  }

  /**
   * Selecciona un dispositivo de la lista
   */
  selectDevice(device: BluetoothDeviceItem) {
    this.selectedDevice.set(device);
  }

  /**
   * Agrega un dispositivo a la lista evitando duplicados
   */
  private addDeviceUnique(device: BluetoothDeviceItem) {
    const current = this.devices();
    const exists = current.some((d) => d.deviceId === device.deviceId);
    if (!exists) {
      this.devices.set([...current, device]);
    }
  }
}

