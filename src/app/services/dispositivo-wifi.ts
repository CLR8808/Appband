import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface DispositivoConectado {
  ip: string;
  nombre: string;
  fechaConexion: string;
}

@Injectable({
  providedIn: 'root',
})
export class DispositivoWifi {
  public isConnecting = signal<boolean>(false);
  public isConnected  = signal<boolean>(false);
  public connectedDevice = signal<DispositivoConectado | null>(null);
  public error = signal<string | null>(null);

  private readonly storageKey = 'safestep_dispositivo_wifi';

  constructor(private http: HttpClient) {
    // Restaurar dispositivo previamente guardado
    const guardado = this.cargarDispositivoGuardado();
    if (guardado) {
      this.connectedDevice.set(guardado);
    }
  }

  // ─── Persistencia Local ────────────────────────────────────────────────────

  private cargarDispositivoGuardado(): DispositivoConectado | null {
    try {
      const data = localStorage.getItem(this.storageKey);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  private guardarDispositivo(device: DispositivoConectado): void {
    localStorage.setItem(this.storageKey, JSON.stringify(device));
    this.connectedDevice.set(device);
  }

  public desvincularDispositivo(): void {
    localStorage.removeItem(this.storageKey);
    this.connectedDevice.set(null);
    this.isConnected.set(false);
  }

  // ─── Conexión HTTP al ESP32 ────────────────────────────────────────────────

  /**
   * Intenta conectarse al ESP32 haciendo una petición HTTP a su IP.
   * El ESP32 debe tener un endpoint /status que responda con JSON.
   * Si responde exitosamente, se marca como conectado y se guarda.
   */
  async conectar(ip: string): Promise<boolean> {
    this.isConnecting.set(true);
    this.error.set(null);

    // Limpiar IP de espacios
    const ipLimpia = ip.trim();

    if (!ipLimpia) {
      this.error.set('Ingresa una dirección IP válida.');
      this.isConnecting.set(false);
      return false;
    }

    try {
      const url = `http://${ipLimpia}/status`;

      // Intentar conectar con timeout de 5 segundos
      const respuesta: any = await firstValueFrom(
        this.http.get(url, { responseType: 'json' })
      ).catch(async () => {
        // Si falla como JSON, intentar como texto
        return await firstValueFrom(
          this.http.get(url, { responseType: 'text' })
        );
      });

      // Si llegó aquí, el ESP32 respondió
      const dispositivo: DispositivoConectado = {
        ip: ipLimpia,
        nombre: (typeof respuesta === 'object' && respuesta?.nombre)
          ? respuesta.nombre
          : `ESP32 (${ipLimpia})`,
        fechaConexion: new Date().toISOString(),
      };

      this.isConnected.set(true);
      this.guardarDispositivo(dispositivo);
      console.log('[WiFi] Conectado exitosamente a:', ipLimpia);
      return true;

    } catch (err: any) {
      console.error('[WiFi] Error al conectar con ESP32:', err);
      this.error.set(
        `No se pudo conectar al dispositivo en ${ipLimpia}. ` +
        'Verifica que el ESP32 esté encendido y en la misma red WiFi.'
      );
      this.isConnected.set(false);
      return false;
    } finally {
      this.isConnecting.set(false);
    }
  }

  /**
   * Desconecta del dispositivo actual.
   */
  desconectar(): void {
    this.isConnected.set(false);
    console.log('[WiFi] Desconectado del dispositivo');
  }

  /**
   * Verifica si el dispositivo guardado sigue accesible.
   */
  async verificarConexion(): Promise<boolean> {
    const dev = this.connectedDevice();
    if (!dev) return false;

    try {
      await firstValueFrom(
        this.http.get(`http://${dev.ip}/status`, { responseType: 'text' })
      );
      this.isConnected.set(true);
      return true;
    } catch {
      this.isConnected.set(false);
      return false;
    }
  }
}
