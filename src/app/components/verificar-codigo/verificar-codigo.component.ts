import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-verificar-codigo',
  templateUrl: './verificar-codigo.component.html',
  styleUrls: ['./verificar-codigo.component.scss'],
  standalone: true,
  imports: [IonicModule, FormsModule, CommonModule]
})
export class VerificarCodigoComponent {
  codigo = ['', '', '', ''];

  constructor(private router: Router) {}

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
    if (codigoCompleto.length === 4) {
      this.router.navigate(['/tabs/inicio']);
    } else {
      alert('Por favor ingresa los 4 dígitos');
    }
  }
}
