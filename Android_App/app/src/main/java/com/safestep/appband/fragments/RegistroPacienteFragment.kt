package com.safestep.appband.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentRegistroPacienteBinding
import com.safestep.appband.helpers.HealthConfigHelper
import com.safestep.appband.models.DatosPaciente
import com.safestep.appband.models.RegistroPaciente
import com.safestep.appband.viewmodels.RegistroPacienteViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Fragment de Registro de Pacientes.
 * Adaptado a la interfaz visual por pasos (1 de 7) idéntica al prototipo.
 * Migrado desde components/registro-paciente/registro-paciente.component.ts
 */
class RegistroPacienteFragment : Fragment() {

    private var _binding: FragmentRegistroPacienteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegistroPacienteViewModel by viewModels()
    private var fechaNacimientoIso = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroPacienteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarSpinners()
        configurarListeners()
        observarEstado()
    }

    private fun configurarSpinners() {
        val adapterGenero = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            HealthConfigHelper.generos
        )
        adapterGenero.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spGeneroPaciente.adapter = adapterGenero

        val adapterSangre = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            HealthConfigHelper.tiposSangre
        )
        adapterSangre.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTipoSangre.adapter = adapterSangre
    }

    private fun configurarListeners() {
        binding.containerFechaNacPaciente.setOnClickListener {
            mostrarDatePicker()
        }

        binding.btnNextStep.setOnClickListener {
            val stepActual = viewModel.currentStep.value
            if (stepActual < 2) {
                viewModel.siguientePaso()
            } else {
                val nombre = binding.etNombrePaciente.text.toString()
                val pesoStr = binding.etPeso.text.toString()
                val alturaStr = binding.etAltura.text.toString()
                val peso = pesoStr.toDoubleOrNull() ?: 70.0
                val altura = alturaStr.toDoubleOrNull() ?: 165.0
                val direccion = binding.etDireccionPaciente.text.toString()
                val telefono = binding.etTelefonoPaciente.text.toString()

                val datosPac = DatosPaciente(
                    nombreCompleto = nombre,
                    fechaNacimiento = fechaNacimientoIso,
                    edad = HealthConfigHelper.calcularEdad(fechaNacimientoIso),
                    genero = binding.spGeneroPaciente.selectedItem?.toString() ?: "",
                    peso = peso,
                    altura = altura,
                    tipoSangre = binding.spTipoSangre.selectedItem?.toString() ?: "",
                    direccion = direccion,
                    telefono = telefono
                )

                val registro = RegistroPaciente(datosPaciente = datosPac)
                viewModel.guardarRegistro(registro)
            }
        }

        binding.btnPrevStep.setOnClickListener {
            viewModel.pasoAnterior()
        }

        binding.switchMarcapasos.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(
                    requireContext(),
                    "⚠️ Advertencia: Consulte con el cardiólogo sobre la compatibilidad de la banda.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val monthStr = String.format(Locale.getDefault(), "%02d", month + 1)
                val dayStr = String.format(Locale.getDefault(), "%02d", dayOfMonth)
                fechaNacimientoIso = "$year-$monthStr-$dayStr"
                binding.tvFechaNacPaciente.text = fechaNacimientoIso
            },
            calendar.get(Calendar.YEAR) - 60,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentStep.collect { step ->
                        actualizarVistaPaso(step)
                    }
                }

                launch {
                    viewModel.saveSuccessEvent.collect {
                        Toast.makeText(
                            requireContext(),
                            "Registro de paciente guardado exitosamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                launch {
                    viewModel.errorMessageEvent.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun actualizarVistaPaso(step: Int) {
        val titulos = listOf(
            "Datos del Paciente",
            "Información Médica",
            "Umbrales de Alerta",
            "Tutor / Familiar",
            "Contactos de Emergencia",
            "Configuración de Alertas",
            "Resumen Final"
        )

        val stepIndex = step.coerceIn(0, titulos.size - 1)
        binding.tvStepNumber.text = "PASO ${stepIndex + 1} DE 7"
        binding.tvStepNameHeader.text = titulos[stepIndex]
        binding.tvStepMainTitle.text = titulos[stepIndex]

        val stepDots = listOf(
            binding.stepDot1, binding.stepDot2, binding.stepDot3,
            binding.stepDot4, binding.stepDot5, binding.stepDot6, binding.stepDot7
        )

        stepDots.forEachIndexed { idx, tv ->
            if (idx == stepIndex) {
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tv.setBackgroundResource(R.drawable.bg_primary_button)
            } else if (idx < stepIndex) {
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
                tv.setBackgroundResource(R.drawable.bg_input_container)
                tv.text = "✓"
            } else {
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                tv.setBackgroundResource(R.drawable.bg_input_container)
                tv.text = "${idx + 1}"
            }
        }

        binding.step1Container.visibility = if (stepIndex == 0) View.VISIBLE else View.GONE
        binding.step2Container.visibility = if (stepIndex == 1) View.VISIBLE else View.GONE
        binding.step3Container.visibility = if (stepIndex == 2) View.VISIBLE else View.GONE

        binding.btnPrevStep.visibility = if (stepIndex > 0) View.VISIBLE else View.GONE
        binding.btnNextStep.text = if (stepIndex >= 2) "Guardar y Finalizar ✓" else "Guardar y Continuar →"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
