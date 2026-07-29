package com.safestep.appband.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.safestep.appband.R
import com.safestep.appband.viewmodels.RegistroPacienteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Fragment de Registro de Paciente – 7 pasos completos.
 * Diseño fiel a las imágenes de referencia.
 */
class RegistroPacienteFragment : Fragment() {

    private val viewModel: RegistroPacienteViewModel by viewModels()

    // Step indicator circles and lines
    private lateinit var stepCircles: List<FrameLayout>
    private lateinit var stepLines: List<View>

    // Step layouts
    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var layoutStep3: LinearLayout
    private lateinit var layoutStep4: LinearLayout
    private lateinit var layoutStep5: LinearLayout
    private lateinit var layoutStep6: LinearLayout
    private lateinit var layoutStep7: LinearLayout
    private lateinit var allStepLayouts: List<LinearLayout>

    // Navigation
    private lateinit var tvStepHeader: TextView
    private lateinit var btnAnterior: MaterialButton
    private lateinit var btnSiguiente: MaterialButton

    // Step 1 Views
    private lateinit var etNombreCompleto: TextInputEditText
    private lateinit var etFechaNacimiento: TextInputEditText
    private lateinit var actvGenero: AutoCompleteTextView
    private lateinit var etPeso: TextInputEditText
    private lateinit var etAltura: TextInputEditText
    private lateinit var actvTipoSangre: AutoCompleteTextView
    private lateinit var etDireccion: TextInputEditText
    private lateinit var etTelefono: TextInputEditText

    // Step 2 Views
    private lateinit var switchMarcapasos: SwitchMaterial
    private lateinit var chipGroupEnfermedades: ChipGroup
    private lateinit var chipGroupMedicamentos: ChipGroup
    private lateinit var etFrecuenciaReposo: TextInputEditText
    private lateinit var switchDesmayos: SwitchMaterial
    private lateinit var switchCaidas: SwitchMaterial
    private lateinit var switchDesfibrilador: SwitchMaterial

    // Step 3 Views
    private lateinit var tvPulsoMaxValue: TextView
    private lateinit var tvPulsoMinValue: TextView
    private lateinit var sliderPulsoMax: Slider
    private lateinit var sliderPulsoMin: Slider
    private lateinit var radioGroupSensibilidad: RadioGroup

    // Step 4 Views
    private lateinit var etTutorNombre: TextInputEditText
    private lateinit var actvParentesco: AutoCompleteTextView
    private lateinit var etTutorTelefono: TextInputEditText
    private lateinit var etTutorTelefonoAlt: TextInputEditText
    private lateinit var etTutorCorreo: TextInputEditText
    private lateinit var switchNotifSMS: SwitchMaterial
    private lateinit var switchNotifCorreo: SwitchMaterial
    private lateinit var etTutorDireccion: TextInputEditText

    // Step 5 Views
    private lateinit var contactCardsContainer: LinearLayout
    private lateinit var btnAddContact: LinearLayout

    // Step 6 Views
    private lateinit var radioGroupModoAlerta: RadioGroup
    private lateinit var switchAlertaPulso: SwitchMaterial
    private lateinit var switchAlertaCaida: SwitchMaterial
    private lateinit var switchAlertaInactividad: SwitchMaterial
    private lateinit var switchEnviarGPS: SwitchMaterial
    private lateinit var tvIntervaloValue: TextView
    private lateinit var sliderIntervalo: Slider

    // Step 7 Views
    private lateinit var tvResumenDatos: TextView
    private lateinit var tvResumenMedica: TextView
    private lateinit var tvResumenUmbrales: TextView
    private lateinit var tvResumenTutor: TextView
    private lateinit var tvResumenContactos: TextView
    private lateinit var tvResumenConfig: TextView
    private lateinit var cbTerminos: MaterialCheckBox

    private val stepTitles = arrayOf(
        "PASO 1 DE 7", "PASO 2 DE 7", "PASO 3 DE 7", "PASO 4 DE 7",
        "PASO 5 DE 7", "PASO 6 DE 7", "PASO 7 DE 7"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_registro_paciente, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupDropdowns()
        setupListeners()
        setupStep5ContactCards()
        observeViewModel()
        updateUI(1)
    }

    private fun bindViews(view: View) {
        // Header and navigation
        tvStepHeader = view.findViewById(R.id.tvStepHeader)
        btnAnterior = view.findViewById(R.id.btnAnterior)
        btnSiguiente = view.findViewById(R.id.btnSiguiente)

        // Step indicator circles
        stepCircles = listOf(
            view.findViewById(R.id.step1Circle),
            view.findViewById(R.id.step2Circle),
            view.findViewById(R.id.step3Circle),
            view.findViewById(R.id.step4Circle),
            view.findViewById(R.id.step5Circle),
            view.findViewById(R.id.step6Circle),
            view.findViewById(R.id.step7Circle)
        )
        stepLines = listOf(
            view.findViewById(R.id.line1_2),
            view.findViewById(R.id.line2_3),
            view.findViewById(R.id.line3_4),
            view.findViewById(R.id.line4_5),
            view.findViewById(R.id.line5_6),
            view.findViewById(R.id.line6_7)
        )

        // Step layouts
        layoutStep1 = view.findViewById(R.id.layoutStep1)
        layoutStep2 = view.findViewById(R.id.layoutStep2)
        layoutStep3 = view.findViewById(R.id.layoutStep3)
        layoutStep4 = view.findViewById(R.id.layoutStep4)
        layoutStep5 = view.findViewById(R.id.layoutStep5)
        layoutStep6 = view.findViewById(R.id.layoutStep6)
        layoutStep7 = view.findViewById(R.id.layoutStep7)
        allStepLayouts = listOf(layoutStep1, layoutStep2, layoutStep3, layoutStep4, layoutStep5, layoutStep6, layoutStep7)

        // Step 1
        etNombreCompleto = view.findViewById(R.id.etNombreCompleto)
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento)
        actvGenero = view.findViewById(R.id.actvGenero)
        etPeso = view.findViewById(R.id.etPeso)
        etAltura = view.findViewById(R.id.etAltura)
        actvTipoSangre = view.findViewById(R.id.actvTipoSangre)
        etDireccion = view.findViewById(R.id.etDireccion)
        etTelefono = view.findViewById(R.id.etTelefono)

        // Step 2
        switchMarcapasos = view.findViewById(R.id.switchMarcapasos)
        chipGroupEnfermedades = view.findViewById(R.id.chipGroupEnfermedades)
        chipGroupMedicamentos = view.findViewById(R.id.chipGroupMedicamentos)
        etFrecuenciaReposo = view.findViewById(R.id.etFrecuenciaReposo)
        switchDesmayos = view.findViewById(R.id.switchDesmayos)
        switchCaidas = view.findViewById(R.id.switchCaidas)
        switchDesfibrilador = view.findViewById(R.id.switchDesfibrilador)

        // Step 3
        tvPulsoMaxValue = view.findViewById(R.id.tvPulsoMaxValue)
        tvPulsoMinValue = view.findViewById(R.id.tvPulsoMinValue)
        sliderPulsoMax = view.findViewById(R.id.sliderPulsoMax)
        sliderPulsoMin = view.findViewById(R.id.sliderPulsoMin)
        radioGroupSensibilidad = view.findViewById(R.id.radioGroupSensibilidad)

        // Step 4
        etTutorNombre = view.findViewById(R.id.etTutorNombre)
        actvParentesco = view.findViewById(R.id.actvParentesco)
        etTutorTelefono = view.findViewById(R.id.etTutorTelefono)
        etTutorTelefonoAlt = view.findViewById(R.id.etTutorTelefonoAlt)
        etTutorCorreo = view.findViewById(R.id.etTutorCorreo)
        switchNotifSMS = view.findViewById(R.id.switchNotifSMS)
        switchNotifCorreo = view.findViewById(R.id.switchNotifCorreo)
        etTutorDireccion = view.findViewById(R.id.etTutorDireccion)

        // Step 5
        contactCardsContainer = view.findViewById(R.id.contactCardsContainer)
        btnAddContact = view.findViewById(R.id.btnAddContact)

        // Step 6
        radioGroupModoAlerta = view.findViewById(R.id.radioGroupModoAlerta)
        switchAlertaPulso = view.findViewById(R.id.switchAlertaPulso)
        switchAlertaCaida = view.findViewById(R.id.switchAlertaCaida)
        switchAlertaInactividad = view.findViewById(R.id.switchAlertaInactividad)
        switchEnviarGPS = view.findViewById(R.id.switchEnviarGPS)
        tvIntervaloValue = view.findViewById(R.id.tvIntervaloValue)
        sliderIntervalo = view.findViewById(R.id.sliderIntervalo)

        // Step 7
        tvResumenDatos = view.findViewById(R.id.tvResumenDatos)
        tvResumenMedica = view.findViewById(R.id.tvResumenMedica)
        tvResumenUmbrales = view.findViewById(R.id.tvResumenUmbrales)
        tvResumenTutor = view.findViewById(R.id.tvResumenTutor)
        tvResumenContactos = view.findViewById(R.id.tvResumenContactos)
        tvResumenConfig = view.findViewById(R.id.tvResumenConfig)
        cbTerminos = view.findViewById(R.id.cbTerminos)
    }

    private fun setupDropdowns() {
        // Género dropdown
        val generos = arrayOf("Masculino", "Femenino", "Otro", "Prefiero no decir")
        actvGenero.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, generos))

        // Tipo de sangre dropdown
        val tiposSangre = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        actvTipoSangre.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposSangre))

        // Parentesco dropdown
        val parentescos = arrayOf("Padre/Madre", "Hijo/Hija", "Hermano/Hermana", "Esposo/Esposa", "Abuelo/Abuela", "Tío/Tía", "Primo/Prima", "Amigo/Amiga", "Cuidador/Enfermero", "Otro")
        actvParentesco.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, parentescos))
    }

    private fun setupListeners() {
        // ═══ Navigation ═══
        btnAnterior.setOnClickListener {
            saveCurrentStepData()
            viewModel.previousStep()
        }
        btnSiguiente.setOnClickListener {
            saveCurrentStepData()
            val current = viewModel.currentStep.value
            if (current == 7) {
                // Final step: save
                if (viewModel.validateCurrentStep()) {
                    viewModel.saveAllData()
                } else {
                    Toast.makeText(requireContext(), "Acepta los términos para continuar", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.nextStep()
            }
        }

        // ═══ Step 1: Date Picker ═══
        etFechaNacimiento.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                val date = String.format("%02d/%02d/%04d", day, month + 1, year)
                etFechaNacimiento.setText(date)
                viewModel.fechaNacimiento = date
            }, cal.get(Calendar.YEAR) - 70, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // ═══ Step 2: Chips ═══
        setupChipListeners(chipGroupEnfermedades, viewModel.enfermedadesSeleccionadas)
        setupChipListeners(chipGroupMedicamentos, viewModel.medicamentosSeleccionados)

        // ═══ Step 3: Sliders ═══
        sliderPulsoMax.addOnChangeListener { _, value, _ ->
            viewModel.pulsoMaximo = value.toInt()
            tvPulsoMaxValue.text = value.toInt().toString()
        }
        sliderPulsoMin.addOnChangeListener { _, value, _ ->
            viewModel.pulsoMinimo = value.toInt()
            tvPulsoMinValue.text = value.toInt().toString()
        }
        radioGroupSensibilidad.setOnCheckedChangeListener { _, checkedId ->
            viewModel.sensibilidadCaidas = when (checkedId) {
                R.id.radioAlta -> "Alta"
                R.id.radioMedia -> "Media"
                R.id.radioBaja -> "Baja"
                else -> "Media"
            }
        }

        // ═══ Step 4: Toggles ═══
        switchNotifSMS.setOnCheckedChangeListener { _, isChecked -> viewModel.notifSMS = isChecked }
        switchNotifCorreo.setOnCheckedChangeListener { _, isChecked -> viewModel.notifCorreo = isChecked }

        // ═══ Step 5: Add Contact ═══
        btnAddContact.setOnClickListener {
            if (viewModel.contactosEmergencia.size < 3) {
                viewModel.addContact()
                setupStep5ContactCards()
            } else {
                Toast.makeText(requireContext(), "Máximo 3 contactos de emergencia", Toast.LENGTH_SHORT).show()
            }
        }

        // ═══ Step 6: Sliders and toggles ═══
        sliderIntervalo.addOnChangeListener { _, value, _ ->
            viewModel.intervaloMonitoreo = value.toInt()
            tvIntervaloValue.text = value.toInt().toString()
        }
        radioGroupModoAlerta.setOnCheckedChangeListener { _, checkedId ->
            viewModel.modoAlerta = when (checkedId) {
                R.id.radioSonido -> "Sonido y Vibración"
                R.id.radioVibracion -> "Solo Vibración"
                R.id.radioSilencioso -> "Silencioso (solo visual)"
                else -> "Sonido y Vibración"
            }
        }
        switchAlertaPulso.setOnCheckedChangeListener { _, c -> viewModel.alertaPulso = c }
        switchAlertaCaida.setOnCheckedChangeListener { _, c -> viewModel.alertaCaida = c }
        switchAlertaInactividad.setOnCheckedChangeListener { _, c -> viewModel.alertaInactividad = c }
        switchEnviarGPS.setOnCheckedChangeListener { _, c -> viewModel.enviarGPS = c }

        // ═══ Step 7: Terms checkbox ═══
        cbTerminos.setOnCheckedChangeListener { _, isChecked -> viewModel.aceptaTerminos = isChecked }
    }

    private fun setupChipListeners(chipGroup: ChipGroup, selectedSet: MutableSet<String>) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                val text = (buttonView as Chip).text.toString()
                if (isChecked) selectedSet.add(text) else selectedSet.remove(text)
            }
        }
    }

    private fun setupStep5ContactCards() {
        // Save data from existing cards before rebuilding
        saveContactCardsData()

        contactCardsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val parentescos = arrayOf("Padre/Madre", "Hijo/Hija", "Hermano/Hermana", "Esposo/Esposa", "Abuelo/Abuela", "Tío/Tía", "Otro")

        viewModel.contactosEmergencia.forEachIndexed { index, contacto ->
            val cardView = inflater.inflate(R.layout.item_contact_card, contactCardsContainer, false)

            val tvTitle = cardView.findViewById<TextView>(R.id.tvContactTitle)
            val etNombre = cardView.findViewById<TextInputEditText>(R.id.etContactNombre)
            val etTelefono = cardView.findViewById<TextInputEditText>(R.id.etContactTelefono)
            val actvParentesco = cardView.findViewById<AutoCompleteTextView>(R.id.actvContactParentesco)
            val switchAutoCall = cardView.findViewById<SwitchMaterial>(R.id.switchAutoCall)
            val btnDelete = cardView.findViewById<ImageView>(R.id.btnDeleteContact)

            tvTitle.text = "CONTACTO ${index + 1}"
            etNombre.setText(contacto.nombre)
            etTelefono.setText(contacto.telefono)
            actvParentesco.setText(contacto.parentesco)
            switchAutoCall.isChecked = contacto.llamarAuto

            actvParentesco.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, parentescos))

            // Delete button
            if (viewModel.contactosEmergencia.size > 1) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    saveContactCardsData()
                    viewModel.removeContact(index)
                    setupStep5ContactCards()
                }
            } else {
                btnDelete.visibility = View.GONE
            }

            contactCardsContainer.addView(cardView)
        }

        // Update add button visibility
        btnAddContact.visibility = if (viewModel.contactosEmergencia.size >= 3) View.GONE else View.VISIBLE
    }

    private fun saveContactCardsData() {
        for (i in 0 until contactCardsContainer.childCount) {
            if (i >= viewModel.contactosEmergencia.size) break
            val card = contactCardsContainer.getChildAt(i)
            val etNombre = card.findViewById<TextInputEditText>(R.id.etContactNombre)
            val etTelefono = card.findViewById<TextInputEditText>(R.id.etContactTelefono)
            val actvParentesco = card.findViewById<AutoCompleteTextView>(R.id.actvContactParentesco)
            val switchAutoCall = card.findViewById<SwitchMaterial>(R.id.switchAutoCall)

            viewModel.contactosEmergencia[i].apply {
                nombre = etNombre?.text?.toString() ?: ""
                telefono = etTelefono?.text?.toString() ?: ""
                parentesco = actvParentesco?.text?.toString() ?: ""
                llamarAuto = switchAutoCall?.isChecked ?: false
            }
        }
    }

    private fun saveCurrentStepData() {
        when (viewModel.currentStep.value) {
            1 -> {
                viewModel.nombreCompleto = etNombreCompleto.text?.toString() ?: ""
                viewModel.fechaNacimiento = etFechaNacimiento.text?.toString() ?: ""
                viewModel.genero = actvGenero.text?.toString() ?: ""
                viewModel.peso = etPeso.text?.toString() ?: ""
                viewModel.altura = etAltura.text?.toString() ?: ""
                viewModel.tipoSangre = actvTipoSangre.text?.toString() ?: ""
                viewModel.direccion = etDireccion.text?.toString() ?: ""
                viewModel.telefono = etTelefono.text?.toString() ?: ""
            }
            2 -> {
                viewModel.tieneMarcapasos = switchMarcapasos.isChecked
                viewModel.frecuenciaReposo = etFrecuenciaReposo.text?.toString()?.toIntOrNull() ?: 70
                viewModel.tieneDesmayos = switchDesmayos.isChecked
                viewModel.tieneCaidas = switchCaidas.isChecked
                viewModel.tieneDesfibrilador = switchDesfibrilador.isChecked
            }
            3 -> {
                // Already handled by slider listeners
            }
            4 -> {
                viewModel.tutorNombre = etTutorNombre.text?.toString() ?: ""
                viewModel.tutorParentesco = actvParentesco.text?.toString() ?: ""
                viewModel.tutorTelefono = etTutorTelefono.text?.toString() ?: ""
                viewModel.tutorTelefonoAlt = etTutorTelefonoAlt.text?.toString() ?: ""
                viewModel.tutorCorreo = etTutorCorreo.text?.toString() ?: ""
                viewModel.tutorDireccion = etTutorDireccion.text?.toString() ?: ""
            }
            5 -> {
                saveContactCardsData()
            }
            6 -> {
                // Already handled by listeners
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStep.collectLatest { step ->
                updateUI(step)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collectLatest { state ->
                when (state) {
                    is RegistroPacienteViewModel.SaveState.Success -> {
                        Toast.makeText(requireContext(), "✅ Paciente registrado exitosamente", Toast.LENGTH_LONG).show()
                        // Navigate back or to dashboard
                        parentFragmentManager.popBackStack()
                    }
                    is RegistroPacienteViewModel.SaveState.Error -> {
                        Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    is RegistroPacienteViewModel.SaveState.Saving -> {
                        btnSiguiente.isEnabled = false
                        btnSiguiente.text = "Guardando..."
                    }
                    else -> {
                        btnSiguiente.isEnabled = true
                    }
                }
            }
        }
    }

    private fun updateUI(step: Int) {
        // Update step header
        tvStepHeader.text = stepTitles[step - 1]

        // Show/hide step layouts
        allStepLayouts.forEachIndexed { index, layout ->
            layout.visibility = if (index == step - 1) View.VISIBLE else View.GONE
        }

        // Update step indicator circles
        stepCircles.forEachIndexed { index, circle ->
            when {
                index < step - 1 -> circle.setBackgroundResource(R.drawable.bg_step_completed) // completed
                index == step - 1 -> circle.setBackgroundResource(R.drawable.bg_step_active) // current
                else -> circle.setBackgroundResource(R.drawable.bg_step_inactive) // future
            }
            // Update icon tint
            val icon = circle.getChildAt(0) as? ImageView
            icon?.setColorFilter(
                if (index <= step - 1) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#90A4AE")
            )
        }

        // Update lines
        stepLines.forEachIndexed { index, line ->
            line.setBackgroundResource(
                if (index < step - 1) R.drawable.bg_step_line_completed
                else R.drawable.bg_step_line
            )
        }

        // Navigation buttons
        btnAnterior.visibility = if (step > 1) View.VISIBLE else View.GONE
        btnSiguiente.text = if (step == 7) "Guardar Registro ✓" else "Guardar y Continuar →"

        // If step 7, populate summaries
        if (step == 7) {
            populateResumen()
        }

        // If step 5, rebuild contacts
        if (step == 5) {
            setupStep5ContactCards()
        }

        // Restore data when navigating back
        restoreStepData(step)

        // Scroll to top
        (view?.parent as? androidx.core.widget.NestedScrollView)?.smoothScrollTo(0, 0)
    }

    private fun restoreStepData(step: Int) {
        when (step) {
            1 -> {
                etNombreCompleto.setText(viewModel.nombreCompleto)
                etFechaNacimiento.setText(viewModel.fechaNacimiento)
                if (viewModel.genero.isNotBlank()) actvGenero.setText(viewModel.genero, false)
                etPeso.setText(viewModel.peso)
                etAltura.setText(viewModel.altura)
                if (viewModel.tipoSangre.isNotBlank()) actvTipoSangre.setText(viewModel.tipoSangre, false)
                etDireccion.setText(viewModel.direccion)
                etTelefono.setText(viewModel.telefono)
            }
            2 -> {
                switchMarcapasos.isChecked = viewModel.tieneMarcapasos
                etFrecuenciaReposo.setText(viewModel.frecuenciaReposo.toString())
                switchDesmayos.isChecked = viewModel.tieneDesmayos
                switchCaidas.isChecked = viewModel.tieneCaidas
                switchDesfibrilador.isChecked = viewModel.tieneDesfibrilador
            }
            3 -> {
                sliderPulsoMax.value = viewModel.pulsoMaximo.toFloat()
                sliderPulsoMin.value = viewModel.pulsoMinimo.toFloat()
                tvPulsoMaxValue.text = viewModel.pulsoMaximo.toString()
                tvPulsoMinValue.text = viewModel.pulsoMinimo.toString()
                when (viewModel.sensibilidadCaidas) {
                    "Alta" -> radioGroupSensibilidad.check(R.id.radioAlta)
                    "Media" -> radioGroupSensibilidad.check(R.id.radioMedia)
                    "Baja" -> radioGroupSensibilidad.check(R.id.radioBaja)
                }
            }
            4 -> {
                etTutorNombre.setText(viewModel.tutorNombre)
                if (viewModel.tutorParentesco.isNotBlank()) actvParentesco.setText(viewModel.tutorParentesco, false)
                etTutorTelefono.setText(viewModel.tutorTelefono)
                etTutorTelefonoAlt.setText(viewModel.tutorTelefonoAlt)
                etTutorCorreo.setText(viewModel.tutorCorreo)
                switchNotifSMS.isChecked = viewModel.notifSMS
                switchNotifCorreo.isChecked = viewModel.notifCorreo
                etTutorDireccion.setText(viewModel.tutorDireccion)
            }
            6 -> {
                when (viewModel.modoAlerta) {
                    "Sonido y Vibración" -> radioGroupModoAlerta.check(R.id.radioSonido)
                    "Solo Vibración" -> radioGroupModoAlerta.check(R.id.radioVibracion)
                    "Silencioso (solo visual)" -> radioGroupModoAlerta.check(R.id.radioSilencioso)
                }
                switchAlertaPulso.isChecked = viewModel.alertaPulso
                switchAlertaCaida.isChecked = viewModel.alertaCaida
                switchAlertaInactividad.isChecked = viewModel.alertaInactividad
                switchEnviarGPS.isChecked = viewModel.enviarGPS
                sliderIntervalo.value = viewModel.intervaloMonitoreo.toFloat()
                tvIntervaloValue.text = viewModel.intervaloMonitoreo.toString()
            }
        }
    }

    private fun populateResumen() {
        tvResumenDatos.text = viewModel.getResumenDatos()
        tvResumenMedica.text = viewModel.getResumenMedica()
        tvResumenUmbrales.text = viewModel.getResumenUmbrales()
        tvResumenTutor.text = viewModel.getResumenTutor()
        tvResumenContactos.text = viewModel.getResumenContactos()
        tvResumenConfig.text = viewModel.getResumenConfig()
    }
}
