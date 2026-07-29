package com.safestep.appband.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.safestep.appband.activities.MainActivity
import com.safestep.appband.databinding.FragmentVerifyCodeBinding
import com.safestep.appband.viewmodels.VerifyCodeViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Verificación de Código SMS / Alerta.
 * Migrado desde components/verificar-codigo/verificar-codigo.component.ts
 */
class VerifyCodeFragment : Fragment() {

    private var _binding: FragmentVerifyCodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VerifyCodeViewModel by viewModels()
    private val args: VerifyCodeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val telFormateado = viewModel.formatearTelefono(args.telefono)
        binding.tvDescription.text = "Ingresa el código enviado a\n$telFormateado"

        configurarInputsCodigo()
        configurarListeners()
        observarEstado()
    }

    private fun configurarListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnVerify.setOnClickListener {
            val c1 = binding.etCode1.text.toString()
            val c2 = binding.etCode2.text.toString()
            val c3 = binding.etCode3.text.toString()
            val c4 = binding.etCode4.text.toString()
            viewModel.verificar(c1, c2, c3, c4)
        }
    }

    private fun configurarInputsCodigo() {
        setupCodeWatcher(binding.etCode1, null, binding.etCode2)
        setupCodeWatcher(binding.etCode2, binding.etCode1, binding.etCode3)
        setupCodeWatcher(binding.etCode3, binding.etCode2, binding.etCode4)
        setupCodeWatcher(binding.etCode4, binding.etCode3, null)
    }

    private fun setupCodeWatcher(current: EditText, previous: EditText?, next: EditText?) {
        current.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) {
                    next?.requestFocus()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        current.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                if (current.text.isEmpty()) {
                    previous?.requestFocus()
                    previous?.setSelection(previous.text.length)
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.simulatedCode.collect { codigo ->
                        if (codigo.isNotEmpty()) {
                            val telStr = viewModel.formatearTelefono(args.telefono)
                            AlertDialog.Builder(requireContext())
                                .setTitle("Código de Verificación")
                                .setMessage("Código enviado al número $telStr.\n\nCódigo: $codigo")
                                .setPositiveButton("Aceptar", null)
                                .show()
                        }
                    }
                }

                launch {
                    viewModel.verifySuccessEvent.collect {
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
