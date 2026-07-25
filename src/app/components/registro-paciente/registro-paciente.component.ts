// ============================================================
// Componente de Registro de Pacientes
// Formulario de 6 secciones con validación en tiempo real,
// barra de progreso, dictado por voz, y confirmación final.
// ============================================================

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { IonicModule, AlertController, ToastController } from '@ionic/angular';
import { Subscription } from 'rxjs';
import { take } from 'rxjs/operators';

import { HealthConfig } from '../../services/health-config';
import { Paciente } from '../../services/paciente';
import { Auth } from '../../services/auth';
import {
  RegistroPaciente,
  ContactoEmergencia,
} from '../../interfaces/paciente';

@Component({
  selector: 'app-registro-paciente',
  templateUrl: './registro-paciente.component.html',
  styleUrls: ['./registro-paciente.component.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, ReactiveFormsModule],
})
export class RegistroPacienteComponent implements OnInit, OnDestroy {

  // ── Estado del formulario ─────────────────────────────────
  form!: FormGroup;
  pasoActual = 0;
  totalPasos = 7;               // 6 secciones + 1 resumen
  guardando = false;
  enviado = false;               // True tras enviar exitosamente
  edadCalculada: number | null = null;
  cargandoDatos = true;

  // ── Estado de chips (enfermedades y medicamentos) ─────────
  enfermedadesSeleccionadas: Set<string> = new Set();
  medicamentosSeleccionados: Set<string> = new Set();

  // ── Dictado por voz ───────────────────────────────────────
  vozSoportada = false;
  grabandoVoz = false;
  campoVozActivo: string | null = null;
  private reconocimientoVoz: any = null;

  // ── Subscripciones ────────────────────────────────────────
  private subs: Subscription[] = [];

  constructor(
    private fb: FormBuilder,
    public healthConfig: HealthConfig,
    private pacienteService: Paciente,
    private authService: Auth,
    private alertCtrl: AlertController,
    private toastCtrl: ToastController,
  ) {}

  ngOnInit() {
    this.inicializarFormulario();
    this.cargarDatosExistentes();
    this.inicializarVoz();
  }

  ngOnDestroy() {
    this.subs.forEach((s) => s.unsubscribe());
    this.detenerVoz();
  }

  // ══════════════════════════════════════════════════════════
  // INICIALIZACIÓN DEL FORMULARIO
  // ══════════════════════════════════════════════════════════

  private inicializarFormulario() {
    const limites = this.healthConfig.limites;
    const defecto = this.healthConfig.umbralesDefecto;

    this.form = this.fb.group({
      // Sección 1 — Datos del Paciente
      datosPaciente: this.fb.group({
        nombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
        fechaNacimiento: ['', Validators.required],
        genero: ['', Validators.required],
        peso: [null, [Validators.required, Validators.min(limites.pesoMin), Validators.max(limites.pesoMax)]],
        altura: [null, [Validators.required, Validators.min(limites.alturaMin), Validators.max(limites.alturaMax)]],
        tipoSangre: ['', Validators.required],
        fotoPerfil: [''],
        direccion: ['', Validators.required],
        telefono: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-()]{7,15}$/)]],
      }),

      // Sección 2 — Información Médica
      infoMedica: this.fb.group({
        tieneMarcapasos: [false],
        enfermedadesCardiacas: [[] as string[]],
        otraEnfermedad: [''],
        medicamentos: [[] as string[]],
        otroMedicamento: [''],
        fcReposo: [70, [Validators.min(30), Validators.max(220)]],
        desmayosTiene: [false],
        desmayosFrecuencia: [''],
        historialCaidasTiene: [false],
        historialCaidasCantidad: [0, [Validators.min(0)]],
        tieneDesfibrilador: [false],
      }),

      // Sección 3 — Umbrales de Alerta
      umbralesAlerta: this.fb.group({
        pulsoMaximo: [defecto.pulsoMaximo, [Validators.required, Validators.min(limites.pulsoAbsolutoMin), Validators.max(limites.pulsoAbsolutoMax)]],
        pulsoMinimo: [defecto.pulsoMinimo, [Validators.required, Validators.min(limites.pulsoAbsolutoMin), Validators.max(limites.pulsoAbsolutoMax)]],
        sensibilidadCaidas: [defecto.sensibilidadCaidas, Validators.required],
      }),

      // Sección 4 — Datos del Tutor
      datosTutor: this.fb.group({
        nombreCompleto: ['', [Validators.required, Validators.minLength(3)]],
        parentesco: ['', Validators.required],
        telefonoPrincipal: ['', [Validators.required, Validators.pattern(/^\+?[\d\s\-()]{7,15}$/)]],
        telefonoAlternativo: ['', Validators.pattern(/^\+?[\d\s\-()]{7,15}$/)],
        correo: ['', [Validators.required, Validators.email]],
        notificacionesSMS: [true],
        notificacionesCorreo: [true],
        direccion: ['', Validators.required],
      }),

      // Sección 5 — Contactos de Emergencia (FormArray, máx 3)
      contactosEmergencia: this.fb.array([]),

      // Sección 6 — Configuración de Alertas
      configuracionAlertas: this.fb.group({
        deteccionCaidas: [true],
        tiempoInactividad: [defecto.tiempoInactividad, [Validators.required, Validators.min(1), Validators.max(60)]],
        notificarTutorPulso: [true],
        numeroEmergencias: ['911', Validators.required],
      }),
    });

    // Agregar un contacto de emergencia por defecto
    this.agregarContactoEmergencia();

    // Escuchar cambios en fecha de nacimiento para calcular edad
    const fechaSub = this.datosPaciente.get('fechaNacimiento')!.valueChanges.subscribe((valor: string) => {
      this.edadCalculada = this.healthConfig.calcularEdad(valor);
    });
    this.subs.push(fechaSub);
  }

  // ══════════════════════════════════════════════════════════
  // CARGA DE DATOS EXISTENTES (edición)
  // ══════════════════════════════════════════════════════════

  private cargarDatosExistentes() {
    const usuario = this.authService.usuarioActual;
    if (!usuario) {
      this.cargandoDatos = false;
      return;
    }

    this.pacienteService.obtenerPaciente(usuario.uid).pipe(take(1)).subscribe({
      next: (paciente) => {
        if (paciente) {
          this.rellenarFormulario(paciente);
        }
        this.cargandoDatos = false;
      },
      error: () => {
        this.cargandoDatos = false;
      },
    });
  }

  private rellenarFormulario(p: RegistroPaciente) {
    // Sección 1
    this.datosPaciente.patchValue(p.datosPaciente || {});
    if (p.datosPaciente?.fechaNacimiento) {
      this.edadCalculada = this.healthConfig.calcularEdad(p.datosPaciente.fechaNacimiento);
    }

    // Sección 2
    if (p.infoMedica) {
      this.infoMedica.patchValue({
        tieneMarcapasos: p.infoMedica.tieneMarcapasos,
        otraEnfermedad: p.infoMedica.otraEnfermedad,
        otroMedicamento: p.infoMedica.otroMedicamento,
        fcReposo: p.infoMedica.fcReposo,
        desmayosTiene: p.infoMedica.desmayos?.tiene,
        desmayosFrecuencia: p.infoMedica.desmayos?.frecuencia,
        historialCaidasTiene: p.infoMedica.historialCaidas?.tiene,
        historialCaidasCantidad: p.infoMedica.historialCaidas?.cantidad,
        tieneDesfibrilador: p.infoMedica.tieneDesfibrilador,
      });
      // Restaurar chips
      (p.infoMedica.enfermedadesCardiacas || []).forEach((e: string) => this.enfermedadesSeleccionadas.add(e));
      this.infoMedica.get('enfermedadesCardiacas')!.setValue([...this.enfermedadesSeleccionadas]);
      (p.infoMedica.medicamentos || []).forEach((m: string) => this.medicamentosSeleccionados.add(m));
      this.infoMedica.get('medicamentos')!.setValue([...this.medicamentosSeleccionados]);
    }

    // Sección 3
    if (p.umbralesAlerta) {
      this.umbralesAlerta.patchValue(p.umbralesAlerta);
    }

    // Sección 4
    if (p.datosTutor) {
      this.datosTutor.patchValue(p.datosTutor);
    }

    // Sección 5
    if (p.contactosEmergencia?.length) {
      this.contactosEmergenciaArray.clear();
      p.contactosEmergencia.forEach((c) => this.agregarContactoEmergencia(c));
    }

    // Sección 6
    if (p.configuracionAlertas) {
      this.configuracionAlertas.patchValue(p.configuracionAlertas);
    }
  }

  // ══════════════════════════════════════════════════════════
  // GETTERS DE CONVENIENCIA
  // ══════════════════════════════════════════════════════════

  get datosPaciente(): FormGroup { return this.form.get('datosPaciente') as FormGroup; }
  get infoMedica(): FormGroup { return this.form.get('infoMedica') as FormGroup; }
  get umbralesAlerta(): FormGroup { return this.form.get('umbralesAlerta') as FormGroup; }
  get datosTutor(): FormGroup { return this.form.get('datosTutor') as FormGroup; }
  get contactosEmergenciaArray(): FormArray { return this.form.get('contactosEmergencia') as FormArray; }
  get configuracionAlertas(): FormGroup { return this.form.get('configuracionAlertas') as FormGroup; }

  /** Progreso del formulario (0–1) */
  get progreso(): number {
    return this.pasoActual / (this.totalPasos - 1);
  }

  /** Título de cada sección */
  get titulosPasos(): string[] {
    return [
      'Datos del Paciente',
      'Información Médica',
      'Umbrales de Alerta',
      'Tutor / Familiar',
      'Contactos de Emergencia',
      'Configuración de Alertas',
      'Resumen Final',
    ];
  }

  /** Íconos de cada sección */
  get iconosPasos(): string[] {
    return [
      'person-outline',
      'heart-outline',
      'pulse-outline',
      'people-outline',
      'call-outline',
      'settings-outline',
      'checkmark-circle-outline',
    ];
  }

  // ══════════════════════════════════════════════════════════
  // NAVEGACIÓN ENTRE SECCIONES
  // ══════════════════════════════════════════════════════════

  siguientePaso() {
    if (this.pasoActual < this.totalPasos - 1) {
      this.pasoActual++;
      this.scrollAlInicio();
    }
  }

  pasoAnterior() {
    if (this.pasoActual > 0) {
      this.pasoActual--;
      this.scrollAlInicio();
    }
  }

  irAPaso(paso: number) {
    if (paso >= 0 && paso < this.totalPasos) {
      this.pasoActual = paso;
      this.scrollAlInicio();
    }
  }

  private scrollAlInicio() {
    const content = document.querySelector('ion-content');
    if (content) {
      (content as any).scrollToTop(300);
    }
  }

  /** Verifica si la sección actual es válida antes de avanzar */
  esPasoActualValido(): boolean {
    switch (this.pasoActual) {
      case 0: {
        const dp = this.datosPaciente;
        const edadOk = this.edadCalculada !== null && this.edadCalculada >= this.healthConfig.limites.edadMinima;
        return dp.valid && edadOk;
      }
      case 1: return true; // Info médica: todo es opcional excepto selecciones
      case 2: {
        const u = this.umbralesAlerta;
        const max = u.get('pulsoMaximo')!.value;
        const min = u.get('pulsoMinimo')!.value;
        return u.valid && max > min;
      }
      case 3: return this.datosTutor.valid;
      case 4: return this.contactosEmergenciaArray.length > 0 && this.contactosEmergenciaArray.valid;
      case 5: return this.configuracionAlertas.valid;
      case 6: return true; // Resumen
      default: return false;
    }
  }

  // ══════════════════════════════════════════════════════════
  // CHIPS — Enfermedades y Medicamentos
  // ══════════════════════════════════════════════════════════

  toggleEnfermedad(valor: string) {
    if (this.enfermedadesSeleccionadas.has(valor)) {
      this.enfermedadesSeleccionadas.delete(valor);
    } else {
      this.enfermedadesSeleccionadas.add(valor);
    }
    this.infoMedica.get('enfermedadesCardiacas')!.setValue([...this.enfermedadesSeleccionadas]);
  }

  toggleMedicamento(valor: string) {
    // Si selecciona "ninguno", deseleccionar los demás
    if (valor === 'ninguno') {
      this.medicamentosSeleccionados.clear();
      this.medicamentosSeleccionados.add('ninguno');
    } else {
      this.medicamentosSeleccionados.delete('ninguno');
      if (this.medicamentosSeleccionados.has(valor)) {
        this.medicamentosSeleccionados.delete(valor);
      } else {
        this.medicamentosSeleccionados.add(valor);
      }
    }
    this.infoMedica.get('medicamentos')!.setValue([...this.medicamentosSeleccionados]);
  }

  // ══════════════════════════════════════════════════════════
  // CONTACTOS DE EMERGENCIA (FormArray)
  // ══════════════════════════════════════════════════════════

  agregarContactoEmergencia(datos?: ContactoEmergencia) {
    if (this.contactosEmergenciaArray.length >= this.healthConfig.limites.maxContactosEmergencia) return;

    const grupo = this.fb.group({
      nombre: [datos?.nombre || '', [Validators.required, Validators.minLength(3)]],
      telefono: [datos?.telefono || '', [Validators.required, Validators.pattern(/^\+?[\d\s\-()]{7,15}$/)]],
      parentesco: [datos?.parentesco || '', Validators.required],
      llamarAutomaticamente: [datos?.llamarAutomaticamente ?? true],
    });

    this.contactosEmergenciaArray.push(grupo);
  }

  eliminarContactoEmergencia(index: number) {
    if (this.contactosEmergenciaArray.length > 1) {
      this.contactosEmergenciaArray.removeAt(index);
    }
  }

  // ══════════════════════════════════════════════════════════
  // FOTO DE PERFIL
  // ══════════════════════════════════════════════════════════

  async seleccionarFoto() {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    input.onchange = (event: any) => {
      const file = event.target.files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        this.datosPaciente.get('fotoPerfil')!.setValue(reader.result as string);
      };
      reader.readAsDataURL(file);
    };
    input.click();
  }

  // ══════════════════════════════════════════════════════════
  // DICTADO POR VOZ (Web Speech API)
  // ══════════════════════════════════════════════════════════

  private inicializarVoz() {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      this.vozSoportada = true;
      this.reconocimientoVoz = new SpeechRecognition();
      this.reconocimientoVoz.lang = 'es-ES';
      this.reconocimientoVoz.continuous = false;
      this.reconocimientoVoz.interimResults = false;

      this.reconocimientoVoz.onresult = (event: any) => {
        const texto = event.results[0][0].transcript;
        if (this.campoVozActivo) {
          const control = this.form.get(this.campoVozActivo);
          if (control) {
            const valorActual = control.value || '';
            control.setValue(valorActual ? `${valorActual} ${texto}` : texto);
          }
        }
        this.grabandoVoz = false;
        this.campoVozActivo = null;
      };

      this.reconocimientoVoz.onerror = () => {
        this.grabandoVoz = false;
        this.campoVozActivo = null;
      };

      this.reconocimientoVoz.onend = () => {
        this.grabandoVoz = false;
      };
    }
  }

  iniciarVoz(campoPath: string) {
    if (!this.vozSoportada || !this.reconocimientoVoz) return;
    this.campoVozActivo = campoPath;
    this.grabandoVoz = true;
    this.reconocimientoVoz.start();
  }

  detenerVoz() {
    if (this.reconocimientoVoz && this.grabandoVoz) {
      this.reconocimientoVoz.stop();
      this.grabandoVoz = false;
      this.campoVozActivo = null;
    }
  }

  // ══════════════════════════════════════════════════════════
  // COLOR DEL SLIDER DE PULSO
  // ══════════════════════════════════════════════════════════

  colorPulso(valor: number): string {
    if (valor < 40 || valor > 180) return '#DC2626';    // Rojo — peligro
    if (valor < 50 || valor > 120) return '#F59E0B';    // Amarillo — precaución
    return '#10B981';                                     // Verde — normal
  }

  /** Formateador del pin del slider de pulso */
  formatearPulso = (value: number) => `${value} BPM`;

  // ══════════════════════════════════════════════════════════
  // ADVERTENCIA DE MARCAPASOS
  // ══════════════════════════════════════════════════════════

  async mostrarAdvertenciaMarcapasos(activado: boolean) {
    if (!activado) return;

    const alert = await this.alertCtrl.create({
      header: '⚠️ Advertencia de Compatibilidad',
      message: 'El paciente tiene un marcapasos implantado. Algunos dispositivos de monitoreo pueden generar interferencia electromagnética. Consulte con su cardiólogo sobre la compatibilidad de la banda de monitoreo con el marcapasos.',
      buttons: ['Entendido'],
      cssClass: 'alerta-marcapasos',
    });
    await alert.present();
  }

  // ══════════════════════════════════════════════════════════
  // BOTÓN DE EMERGENCIA
  // ══════════════════════════════════════════════════════════

  async llamarEmergencia() {
    const numero = this.configuracionAlertas.get('numeroEmergencias')?.value || '911';
    const alert = await this.alertCtrl.create({
      header: '🚨 Llamar a Emergencias',
      message: `¿Desea llamar al ${numero}?`,
      buttons: [
        { text: 'Cancelar', role: 'cancel' },
        {
          text: 'Llamar',
          handler: () => {
            window.open(`tel:${numero}`, '_system');
          },
        },
      ],
    });
    await alert.present();
  }

  // ══════════════════════════════════════════════════════════
  // DATOS DEL RESUMEN
  // ══════════════════════════════════════════════════════════

  get resumenDatos() {
    const dp = this.datosPaciente.value;
    const im = this.infoMedica.value;
    const ua = this.umbralesAlerta.value;
    const dt = this.datosTutor.value;
    const ce = this.contactosEmergenciaArray.value;
    const ca = this.configuracionAlertas.value;

    return { dp, im, ua, dt, ce, ca };
  }

  obtenerEtiquetaEnfermedad(valor: string): string {
    return this.healthConfig.enfermedadesCardiacas.find((e) => e.valor === valor)?.etiqueta || valor;
  }

  obtenerEtiquetaMedicamento(valor: string): string {
    return this.healthConfig.medicamentos.find((m) => m.valor === valor)?.etiqueta || valor;
  }

  obtenerEtiquetaSensibilidad(valor: string): string {
    return this.healthConfig.sensibilidadesCaidas.find((s) => s.valor === valor)?.etiqueta || valor;
  }

  // ══════════════════════════════════════════════════════════
  // ENVÍO FINAL
  // ══════════════════════════════════════════════════════════

  async enviarFormulario() {
    const usuario = this.authService.usuarioActual;
    if (!usuario) {
      await this.mostrarToast('Debe iniciar sesión para registrar un paciente.', 'warning');
      return;
    }

    this.guardando = true;

    try {
      const im = this.infoMedica.value;

      const datos = {
        datosPaciente: {
          ...this.datosPaciente.value,
          edad: this.edadCalculada || 0,
        },
        infoMedica: {
          tieneMarcapasos: im.tieneMarcapasos,
          enfermedadesCardiacas: im.enfermedadesCardiacas,
          otraEnfermedad: im.otraEnfermedad,
          medicamentos: im.medicamentos,
          otroMedicamento: im.otroMedicamento,
          fcReposo: im.fcReposo,
          desmayos: {
            tiene: im.desmayosTiene,
            frecuencia: im.desmayosFrecuencia,
          },
          historialCaidas: {
            tiene: im.historialCaidasTiene,
            cantidad: im.historialCaidasCantidad,
          },
          tieneDesfibrilador: im.tieneDesfibrilador,
        },
        umbralesAlerta: this.umbralesAlerta.value,
        datosTutor: this.datosTutor.value,
        contactosEmergencia: this.contactosEmergenciaArray.value,
        configuracionAlertas: this.configuracionAlertas.value,
      };

      await this.pacienteService.guardarPaciente(usuario.uid, datos);

      this.guardando = false;
      this.enviado = true;

      await this.mostrarToast('Registro de paciente guardado exitosamente.', 'success');
    } catch (error) {
      this.guardando = false;
      console.error('Error al guardar paciente:', error);
      await this.mostrarToast('Error al guardar. Intente de nuevo.', 'danger');
    }
  }

  /** Reinicia el formulario para un nuevo registro */
  nuevoRegistro() {
    this.enviado = false;
    this.pasoActual = 0;
  }

  private async mostrarToast(mensaje: string, color: string) {
    const toast = await this.toastCtrl.create({
      message: mensaje,
      duration: 3000,
      position: 'top',
      color,
      icon: color === 'success' ? 'checkmark-circle' : color === 'danger' ? 'alert-circle' : 'warning',
    });
    await toast.present();
  }
}
