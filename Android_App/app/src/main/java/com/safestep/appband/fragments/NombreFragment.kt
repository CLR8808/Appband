package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentNombreBinding
import com.safestep.appband.viewmodels.NombreViewModel
import kotlinx.coroutines.launch

/**
 * Fragment para asignar Nombre al Dispositivo.
 * Migrado desde components/nombre/nombre.component.ts
 */
class NombreFragment : Fragment() {

    private var _binding: FragmentNombreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NombreViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNombreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            val nombre = binding.etDeviceName.text.toString()
            viewModel.guardarDispositivo(nombre)
        }

        observarEstado()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveSuccessEvent.collect {
                    findNavController().navigate(R.id.action_nombre_to_anadida)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
