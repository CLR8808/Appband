import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { Observable, map, of, switchMap } from 'rxjs';
import { Auth } from '../../services/auth';
import { Usuario } from '../../services/usuario';

interface DetallesUsuarioVista {
  nombre: string;
  genero: string;
  fechaNacimiento: string;
  telefono: string;
  correo: string;
}

@Component({
  selector: 'app-detalles-usuario',
  templateUrl: './detalles-usuario.component.html',
  styleUrls: ['./detalles-usuario.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule]
})
export class DetallesUsuarioComponent {
  readonly detalles$: Observable<DetallesUsuarioVista>;

  constructor(
    private router: Router,
    private authService: Auth,
    private usuarioService: Usuario
  ) {
    this.detalles$ = this.authService.usuarioActual$.pipe(
      switchMap((usuarioAuth) => {
        if (!usuarioAuth) {
          return of(this.crearDetallesVacios());
        }

        return this.usuarioService.obtenerUsuario(usuarioAuth.uid).pipe(
          map((perfil) => ({
            nombre: perfil?.nombre || usuarioAuth.displayName || 'Usuario',
            genero: this.formatearGenero(perfil?.genero),
            fechaNacimiento: this.formatearFecha(perfil?.fechaNacimiento),
            telefono: perfil?.telefono || 'Sin telefono',
            correo: perfil?.correo || usuarioAuth.email || 'Sin correo',
          }))
        );
      })
    );
  }

  goBack() {
    this.router.navigate(['/tabs/perfil']);
  }

  private crearDetallesVacios(): DetallesUsuarioVista {
    return {
      nombre: 'Usuario',
      genero: 'Sin genero',
      fechaNacimiento: 'Sin fecha de nacimiento',
      telefono: 'Sin telefono',
      correo: 'Sin correo',
    };
  }

  private formatearGenero(genero?: string): string {
    switch (genero?.toLowerCase()) {
      case 'femenino':
        return 'Mujer';
      case 'masculino':
        return 'Hombre';
      case 'otro':
        return 'Otro';
      default:
        return genero || 'Sin genero';
    }
  }

  private formatearFecha(fecha?: string): string {
    if (!fecha) {
      return 'Sin fecha de nacimiento';
    }

    const partes = fecha.split('-').map(Number);

    if (partes.length !== 3 || partes.some(Number.isNaN)) {
      return fecha;
    }

    const [anio, mes, dia] = partes;
    const meses = [
      'Enero',
      'Febrero',
      'Marzo',
      'Abril',
      'Mayo',
      'Junio',
      'Julio',
      'Agosto',
      'Septiembre',
      'Octubre',
      'Noviembre',
      'Diciembre',
    ];

    return `${dia} ${meses[mes - 1]}, ${anio}`;
  }
}
