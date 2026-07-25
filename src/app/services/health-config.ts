// ============================================================
// Servicio de Configuración de Salud
// Constantes, catálogos y utilidades para el formulario
// de registro de pacientes.
// ============================================================

import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class HealthConfig {

  // ── Catálogo de enfermedades cardíacas ──────────────────
  readonly enfermedadesCardiacas: { valor: string; etiqueta: string }[] = [
    { valor: 'arritmia', etiqueta: 'Arritmia' },
    { valor: 'bradicardia', etiqueta: 'Bradicardia (pulso lento)' },
    { valor: 'taquicardia', etiqueta: 'Taquicardia (pulso rápido)' },
    { valor: 'fibrilacion_auricular', etiqueta: 'Fibrilación auricular' },
    { valor: 'insuficiencia_cardiaca', etiqueta: 'Insuficiencia cardíaca' },
    { valor: 'hipertension', etiqueta: 'Hipertensión' },
    { valor: 'hipotension', etiqueta: 'Hipotensión' },
    { valor: 'cardiopatia_isquemica', etiqueta: 'Cardiopatía isquémica' },
    { valor: 'infarto_previo', etiqueta: 'Infarto previo' },
    { valor: 'otra', etiqueta: 'Otra (especificar)' },
  ];

  // ── Catálogo de medicamentos ────────────────────────────
  readonly medicamentos: { valor: string; etiqueta: string }[] = [
    { valor: 'betabloqueadores', etiqueta: 'Betabloqueadores' },
    { valor: 'digitalicos', etiqueta: 'Digitálicos' },
    { valor: 'antiarritmicos', etiqueta: 'Antiarrítmicos' },
    { valor: 'calcioantagonistas', etiqueta: 'Calcioantagonistas' },
    { valor: 'ninguno', etiqueta: 'Ninguno' },
    { valor: 'otros', etiqueta: 'Otros (especificar)' },
  ];

  // ── Tipos de sangre ─────────────────────────────────────
  readonly tiposSangre: string[] = [
    'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-',
  ];

  // ── Géneros ─────────────────────────────────────────────
  readonly generos: string[] = [
    'Masculino', 'Femenino', 'No binario', 'Prefiero no decir',
  ];

  // ── Parentescos ─────────────────────────────────────────
  readonly parentescos: string[] = [
    'Hijo/a', 'Esposo/a', 'Hermano/a', 'Nieto/a',
    'Sobrino/a', 'Cuidador/a', 'Amigo/a', 'Otro',
  ];

  // ── Sensibilidades de detección de caídas ───────────────
  readonly sensibilidadesCaidas: { valor: string; etiqueta: string; descripcion: string }[] = [
    { valor: 'alta', etiqueta: 'Alta', descripcion: 'Detecta movimientos leves — menos falsos negativos' },
    { valor: 'media', etiqueta: 'Media', descripcion: 'Balance entre sensibilidad y falsas alarmas' },
    { valor: 'baja', etiqueta: 'Baja', descripcion: 'Solo detecta caídas fuertes — menos falsas alarmas' },
  ];

  // ── Frecuencias de desmayos ─────────────────────────────
  readonly frecuenciasDesmayos: string[] = [
    '1-2 veces', '3-5 veces', 'Más de 5 veces', 'Mensualmente',
  ];

  // ── Números de emergencia ───────────────────────────────
  readonly numerosEmergencia: { valor: string; etiqueta: string }[] = [
    { valor: '911', etiqueta: '911 (México/EE.UU.)' },
    { valor: '061', etiqueta: '061 (España)' },
    { valor: '112', etiqueta: '112 (Europa)' },
    { valor: 'otro', etiqueta: 'Otro número' },
  ];

  // ── Valores por defecto de umbrales ─────────────────────
  readonly umbralesDefecto = {
    pulsoMaximo: 120,
    pulsoMinimo: 50,
    sensibilidadCaidas: 'media' as const,
    tiempoInactividad: 5,
  };

  // ── Límites de validación ───────────────────────────────
  readonly limites = {
    pesoMin: 30,
    pesoMax: 300,
    alturaMin: 100,
    alturaMax: 250,
    edadMinima: 18,
    pulsoAbsolutoMin: 30,
    pulsoAbsolutoMax: 220,
    maxContactosEmergencia: 3,
  };

  // ── Utilidades ──────────────────────────────────────────

  /** Calcula la edad a partir de una fecha de nacimiento (ISO string) */
  calcularEdad(fechaNacimiento: string): number {
    if (!fechaNacimiento) return 0;
    const hoy = new Date();
    const nacimiento = new Date(fechaNacimiento);
    let edad = hoy.getFullYear() - nacimiento.getFullYear();
    const mesActual = hoy.getMonth();
    const mesNac = nacimiento.getMonth();
    if (mesActual < mesNac || (mesActual === mesNac && hoy.getDate() < nacimiento.getDate())) {
      edad--;
    }
    return edad;
  }

  /** Retorna la zona de riesgo para un valor de pulso dados los umbrales */
  zonaRiesgoPulso(bpm: number, min: number, max: number): 'normal' | 'precaucion' | 'peligro' {
    if (bpm >= min && bpm <= max) return 'normal';
    const margenPrecaucion = 10;
    if (bpm < min && bpm >= min - margenPrecaucion) return 'precaucion';
    if (bpm > max && bpm <= max + margenPrecaucion) return 'precaucion';
    return 'peligro';
  }

  /** Valida que el pulso máximo sea mayor al mínimo */
  validarRangoPulso(min: number, max: number): boolean {
    return max > min && min >= this.limites.pulsoAbsolutoMin && max <= this.limites.pulsoAbsolutoMax;
  }
}
