import { Injectable } from '@angular/core';
import { Firestore, doc, docData, serverTimestamp, setDoc } from '@angular/fire/firestore';
import { Observable, catchError, concat, map, of, tap } from 'rxjs';
import { PerfilUsuario } from '../interfaces/usuario';

@Injectable({
  providedIn: 'root',
})
export class Usuario {
  private readonly perfilCacheKey = 'safestep_perfil_usuario';

  constructor(private firestore: Firestore) {}

  guardarUsuario(usuario: Omit<PerfilUsuario, 'fechaCreacion'>): Promise<void> {
    this.guardarUsuarioLocal(usuario);

    const usuarioRef = doc(this.firestore, `usuarios/${usuario.uid}`);

    return setDoc(usuarioRef, {
      ...usuario,
      fechaCreacion: serverTimestamp(),
    });
  }

  obtenerUsuario(uid: string): Observable<PerfilUsuario | undefined> {
    const usuarioRef = doc(this.firestore, `usuarios/${uid}`);
    const usuarioLocal = this.obtenerUsuarioLocal(uid);
    const usuarioFirestore$ = (docData(usuarioRef) as Observable<PerfilUsuario | undefined>).pipe(
      map((usuario) => usuario || usuarioLocal),
      tap((usuario) => {
        if (usuario) {
          this.guardarUsuarioLocal(usuario);
        }
      }),
      catchError(() => of(usuarioLocal))
    );

    return usuarioLocal
      ? concat(of(usuarioLocal), usuarioFirestore$)
      : usuarioFirestore$;
  }

  guardarUsuarioLocal(usuario: Omit<PerfilUsuario, 'fechaCreacion'>): void {
    try {
      localStorage.setItem(this.perfilCacheKey, JSON.stringify(usuario));
    } catch {
      // localStorage puede fallar si el navegador lo bloquea.
    }
  }

  obtenerUsuarioLocal(uid?: string): PerfilUsuario | undefined {
    try {
      const contenido = localStorage.getItem(this.perfilCacheKey);

      if (!contenido) {
        return undefined;
      }

      const usuario = JSON.parse(contenido) as PerfilUsuario;

      if (uid && usuario.uid !== uid) {
        return undefined;
      }

      return usuario;
    } catch {
      return undefined;
    }
  }
}
