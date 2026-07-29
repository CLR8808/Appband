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
import androidx.recyclerview.widget.LinearLayoutManager
import com.safestep.appband.R
import com.safestep.appband.adapters.DispositivosAdapter
import com.safestep.appband.databinding.FragmentInicioBinding
import com.safestep.appband.viewmodels.InicioViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Inicio (Lista de Dispositivos).
 * Migrado desde components/inicio/inicio.component.ts
 */
class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InicioViewModel by viewModels()
    private lateinit var adapter: DispositivosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        configurarListeners()
        observarEstado()
    }

    private fun configurarRecyclerView() {
        adapter = DispositivosAdapter { _ ->
            findNavController().navigate(R.id.action_inicio_to_dispositivo)
        }
        binding.rvDispositivos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDispositivos.adapter = adapter
    }

    private fun configurarListeners() {
        binding.cardAddDevice.setOnClickListener {
            findNavController().navigate(R.id.action_inicio_to_prepararDispositivo)
        }
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dispositivos.collect { lista ->
                    if (lista.isEmpty()) {
                        binding.containerEmptyState.visibility = View.VISIBLE
                        binding.containerListSection.visibility = View.GONE
                    } else {
                        binding.containerEmptyState.visibility = View.GONE
                        binding.containerListSection.visibility = View.VISIBLE
                        adapter.submitList(lista)
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
