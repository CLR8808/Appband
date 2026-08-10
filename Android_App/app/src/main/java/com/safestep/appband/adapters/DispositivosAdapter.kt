package com.safestep.appband.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.safestep.appband.R
import com.safestep.appband.databinding.ItemDispositivoBinding
import com.safestep.appband.models.Dispositivo

/**
 * Adapter RecyclerView para la lista de dispositivos guardados.
 * Renderiza el diseño estilizado de SafeBand según Figma.
 */
class DispositivosAdapter(
    private var dispositivos: List<Dispositivo> = emptyList(),
    private val onItemClick: (Dispositivo) -> Unit
) : RecyclerView.Adapter<DispositivosAdapter.DispositivoViewHolder>() {

    fun submitList(nuevaLista: List<Dispositivo>) {
        dispositivos = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DispositivoViewHolder {
        val binding = ItemDispositivoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DispositivoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DispositivoViewHolder, position: Int) {
        holder.bind(dispositivos[position])
    }

    override fun getItemCount(): Int = dispositivos.size

    inner class DispositivoViewHolder(private val binding: ItemDispositivoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(dispositivo: Dispositivo) {
            binding.tvDeviceName.text = dispositivo.nombre.ifBlank { "SafeBand" }

            if (dispositivo.incidenteDetectado) {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_help_circle)
                binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                binding.tvStatusMessage.text = "Incidente detectado"
                binding.tvStatusMessage.setTextColor(Color.parseColor("#DC2626"))
            } else {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_checkmark_circle)
                binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                binding.tvStatusMessage.text = dispositivo.mensajeEstado.ifBlank { "Todo está tranquilo" }
                binding.tvStatusMessage.setTextColor(Color.parseColor("#10B981"))
            }

            binding.root.setOnClickListener {
                onItemClick(dispositivo)
            }
        }
    }
}
