package com.safestep.appband.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentRegisterBinding
import com.safestep.appband.viewmodels.RegisterViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * Fragment de Creación de Cuenta (Registro de usuario).
 * Migrado desde components/crear-cuenta/crear-cuenta.component.ts
 */
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()
    private var mostrarContrasena = false
    private var fechaNacimientoIso = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarSpinnerGenero()
        configurarListeners()
        observarEstado()
    }

    private fun configurarSpinnerGenero() {
        val generos = listOf("Género", "Masculino", "Femenino", "Otro")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, generos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spGenero.adapter = adapter
    }

    private fun configurarListeners() {
        // Botón Atrás
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Validación visual de email en tiempo real
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val emailStr = s.toString().trim()
                if (emailStr.contains("@") && emailStr.lowercase(Locale.ROOT).contains(".com")) {
                    binding.ivEmailValid.visibility = View.VISIBLE
                } else {
                    binding.ivEmailValid.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Seleccionar Fecha de Nacimiento con DatePicker
        binding.containerFechaNacimiento.setOnClickListener {
            mostrarDatePicker()
        }

        // Toggle Password visibility
        binding.ivTogglePassword.setOnClickListener {
            toggleMostrarContrasena()
        }
        binding.switchShowPassword.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != mostrarContrasena) {
                toggleMostrarContrasena()
            }
        }

        // Botón Crear Cuenta
        binding.btnCreateAccount.setOnClickListener {
            val nombre = binding.etNombre.text.toString()
            val telefono = binding.etTelefono.text.toString()
            val email = binding.etEmail.text.toString()
            val posGenero = binding.spGenero.selectedItemPosition
            val generoStr = if (posGenero > 0) binding.spGenero.selectedItem.toString() else ""
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            viewModel.validarYRegistrar(
                nombre,
                telefono,
                email,
                generoStr,
                fechaNacimientoIso,
                pass,
                confirmPass
            )
        }

        // Ir a Login
        binding.tvLogin.setOnClickListener {
            findNavController().popBackStack()
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
                binding.tvFechaNacimiento.text = "$dayStr/$monthStr/$year"
            },
            calendar.get(Calendar.YEAR) - 20,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun toggleMostrarContrasena() {
        mostrarContrasena = !mostrarContrasena
        binding.switchShowPassword.isChecked = mostrarContrasena

        if (mostrarContrasena) {
            binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.etConfirmPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
        } else {
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.ivTogglePassword.setImageResource(R.drawable.ic_eye)
        }

        binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text?.length ?: 0)
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { cargando ->
                        binding.btnCreateAccount.isEnabled = !cargando
                        binding.btnCreateAccount.text = if (cargando) {
                            getString(R.string.creando_cuenta)
                        } else {
                            getString(R.string.crear_cuenta)
                        }
                    }
                }

                launch {
                    viewModel.registerSuccessEvent.collect { telefono ->
                        val action = RegisterFragmentDirections.actionRegisterToVerifyCode(telefono)
                        findNavController().navigate(action)
                    }
                }

                launch {
                    viewModel.errorMessageEvent.collect { mensaje ->
                        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
