import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';
import { CardComponent } from '../../shared/card/card.component';

@Component({
  selector: 'app-perfil',
  templateUrl: './perfil.component.html',
  styleUrls: ['./perfil.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, CardComponent]
})
export class PerfilComponent {
  notificaciones = true;
  ubicacion = true;
  bluetooth = true;

  constructor(private router: Router) {}

  irADetalles() {
    this.router.navigate(['/detalles-usuario']);
  }

  irAConfiguracion() {
    this.router.navigate(['/configuracion']);
  }

  irASoporte() {
    this.router.navigate(['/soporte']);
  }

  cerrarSesion() {
    this.router.navigate(['/iniciar-sesion']);
  }
}
