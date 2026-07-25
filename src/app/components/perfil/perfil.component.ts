import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { Observable, map, of, switchMap } from 'rxjs';
import { CardComponent } from '../../shared/card/card.component';
import { Auth } from '../../services/auth';
import { Usuario } from '../../services/usuario';
import { RegistroPacienteComponent } from '../registro-paciente/registro-paciente.component';

interface PerfilVista {
  nombre: string;
  correo: string;
}

@Component({
  selector: 'app-perfil',
  templateUrl: './perfil.component.html',
  styleUrls: ['./perfil.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, CardComponent, RegistroPacienteComponent]
})
export class PerfilComponent {
  notificaciones = true;
  ubicacion = true;
  bluetooth = true;
  readonly perfil$: Observable<PerfilVista>;

  /** Pestaña activa: 'perfil' o 'paciente' */
  activeTab: 'perfil' | 'paciente' = 'perfil';

  constructor(
    private router: Router,
    private authService: Auth,
    private usuarioService: Usuario
  ) {
    this.perfil$ = this.authService.usuarioActual$.pipe(
      switchMap((usuarioAuth) => {
        if (!usuarioAuth) {
          return of({
            nombre: 'Usuario',
            correo: '',
          });
        }

        return this.usuarioService.obtenerUsuario(usuarioAuth.uid).pipe(
          map((perfil) => ({
            nombre: perfil?.nombre || usuarioAuth.displayName || 'Usuario',
            correo: perfil?.correo || usuarioAuth.email || '',
          }))
        );
      })
    );
  }

  /** Cambia la pestaña activa */
  onTabChange(event: any) {
    this.activeTab = event.detail.value;
  }

  irADetalles() {
    this.router.navigate(['/detalles-usuario']);
  }

  irAConfiguracion() {
    this.router.navigate(['/configuracion']);
  }

  irASoporte() {
    this.router.navigate(['/soporte']);
  }

  async cerrarSesion() {
    await this.authService.cerrarSesion();
    this.router.navigate(['/iniciar-sesion']);
  }
}
