import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { firstValueFrom } from 'rxjs';
import { Auth } from '../../services/auth';
import { Usuario } from '../../services/usuario';
import { PerfilUsuario } from '../../interfaces/usuario';

@Component({
  selector: 'app-detalles-usuario',
  templateUrl: './detalles-usuario.component.html',
  styleUrls: ['./detalles-usuario.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, FormsModule]
})
export class DetallesUsuarioComponent implements OnInit {
  editando = false;
  guardando = false;
  cargando = true;

  nombre = '';
  genero = '';
  fechaNacimiento = '';
  telefono = '';
  correo = '';
  uid = '';

  // Valores originales para cancelar
  private original = { nombre: '', genero: '', fechaNacimiento: '', telefono: '' };

  constructor(
    private router: Router,
    private authService: Auth,
    private usuarioService: Usuario
  ) {}

  ngOnInit() {
    this.authService.usuarioActual$.subscribe(async (usuario) => {
      if (!usuario) {
        return;
      }

      this.uid = usuario.uid;

      try {
        const perfil = await firstValueFrom(this.usuarioService.obtenerUsuario(usuario.uid));
        if (perfil) {
          this.nombre = perfil.nombre || '';
          this.genero = perfil.genero || '';
          this.fechaNacimiento = this.normalizarFecha(perfil.fechaNacimiento || '');
          this.telefono = perfil.telefono || '';
          this.correo = perfil.correo || usuario.email || '';
        } else {
          this.correo = usuario.email || '';
        }
      } catch {
        this.correo = usuario.email || '';
      }

      this.guardarOriginal();
      this.cargando = false;
    });
  }

  goBack() {
    this.router.navigate(['/tabs/perfil']);
  }

  activarEdicion() {
    this.guardarOriginal();
    this.editando = true;
  }

  cancelarEdicion() {
    this.nombre = this.original.nombre;
    this.genero = this.original.genero;
    this.fechaNacimiento = this.original.fechaNacimiento;
    this.telefono = this.original.telefono;
    this.editando = false;
  }

  async guardarCambios() {
    if (!this.nombre.trim()) {
      alert('El nombre no puede estar vacío.');
      return;
    }
    if (this.telefono && this.telefono.length !== 10) {
      alert('El teléfono debe tener 10 dígitos.');
      return;
    }

    this.guardando = true;

    try {
      await this.usuarioService.actualizarUsuario(this.uid, {
        nombre: this.nombre.trim(),
        genero: this.genero,
        fechaNacimiento: this.fechaNacimiento,
        telefono: this.telefono,
      });

      this.guardarOriginal();
      this.editando = false;
    } catch {
      alert('No se pudieron guardar los cambios.');
    } finally {
      this.guardando = false;
    }
  }

  manejarTelefono(event: Event) {
    const input = event.target as HTMLInputElement;
    const soloNumeros = input.value.replace(/\D/g, '').slice(0, 10);
    this.telefono = soloNumeros;
    input.value = soloNumeros;
  }

  get generoTexto(): string {
    switch (this.genero?.toLowerCase()) {
      case 'femenino': return 'Femenino';
      case 'masculino': return 'Masculino';
      case 'otro': return 'Otro';
      default: return this.genero || 'Sin especificar';
    }
  }

  get fechaTexto(): string {
    if (!this.fechaNacimiento) return 'Sin especificar';
    const str = String(this.fechaNacimiento);

    // Formato ISO: YYYY-MM-DD o YYYY-MM-DDTHH:mm:ss...
    const isoMatch = str.match(/^(\d{4})-(\d{2})-(\d{2})/);
    if (isoMatch) {
      const [, anio, mes, dia] = isoMatch;
      return `${dia}/${mes}/${anio}`;
    }

    // Formato DD/MM/YYYY (ya en el formato correcto)
    const dmyMatch = str.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (dmyMatch) {
      return str;
    }

    return str;
  }

  private normalizarFecha(fecha: string): string {
    if (!fecha) return '';
    const str = String(fecha);

    // Ya está en formato YYYY-MM-DD: devolver solo los primeros 10 caracteres
    if (/^\d{4}-\d{2}-\d{2}/.test(str)) {
      return str.substring(0, 10);
    }

    // Formato DD/MM/YYYY → convertir a YYYY-MM-DD para el input type="date"
    const dmyMatch = str.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (dmyMatch) {
      const [, dia, mes, anio] = dmyMatch;
      return `${anio}-${mes}-${dia}`;
    }

    return str;
  }

  get telefonoFormateado(): string {
    if (!this.telefono) return 'Sin especificar';
    const clean = this.telefono.replace(/\D/g, '');
    if (clean.length === 10) {
      return `(${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6)}`;
    }
    return this.telefono;
  }

  get generoIcono(): string {
    switch (this.genero?.toLowerCase()) {
      case 'femenino': return 'female-outline';
      case 'masculino': return 'male-outline';
      default: return 'transgender-outline';
    }
  }

  private guardarOriginal() {
    this.original = {
      nombre: this.nombre,
      genero: this.genero,
      fechaNacimiento: this.fechaNacimiento,
      telefono: this.telefono,
    };
  }
}
