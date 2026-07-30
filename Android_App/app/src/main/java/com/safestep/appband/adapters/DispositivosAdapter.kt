package com.safestep.appband.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.safestep.appband.databinding.ItemDispositivoBinding
import com.safestep.appband.models.Dispositivo

/**
 * Adapter RecyclerView para la lista de dispositivos guardados.
 * Migrado desde componentes de inicio.component.html
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
            binding.root.setOnClickListener {
                onItemClick(dispositivo)
            }
        }
    }
}
