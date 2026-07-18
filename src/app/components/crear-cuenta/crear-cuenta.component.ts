import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { Auth } from '../../services/auth';
import { Usuario } from '../../services/usuario';

@Component({
  selector: 'app-crear-cuenta',
  templateUrl: './crear-cuenta.component.html',
  styleUrls: ['./crear-cuenta.component.scss'],
  standalone: true,
  imports: [IonicModule, FormsModule, CommonModule]
})
export class CrearCuentaComponent {
  nombre = '';
  telefono = '';
  email = '';
  genero = '';
  fechaNacimiento = '';
  contrasena = '';
  confirmarContrasena = '';
  mostrarContrasena = false;
  creandoCuenta = false;

  constructor(
    private router: Router,
    private authService: Auth,
    private usuarioService: Usuario
  ) {}

  get emailValido(): boolean {
    return this.email.includes('@') && this.email.toLowerCase().includes('.com');
  }

  manejarTelefono(event: Event) {
    const input = event.target as HTMLInputElement;
    const soloNumeros = input.value.replace(/\D/g, '').slice(0, 10);
    this.telefono = soloNumeros;
    input.value = soloNumeros;
  }

  toggleMostrarContrasena() {
    this.mostrarContrasena = !this.mostrarContrasena;
  }

  async validarFormulario() {
    if (!this.nombre.trim()) {
      alert('Ingresa el nombre.');
      return;
    }
    if (this.telefono.length !== 10) {
      alert('Debe tener 10 digitos.');
      return;
    }
    if (!this.emailValido) {
      alert('Ingresa un correo valido.');
      return;
    }
    if (!this.genero) {
      alert('Selecciona el genero.');
      return;
    }
    if (!this.fechaNacimiento) {
      alert('Ingresa la fecha de nacimiento.');
      return;
    }
    if (!this.contrasena) {
      alert('Ingresa una contrasena.');
      return;
    }
    if (this.contrasena !== this.confirmarContrasena) {
      alert('Las contrasenas no coinciden.');
      return;
    }

    this.creandoCuenta = true;

    try {
      const credencial = await this.authService.crearCuenta(this.email.trim(), this.contrasena);

      const perfil = {
        uid: credencial.user.uid,
        nombre: this.nombre.trim(),
        correo: this.email.trim(),
        genero: this.genero,
        telefono: this.telefono,
        fechaNacimiento: this.fechaNacimiento,
      };

      await this.usuarioService.guardarUsuario(perfil);

      this.router.navigate(['/verificar-codigo'], { state: { telefono: this.telefono } });
    } catch (error) {
      if (this.esErrorPermisosFirestore(error)) {
        alert('La cuenta se creo. Tus datos se mostraran en este dispositivo, pero Firestore aun no permite guardar el perfil.');
        this.router.navigate(['/verificar-codigo'], { state: { telefono: this.telefono } });
        return;
      }

      alert(this.obtenerMensajeError(error));
    } finally {
      this.creandoCuenta = false;
    }
  }

  goBack() {
    this.router.navigate(['/iniciar-sesion']);
  }

  private obtenerMensajeError(error: unknown): string {
    const codigo = typeof error === 'object' && error !== null && 'code' in error
      ? String(error.code)
      : '';

    switch (codigo) {
      case 'auth/email-already-in-use':
        return 'Ese correo ya tiene una cuenta.';
      case 'auth/invalid-email':
        return 'Ingresa un correo valido.';
      case 'auth/weak-password':
        return 'La contrasena debe tener al menos 6 caracteres.';
      case 'auth/operation-not-allowed':
        return 'Activa el registro con correo y contrasena en Firebase Authentication.';
      case 'permission-denied':
        return 'La cuenta se creo, pero Firestore no permitio guardar el perfil. Revisa las reglas de Firestore.';
      case 'auth/network-request-failed':
        return 'No se pudo conectar con Firebase. Revisa tu conexion a internet.';
      default:
        return 'No se pudo crear la cuenta. Intenta de nuevo.';
    }
  }

  private esErrorPermisosFirestore(error: unknown): boolean {
    return typeof error === 'object'
      && error !== null
      && 'code' in error
      && String(error.code) === 'permission-denied';
  }
}
