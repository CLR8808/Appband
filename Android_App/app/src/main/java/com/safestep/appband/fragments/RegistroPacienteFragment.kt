package com.safestep.appband.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
            if (stepActual == 0) {
                // Avanzar al paso 2
                viewModel.siguientePaso()
            } else {
                // Guardar registro
                val nombre = binding.etNombrePaciente.text.toString()
                val pesoStr = binding.etPeso.text.toString()
                val alturaStr = binding.etAltura.text.toString()
                val peso = pesoStr.toDoubleOrNull() ?: 70.0
                val altura = alturaStr.toDoubleOrNull() ?: 170.0
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
                    "⚠️ Advertencia: Consulte con el cardiólogo sobre la compatibilidad de la banda de monitoreo con el marcapasos.",
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

                val edad = HealthConfigHelper.calcularEdad(fechaNacimientoIso)
                binding.tvEdadCalculada.text = "$edad años"
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

        binding.tvStepTitle.text = titulos.getOrElse(step) { "Registro Paciente" }
        binding.progressStep.progress = ((step + 1) * 100) / titulos.size

        if (step == 0) {
            binding.step1Container.visibility = View.VISIBLE
            binding.step2Container.visibility = View.GONE
            binding.btnPrevStep.visibility = View.GONE
            binding.btnNextStep.text = "Siguiente"
        } else {
            binding.step1Container.visibility = View.GONE
            binding.step2Container.visibility = View.VISIBLE
            binding.btnPrevStep.visibility = View.VISIBLE
            binding.btnNextStep.text = "Guardar"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
