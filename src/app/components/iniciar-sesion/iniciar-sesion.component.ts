import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { Auth } from '../../services/auth';

@Component({
  selector: 'app-iniciar-sesion',
  templateUrl: './iniciar-sesion.component.html',
  styleUrls: ['./iniciar-sesion.component.scss'],
  standalone: true,
  imports: [IonicModule, FormsModule, CommonModule, RouterModule]
})
export class IniciarSesionComponent {
  correo = '';
  contrasena = '';
  mostrarContrasena = false;
  iniciandoSesion = false;

  constructor(
    private router: Router,
    private authService: Auth
  ) {}

  toggleContrasena() {
    this.mostrarContrasena = !this.mostrarContrasena;
  }

  async manejarInicioSesion() {
    if (!this.correo.trim() || !this.contrasena.trim()) {
      alert('Ingresa tu correo y contrasena');
      return;
    }

    this.iniciandoSesion = true;

    try {
      await this.authService.iniciarSesion(this.correo.trim(), this.contrasena);
      this.router.navigate(['/tabs/inicio']);
    } catch (error) {
      alert(this.obtenerMensajeError(error));
    } finally {
      this.iniciandoSesion = false;
    }
  }

  private obtenerMensajeError(error: unknown): string {
    const codigo = typeof error === 'object' && error !== null && 'code' in error
      ? String(error.code)
      : '';

    switch (codigo) {
      case 'auth/invalid-email':
        return 'Ingresa un correo valido.';
      case 'auth/invalid-credential':
      case 'auth/user-not-found':
      case 'auth/wrong-password':
        return 'Correo o contrasena incorrectos.';
      case 'auth/operation-not-allowed':
        return 'Activa el inicio de sesion con correo y contrasena en Firebase Authentication.';
      case 'auth/network-request-failed':
        return 'No se pudo conectar con Firebase. Revisa tu conexion a internet.';
      default:
        return 'No se pudo iniciar sesion. Intenta de nuevo.';
    }
  }
}
