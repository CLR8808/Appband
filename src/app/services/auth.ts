import { Injectable } from '@angular/core';
import {
  Auth as FirebaseAuth,
  User,
  UserCredential,
  authState,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
} from '@angular/fire/auth';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  readonly usuarioActual$: Observable<User | null>;

  constructor(private firebaseAuth: FirebaseAuth) {
    this.usuarioActual$ = authState(this.firebaseAuth);
  }

  iniciarSesion(correo: string, contrasena: string): Promise<UserCredential> {
    return signInWithEmailAndPassword(this.firebaseAuth, correo, contrasena);
  }

  crearCuenta(correo: string, contrasena: string): Promise<UserCredential> {
    return createUserWithEmailAndPassword(this.firebaseAuth, correo, contrasena);
  }

  cerrarSesion(): Promise<void> {
    return signOut(this.firebaseAuth);
  }

  get usuarioActual(): User | null {
    return this.firebaseAuth.currentUser;
  }
}
