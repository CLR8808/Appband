package com.safestep.appband.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.activities.MainActivity
import com.safestep.appband.databinding.FragmentLoginBinding
import com.safestep.appband.viewmodels.LoginViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Inicio de Sesión.
 * Migrado desde components/iniciar-sesion/iniciar-sesion.component.ts
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private var mostrarContrasena = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarTextoRegistro()
        configurarListeners()
        observarEstado()
    }

    private fun configurarListeners() {
        // Toggle Mostrar/Ocultar contraseña
        binding.ivTogglePassword.setOnClickListener {
            mostrarContrasena = !mostrarContrasena
            if (mostrarContrasena) {
                binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
        }

        // Botón Ingresar
        binding.btnLogin.setOnClickListener {
            val correo = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            viewModel.manejarInicioSesion(correo, pass)
        }

        // Enlace Registrate Aquí
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    private fun configurarTextoRegistro() {
        val textoBase = getString(R.string.no_tienes_cuenta)
        val textoLink = getString(R.string.registrate_aqui)
        val spannable = SpannableString(textoBase + textoLink)

        val tealColor = ContextCompat.getColor(requireContext(), R.color.primary_teal)
        val startIndex = textoBase.length

        spannable.setSpan(
            ForegroundColorSpan(tealColor),
            startIndex,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(android.graphics.Typeface.BOLD),
            startIndex,
            spannable.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvRegister.text = spannable
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { cargando ->
                        binding.btnLogin.isEnabled = !cargando
                        binding.btnLogin.text = if (cargando) {
                            getString(R.string.iniciando_sesion)
                        } else {
                            getString(R.string.iniciar_sesion)
                        }
                    }
                }

                launch {
                    viewModel.loginSuccessEvent.collect {
                        val intent = Intent(requireActivity(), MainActivity::class.java)
                        startActivity(intent)
                        requireActivity().finish()
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
