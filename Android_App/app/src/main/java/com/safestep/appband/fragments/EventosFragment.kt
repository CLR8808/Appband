package com.safestep.appband.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.safestep.appband.R
import com.safestep.appband.adapters.EventosAdapter
import com.safestep.appband.databinding.FragmentEventosBinding
import com.safestep.appband.viewmodels.EventosViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment de Historial de Incidentes.
 * Muestra los incidentes registrados en la base de datos de forma dinámica.
 * Al presionar sobre un incidente, envía la información detallada a DetalleIncidenteFragment.
 */
class EventosFragment : Fragment() {

    private var _binding: FragmentEventosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventosViewModel by viewModels()
    private lateinit var eventosAdapter: EventosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        eventosAdapter = EventosAdapter { evento ->
            val bundle = Bundle().apply {
                putSerializable("evento", evento)
            }
            findNavController().navigate(R.id.action_eventos_to_detalleIncidente, bundle)
        }
        binding.rvEventos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventosAdapter
        }
    }

    private fun setupListeners() {
        binding.btnCargarAnteriores.setOnClickListener {
            viewModel.cargarIncidentesAnteriores()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventos.collectLatest { eventos ->
                if (eventos.isEmpty()) {
                    // Estado vacío
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.rvEventos.visibility = View.GONE
                    binding.btnCargarAnteriores.visibility = View.GONE
                } else {
                    // Hay incidentes registrados en la BD
                    binding.emptyStateContainer.visibility = View.GONE
                    binding.rvEventos.visibility = View.VISIBLE
                    binding.btnCargarAnteriores.visibility = View.VISIBLE
                    eventosAdapter.submitList(eventos)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
