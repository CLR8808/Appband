package com.safestep.appband.helpers

import java.util.Calendar
import java.util.Date

data class CatOption(val valor: String, val etiqueta: String)
data class SensibilidadOption(val valor: String, val etiqueta: String, val descripcion: String)

/**
 * Helper de Configuración de Salud.
 * Constantes, catálogos y utilidades para el monitoreo y registro de pacientes.
 * Migrado desde services/health-config.ts
 */
object HealthConfigHelper {

    val enfermedadesCardiacas: List<CatOption> = listOf(
        CatOption("arritmia", "Arritmia"),
        CatOption("bradicardia", "Bradicardia (pulso lento)"),
        CatOption("taquicardia", "Taquicardia (pulso rápido)"),
        CatOption("fibrilacion_auricular", "Fibrilación auricular"),
        CatOption("insuficiencia_cardiaca", "Insuficiencia cardíaca"),
        CatOption("hipertension", "Hipertensión"),
        CatOption("hipotension", "Hipotensión"),
        CatOption("cardiopatia_isquemica", "Cardiopatía isquémica"),
        CatOption("infarto_previo", "Infarto previo"),
        CatOption("otra", "Otra (especificar)")
    )

    val medicamentos: List<CatOption> = listOf(
        CatOption("betabloqueadores", "Betabloqueadores"),
        CatOption("digitalicos", "Digitálicos"),
        CatOption("antiarritmicos", "Antiarrítmicos"),
        CatOption("calcioantagonistas", "Calcioantagonistas"),
        CatOption("ninguno", "Ninguno"),
        CatOption("otros", "Otros (especificar)")
    )

    val tiposSangre: List<String> = listOf(
        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    )

    val generos: List<String> = listOf(
        "Masculino", "Femenino", "No binario", "Prefiero no decir"
    )

    val parentescos: List<String> = listOf(
        "Hijo/a", "Esposo/a", "Hermano/a", "Nieto/a",
        "Sobrino/a", "Cuidador/a", "Amigo/a", "Otro"
    )

    val sensibilidadesCaidas: List<SensibilidadOption> = listOf(
        SensibilidadOption("alta", "Alta", "Detecta movimientos leves — menos falsos negativos"),
        SensibilidadOption("media", "Media", "Balance entre sensibilidad y falsas alarmas"),
        SensibilidadOption("baja", "Baja", "Solo detecta caídas fuertes — menos falsas alarmas")
    )

    val frecuenciasDesmayos: List<String> = listOf(
        "1-2 veces", "3-5 veces", "Más de 5 veces", "Mensualmente"
    )

    val numerosEmergencia: List<CatOption> = listOf(
        CatOption("911", "911 (México/EE.UU.)"),
        CatOption("061", "061 (España)"),
        CatOption("112", "112 (Europa)"),
        CatOption("otro", "Otro número")
    )

    object UmbralesDefecto {
        const val PULSO_MAXIMO = 120
        const val PULSO_MINIMO = 50
        const val SENSIBILIDAD_CAIDAS = "media"
        const val TIEMPO_INACTIVIDAD = 5
    }

    object Limites {
        const val PESO_MIN = 30.0
        const val PESO_MAX = 300.0
        const val ALTURA_MIN = 100.0
        const val ALTURA_MAX = 250.0
        const val EDAD_MINIMA = 18
        const val PULSO_ABSOLUTO_MIN = 30
        const val PULSO_ABSOLUTO_MAX = 220
        const val MAX_CONTACTOS_EMERGENCIA = 3
    }

    /**
     * Calcula la edad en años a partir de una cadena de fecha ISO (yyyy-MM-dd)
     */
    fun calcularEdad(fechaNacimiento: String): Int {
        if (fechaNacimiento.isBlank()) return 0
        try {
            val parts = fechaNacimiento.split("-")
            if (parts.size < 3) return 0

            val anio = parts[0].toInt()
            val mes = parts[1].toInt() - 1
            val dia = parts[2].substring(0, 2).toInt()

            val hoy = Calendar.getInstance()
            val nac = Calendar.getInstance().apply {
                set(anio, mes, dia)
            }

            var edad = hoy.get(Calendar.YEAR) - nac.get(Calendar.YEAR)
            if (hoy.get(Calendar.DAY_OF_YEAR) < nac.get(Calendar.DAY_OF_YEAR)) {
                edad--
            }
            return if (edad < 0) 0 else edad
        } catch (e: Exception) {
            return 0
        }
    }

    enum class ZonaRiesgo { NORMAL, PRECAUCION, PELIGRO }

    fun zonaRiesgoPulso(bpm: Int, min: Int, max: Int): ZonaRiesgo {
        if (bpm in min..max) return ZonaRiesgo.NORMAL
        val margen = 10
        if (bpm < min && bpm >= min - margen) return ZonaRiesgo.PRECAUCION
        if (bpm > max && bpm <= max + margen) return ZonaRiesgo.PRECAUCION
        return ZonaRiesgo.PELIGRO
    }

    fun validarRangoPulso(min: Int, max: Int): Boolean {
        return max > min && min >= Limites.PULSO_ABSOLUTO_MIN && max <= Limites.PULSO_ABSOLUTO_MAX
    }
}
