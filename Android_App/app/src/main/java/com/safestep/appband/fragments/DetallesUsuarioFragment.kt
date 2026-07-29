package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safestep.appband.databinding.FragmentDetallesUsuarioBinding
import com.safestep.appband.viewmodels.DetallesUsuarioViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Detalles de Usuario (Vista y Edición de Perfil).
 * Migrado desde components/detalles-usuario/detalles-usuario.component.ts
 */
class DetallesUsuarioFragment : Fragment() {

    private var _binding: FragmentDetallesUsuarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetallesUsuarioViewModel by viewModels()
    private var editando = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetallesUsuarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditToggle.setOnClickListener {
            toggleEdicion(!editando)
        }

        binding.btnSave.setOnClickListener {
            val nombre = binding.etNombreInput.text.toString()
            val telefono = binding.etTelefonoInput.text.toString()
            viewModel.guardarCambios(nombre, telefono, "", "")
        }

        observarEstado()
    }

    private fun toggleEdicion(activar: Boolean) {
        editando = activar
        if (editando) {
            binding.btnEditToggle.text = "Cancelar"
            binding.tvNombreValue.visibility = View.GONE
            binding.etNombreInput.visibility = View.VISIBLE
            binding.etNombreInput.setText(binding.tvNombreValue.text)

            binding.tvTelefonoValue.visibility = View.GONE
            binding.etTelefonoInput.visibility = View.VISIBLE

            binding.btnSave.visibility = View.VISIBLE
        } else {
            binding.btnEditToggle.text = "Editar"
            binding.tvNombreValue.visibility = View.VISIBLE
            binding.etNombreInput.visibility = View.GONE

            binding.tvTelefonoValue.visibility = View.VISIBLE
            binding.etTelefonoInput.visibility = View.GONE

            binding.btnSave.visibility = View.GONE
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.perfil.collect { perfil ->
                        if (perfil != null) {
                            binding.tvHeaderName.text = perfil.nombre.ifEmpty { "Usuario SafeStep" }
                            binding.tvHeaderEmail.text = perfil.correo.ifEmpty { "Sin correo" }

                            binding.tvNombreValue.text = perfil.nombre.ifEmpty { "Sin especificar" }
                            binding.tvCorreoValue.text = perfil.correo.ifEmpty { "Sin correo" }

                            val cleanTel = perfil.telefono.replace("\\D".toRegex(), "")
                            binding.tvTelefonoValue.text = if (cleanTel.length == 10) {
                                "(${cleanTel.substring(0, 3)}) ${cleanTel.substring(3, 6)}-${cleanTel.substring(6)}"
                            } else {
                                perfil.telefono.ifEmpty { "Sin especificar" }
                            }
                            binding.etTelefonoInput.setText(cleanTel)
                        }
                    }
                }

                launch {
                    viewModel.saveSuccessEvent.collect {
                        toggleEdicion(false)
                        Toast.makeText(requireContext(), "Cambios guardados exitosamente.", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
