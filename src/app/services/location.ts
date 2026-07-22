import { Injectable, signal } from '@angular/core';
import { Geolocation, Position } from '@capacitor/geolocation';

export interface DeviceCoordinates {
  lat: number;
  lng: number;
  accuracy?: number;
  timestamp?: number;
}

@Injectable({
  providedIn: 'root',
})
export class Location {
  public currentPosition = signal<DeviceCoordinates | null>(null);
  public isLoading = signal<boolean>(false);
  public error = signal<string | null>(null);

  private watchId: string | null = null;

  constructor() {}

  /**
   * Obtiene la posición actual del dispositivo vía GPS / Geolocalización
   */
  async getCurrentPosition(): Promise<DeviceCoordinates | null> {
    this.isLoading.set(true);
    this.error.set(null);

    try {
      // Intentar primero con Capacitor Geolocation plugin
      const position: Position = await Geolocation.getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 10000,
      });

      const coords: DeviceCoordinates = {
        lat: position.coords.latitude,
        lng: position.coords.longitude,
        accuracy: position.coords.accuracy,
        timestamp: position.timestamp,
      };

      this.currentPosition.set(coords);
      this.isLoading.set(false);
      return coords;

    } catch (err: any) {
      console.warn('Capacitor Geolocation error, probando Web Geolocation API...', err);
      return await this.getWebGeolocation();
    }
  }

  /**
   * Fallback usando la API estándar de navegador Web
   */
  private getWebGeolocation(): Promise<DeviceCoordinates | null> {
    return new Promise((resolve) => {
      if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            const coords: DeviceCoordinates = {
              lat: pos.coords.latitude,
              lng: pos.coords.longitude,
              accuracy: pos.coords.accuracy,
              timestamp: pos.timestamp,
            };
            this.currentPosition.set(coords);
            this.isLoading.set(false);
            resolve(coords);
          },
          (err) => {
            console.error('Error al obtener geolocalización:', err);
            this.error.set('No se pudo obtener la ubicación GPS: ' + err.message);
            this.isLoading.set(false);
            resolve(null);
          },
          { enableHighAccuracy: true, timeout: 10000 }
        );
      } else {
        this.error.set('El navegador no soporta servicios de ubicación.');
        this.isLoading.set(false);
        resolve(null);
      }
    });
  }

  /**
   * Inicia el seguimiento continuo de la ubicación GPS en tiempo real
   */
  async startWatch(callback?: (coords: DeviceCoordinates) => void) {
    try {
      this.watchId = await Geolocation.watchPosition(
        { enableHighAccuracy: true },
        (position, err) => {
          if (position) {
            const coords: DeviceCoordinates = {
              lat: position.coords.latitude,
              lng: position.coords.longitude,
              accuracy: position.coords.accuracy,
              timestamp: position.timestamp,
            };
            this.currentPosition.set(coords);
            if (callback) callback(coords);
          }
          if (err) {
            console.error('Error en seguimiento GPS:', err);
          }
        }
      );
    } catch (e) {
      console.warn('Error iniciando watchPosition en Capacitor, usando fallback HTML5:', e);
    }
  }

  /**
   * Detiene el seguimiento continuo
   */
  async stopWatch() {
    if (this.watchId) {
      try {
        await Geolocation.clearWatch({ id: this.watchId });
      } catch (e) {}
      this.watchId = null;
    }
  }
}

