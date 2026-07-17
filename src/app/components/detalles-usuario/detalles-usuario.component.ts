import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-detalles-usuario',
  templateUrl: './detalles-usuario.component.html',
  styleUrls: ['./detalles-usuario.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class DetallesUsuarioComponent {
  constructor(private router: Router) {}

  goBack() {
    this.router.navigate(['/tabs/perfil']);
  }
}
