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
import androidx.navigation.fragment.navArgs
import com.safestep.appband.R
import com.safestep.appband.databinding.FragmentPasswordBinding
import com.safestep.appband.viewmodels.PasswordViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de Contraseña Wi-Fi.
 * Migrado desde components/password/password.component.ts
 */
class PasswordFragment : Fragment() {

    private var _binding: FragmentPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PasswordViewModel by viewModels()
    private val args: PasswordFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSsidDescription.text = "Ingresa la contraseña para\n“${args.ssid}”"

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConnect.setOnClickListener {
            val pass = binding.etPassword.text.toString()
            viewModel.conectarWifi(args.ssid, pass)
        }

        observarEstado()
    }

    private fun observarEstado() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { cargando ->
                        if (cargando) {
                            binding.containerForm.visibility = View.GONE
                            binding.containerLoader.visibility = View.VISIBLE
                        } else {
                            binding.containerForm.visibility = View.VISIBLE
                            binding.containerLoader.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.connectSuccessEvent.collect {
                        findNavController().navigate(R.id.action_password_to_nombre)
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
