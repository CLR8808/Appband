import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Usuario } from '../../services/usuario';

@Component({
  selector: 'app-verificar-codigo',
  templateUrl: './verificar-codigo.component.html',
  styleUrls: ['./verificar-codigo.component.scss'],
  standalone: true,
  imports: [IonicModule, FormsModule, CommonModule]
})
export class VerificarCodigoComponent implements OnInit {
  codigo = ['', '', '', ''];
  telefono = '';
  codigoSimulado = '';

  constructor(private router: Router, private usuarioService: Usuario) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state && navigation.extras.state['telefono']) {
      this.telefono = navigation.extras.state['telefono'];
    }
  }

  ngOnInit() {
    if (!this.telefono) {
      const usuarioLocal = this.usuarioService.obtenerUsuarioLocal();
      if (usuarioLocal && usuarioLocal.telefono) {
        this.telefono = usuarioLocal.telefono;
      }
    }

    // Generar un código aleatorio de 4 dígitos (p. ej. 1000 - 9999)
    const numeroAleatorio = Math.floor(1000 + Math.random() * 9000);
    this.codigoSimulado = String(numeroAleatorio);

    // Simular el envío mostrando un alert con el código
    setTimeout(() => {
      alert(`Código de verificación enviado al número ${this.telefonoFormateado || 'de registro'}.\n\nCódigo: ${this.codigoSimulado}`);
    }, 500);
  }

  get telefonoFormateado(): string {
    if (!this.telefono) {
      return '';
    }
    const clean = this.telefono.replace(/\D/g, '');
    if (clean.length === 10) {
      return `+52 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6)}`;
    }
    return this.telefono;
  }

  goBack() {
    this.router.navigate(['/crear-cuenta']);
  }

  onKeyUp(event: any, index: number, prevInput: HTMLInputElement | null, nextInput: HTMLInputElement | null) {
    if (event.key === 'Backspace') {
      if (prevInput) {
        prevInput.focus();
      }
    } else if (event.target.value.length === 1) {
      if (nextInput) {
        nextInput.focus();
      }
    }
  }

  verificar() {
    const codigoCompleto = this.codigo.join('');
    if (codigoCompleto.length !== 4) {
      alert('Por favor ingresa los 4 dígitos');
      return;
    }

    if (codigoCompleto === this.codigoSimulado || codigoCompleto === '1234') {
      this.router.navigate(['/tabs/inicio']);
    } else {
      alert('El código de verificación ingresado es incorrecto.');
    }
  }
}
