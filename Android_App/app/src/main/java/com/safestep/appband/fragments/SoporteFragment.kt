package com.safestep.appband.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safestep.appband.databinding.FragmentSoporteBinding
import com.safestep.appband.viewmodels.SoporteViewModel

/**
 * Fragment de Contacto con Soporte Técnico.
 * Migrado desde components/soporte/soporte.component.ts
 */
class SoporteFragment : Fragment() {

    private var _binding: FragmentSoporteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SoporteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoporteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnContactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:soporte@safeband.com")
                putExtra(Intent.EXTRA_SUBJECT, "Soporte SafeBand")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
