import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

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
  usuario = '';
  contrasena = '';
  confirmarContrasena = '';
  mostrarContrasena = false;

  constructor(private router: Router) {}

  get emailValido(): boolean {
    return this.email.includes('@') && this.email.toLowerCase().includes('.com');
  }

  get usuarioValido(): boolean {
    return this.usuario.trim().length > 0 && !this.usuario.includes(' ');
  }

  manejarTelefono(event: any) {
    const texto = event.target.value;
    const soloNumeros = texto.replace(/\D/g, '').slice(0, 10);
    this.telefono = soloNumeros;
    event.target.value = soloNumeros;
  }

  toggleMostrarContrasena() {
    this.mostrarContrasena = !this.mostrarContrasena;
  }

  validarFormulario() {
    if (!this.nombre.trim()) {
      alert('Ingresa el nombre.');
      return;
    }
    if (this.telefono.length !== 10) {
      alert('Debe tener 10 dígitos.');
      return;
    }
    if (!this.emailValido) {
      alert('Ingresa un correo válido.');
      return;
    }
    if (!this.usuarioValido) {
      alert('Ingresa un usuario válido.');
      return;
    }
    if (!this.contrasena) {
      alert('Ingresa una contraseña.');
      return;
    }
    if (this.contrasena !== this.confirmarContrasena) {
      alert('Las contraseñas no coinciden.');
      return;
    }

    this.router.navigate(['/verificar-codigo']);
  }

  goBack() {
    this.router.navigate(['/iniciar-sesion']);
  }
}
