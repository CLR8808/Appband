// ============================================================
// Modelo de datos para el Registro de Pacientes
// App de monitoreo con banda inteligente (pulso + caídas)
// ============================================================

/** Sección 1 — Datos personales del paciente */
export interface DatosPaciente {
  nombreCompleto: string;
  fechaNacimiento: string;       // ISO 8601 (yyyy-MM-dd)
  edad: number;                  // Calculada automáticamente
  genero: string;
  peso: number;                  // en kg (30–300)
  altura: number;                // en cm (100–250)
  tipoSangre: string;
  fotoPerfil?: string;           // Base64 o URL (opcional)
  direccion: string;
  telefono: string;
}

/** Sección 2 — Información médica relevante al monitoreo de pulso */
export interface InfoMedica {
  tieneMarcapasos: boolean;
  enfermedadesCardiacas: string[];   // Selección múltiple
  otraEnfermedad: string;            // Texto libre si selecciona "Otra"
  medicamentos: string[];            // Selección múltiple
  otroMedicamento: string;           // Texto libre si selecciona "Otros"
  fcReposo: number;                  // Frecuencia cardíaca en reposo (BPM)
  desmayos: {
    tiene: boolean;
    frecuencia: string;              // Ej: "1-2 veces", "más de 5"
  };
  historialCaidas: {
    tiene: boolean;
    cantidad: number;                // Número de caídas en el último año
  };
  tieneDesfibrilador: boolean;
}

/** Sección 3 — Umbrales de alerta personalizados */
export interface UmbralesAlerta {
  pulsoMaximo: number;               // BPM (default 120)
  pulsoMinimo: number;               // BPM (default 50)
  sensibilidadCaidas: 'alta' | 'media' | 'baja';
}

/** Sección 4 — Datos del tutor/familiar responsable */
export interface DatosTutor {
  nombreCompleto: string;
  parentesco: string;
  telefonoPrincipal: string;
  telefonoAlternativo: string;
  correo: string;
  notificacionesSMS: boolean;
  notificacionesCorreo: boolean;
  direccion: string;
}

/** Sección 5 — Contacto de emergencia (máximo 3) */
export interface ContactoEmergencia {
  nombre: string;
  telefono: string;
  parentesco: string;
  llamarAutomaticamente: boolean;
}

/** Sección 6 — Configuración de alertas */
export interface ConfiguracionAlertas {
  deteccionCaidas: boolean;
  tiempoInactividad: number;         // En minutos
  notificarTutorPulso: boolean;
  numeroEmergencias: string;         // Ej: "911", "061", "112"
}

/** Documento raíz — Unifica todas las secciones */
export interface RegistroPaciente {
  uid: string;                       // ID del usuario autenticado
  datosPaciente: DatosPaciente;
  infoMedica: InfoMedica;
  umbralesAlerta: UmbralesAlerta;
  datosTutor: DatosTutor;
  contactosEmergencia: ContactoEmergencia[];
  configuracionAlertas: ConfiguracionAlertas;
  fechaRegistro?: unknown;           // serverTimestamp()
  ultimaActualizacion?: unknown;     // serverTimestamp()
}
