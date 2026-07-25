// ============================================================
// Servicio de Pacientes
// CRUD en Firebase Firestore con caché offline en localStorage
// ============================================================

import { Injectable } from '@angular/core';
import { Firestore, doc, docData, serverTimestamp, setDoc } from '@angular/fire/firestore';
import { Observable, catchError, concat, map, of, tap } from 'rxjs';
import { RegistroPaciente } from '../interfaces/paciente';

@Injectable({
  providedIn: 'root',
})
export class Paciente {
  private readonly cacheKey = 'safestep_paciente';

  constructor(private firestore: Firestore) {}

  /**
   * Guarda o actualiza el registro completo del paciente en Firestore.
   * También actualiza la caché local para funcionamiento offline.
   */
  guardarPaciente(uid: string, datos: Omit<RegistroPaciente, 'uid' | 'fechaRegistro' | 'ultimaActualizacion'>): Promise<void> {
    const registro: any = {
      uid,
      ...datos,
      ultimaActualizacion: serverTimestamp(),
    };

    // Si es la primera vez, agregar fecha de registro
    const local = this.obtenerPacienteLocal(uid);
    if (!local) {
      registro.fechaRegistro = serverTimestamp();
    }

    // Guardar en localStorage
    this.guardarPacienteLocal({ uid, ...datos } as RegistroPaciente);

    // Guardar en Firestore (merge: true para no sobrescribir campos no incluidos)
    const pacienteRef = doc(this.firestore, `pacientes/${uid}`);
    return setDoc(pacienteRef, registro, { merge: true });
  }

  /**
   * Obtiene el registro del paciente desde Firestore con fallback a localStorage.
   * Usa el patrón concat: primero emite el dato local (instantáneo),
   * luego lo actualiza con el dato de Firestore cuando llegue.
   */
  obtenerPaciente(uid: string): Observable<RegistroPaciente | undefined> {
    const pacienteRef = doc(this.firestore, `pacientes/${uid}`);
    const pacienteLocal = this.obtenerPacienteLocal(uid);

    const pacienteFirestore$ = (docData(pacienteRef) as Observable<RegistroPaciente | undefined>).pipe(
      map((paciente) => paciente || pacienteLocal),
      tap((paciente) => {
        if (paciente) {
          this.guardarPacienteLocal(paciente);
        }
      }),
      catchError(() => of(pacienteLocal))
    );

    return pacienteLocal
      ? concat(of(pacienteLocal), pacienteFirestore$)
      : pacienteFirestore$;
  }

  /** Guarda el registro del paciente en localStorage */
  private guardarPacienteLocal(paciente: RegistroPaciente): void {
    try {
      localStorage.setItem(this.cacheKey, JSON.stringify(paciente));
    } catch {
      // localStorage puede fallar si el navegador lo bloquea
    }
  }

  /** Lee el registro del paciente desde localStorage */
  obtenerPacienteLocal(uid?: string): RegistroPaciente | undefined {
    try {
      const contenido = localStorage.getItem(this.cacheKey);
      if (!contenido) return undefined;
      const paciente = JSON.parse(contenido) as RegistroPaciente;
      if (uid && paciente.uid !== uid) return undefined;
      return paciente;
    } catch {
      return undefined;
    }
  }
}
