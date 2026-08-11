package com.safestep.appband.fragments

import android.content.res.ColorStateList
import android.graphics.Color
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
 * Muestra las pestañas de filtro para alternar entre incidentes "Nuevos" y "Revisados".
 * Al presionar "Marcar como revisado" en un incidente, este se mueve automáticamente a la pestaña de Revisados.
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
        binding.btnFiltroNuevos.setOnClickListener {
            viewModel.setFiltroRevisados(false)
        }

        binding.btnFiltroRevisados.setOnClickListener {
            viewModel.setFiltroRevisados(true)
        }

        binding.btnCargarAnteriores.setOnClickListener {
            viewModel.cargarIncidentesAnteriores()
        }
    }

    private fun observeViewModel() {
        // Observar conteo de incidentes nuevos para mostrar el badge en el botón
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.conteoNuevos.collectLatest { conteo ->
                binding.btnFiltroNuevos.text = if (conteo > 0) "Nuevos ($conteo)" else "Nuevos"
            }
        }

        // Observar filtro activo (Nuevos vs Revisados) para actualizar estilo visual de las pestañas
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filtroMostrarRevisados.collectLatest { mostrarRevisados ->
                val primaryColor = Color.parseColor("#00796B")
                val textMutedColor = Color.parseColor("#64748B")

                if (mostrarRevisados) {
                    // Pestaña 'Revisados' Activa
                    binding.btnFiltroRevisados.backgroundTintList = ColorStateList.valueOf(primaryColor)
                    binding.btnFiltroRevisados.setTextColor(Color.WHITE)

                    binding.btnFiltroNuevos.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    binding.btnFiltroNuevos.setTextColor(textMutedColor)
                } else {
                    // Pestaña 'Nuevos' Activa
                    binding.btnFiltroNuevos.backgroundTintList = ColorStateList.valueOf(primaryColor)
                    binding.btnFiltroNuevos.setTextColor(Color.WHITE)

                    binding.btnFiltroRevisados.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                    binding.btnFiltroRevisados.setTextColor(textMutedColor)
                }
            }
        }

        // Observar lista de eventos filtrados
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventosFiltrados.collectLatest { eventos ->
                if (eventos.isEmpty()) {
                    // Estado vacío
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.rvEventos.visibility = View.GONE
                    binding.btnCargarAnteriores.visibility = View.GONE
                } else {
                    // Hay incidentes registrados
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
