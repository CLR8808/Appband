import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Dispositivo {
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class StorageService {

  private dispositivosSubject = new BehaviorSubject<Dispositivo[]>([]);
  public dispositivos$ = this.dispositivosSubject.asObservable();

  constructor() { }

  get dispositivos(): Dispositivo[] {
    return this.dispositivosSubject.getValue();
  }

  agregarDispositivo(nombre: string) {
    const current = this.dispositivosSubject.getValue();
    this.dispositivosSubject.next([...current, { nombre }]);
  }

  eliminarDispositivos() {
    this.dispositivosSubject.next([]);
  }
}
