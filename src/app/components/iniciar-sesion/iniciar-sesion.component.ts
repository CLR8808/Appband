import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

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

  CORREO_PREDEFINIDO = 'SafeStep@gmail.com';
  CONTRASENA_PREDEFINIDA = '123456';

  constructor(private router: Router) {}

  toggleContrasena() {
    this.mostrarContrasena = !this.mostrarContrasena;
  }

  manejarInicioSesion() {
    if (!this.correo.trim() || !this.contrasena.trim()) {
      alert('Ingresa tu correo y contraseña');
      return;
    }

    if (this.correo === this.CORREO_PREDEFINIDO && this.contrasena === this.CONTRASENA_PREDEFINIDA) {
      this.router.navigate(['/tabs/inicio']);
    } else {
      alert('Correo o contraseña incorrectos');
    }
  }
}
