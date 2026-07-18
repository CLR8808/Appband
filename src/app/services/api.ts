import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegistroUsuarioRequest {
  nombre: string;
  correo: string;
  genero: string;
  telefono: string;
  password: string;
  fechaNacimiento: string;
}

@Injectable({
  providedIn: 'root',
})
export class Api {
  // Backend opcional de Render. El registro actual usa Firebase Auth + Firestore directo.
  private readonly apiUrl = environment.apiUrl.replace(/\/$/, '');

  constructor(private http: HttpClient) {}

  registrarUsuario(datos: RegistroUsuarioRequest): Promise<unknown> {
    return firstValueFrom(
      this.http.post(`${this.apiUrl}/usuarios/registro`, datos)
    );
  }
}
