package com.safestep.appband.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safestep.appband.R
import com.safestep.appband.databinding.ItemEventoBinding
import com.safestep.appband.models.Evento

/**
 * Adapter para la lista de incidentes / eventos de SafeBand.
 * Renderiza fielmente el diseño de referencia "Eventos - Compacto":
 * - CRÍTICO: Barra izquierda roja, badge rojo, botón relleno rojo "Ver incidente"
 * - ADVERTENCIA: Barra izquierda amarilla/ámbar, badge amarillo, botón delineado teal "Ver información"
 */
class EventosAdapter(
    private val onVerIncidente: (Evento) -> Unit = {}
) : ListAdapter<Evento, EventosAdapter.EventoViewHolder>(EventoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventoViewHolder(
        private val binding: ItemEventoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: Evento) {
            val tipoUpper = evento.tipo.uppercase()
            val tituloUpper = evento.titulo.uppercase()
            val descUpper = evento.descripcion.uppercase()

            val isCritical = tipoUpper.contains("CRITIC") ||
                    tipoUpper.contains("CRÍTICO") ||
                    tituloUpper.contains("CAÍDA") ||
                    tituloUpper.contains("CAIDA") ||
                    descUpper.contains("IMPACTO")

            val isBattery = tituloUpper.contains("BATERÍA") ||
                    tituloUpper.contains("BATERIA") ||
                    descUpper.contains("BATERÍA") ||
                    descUpper.contains("BATERIA")

            if (isCritical) {
                // Card & indicator
                binding.cardContainer.setBackgroundResource(R.drawable.bg_incident_critical)
                binding.viewIndicatorBar.setBackgroundResource(R.drawable.bg_indicator_critical)

                // Icon
                binding.frameIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                binding.ivIncidentIcon.setImageResource(R.drawable.ic_warning)
                binding.ivIncidentIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#B71C1C"))

                // Badge
                binding.tvBadge.text = "CRÍTICO"
                binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_critical)
                binding.tvBadge.setTextColor(Color.parseColor("#B71C1C"))

                // Action Button
                binding.btnIncidentAction.text = "Ver incidente"
                binding.btnIncidentAction.setBackgroundResource(R.drawable.bg_btn_critical)
                binding.btnIncidentAction.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                // Card & indicator
                binding.cardContainer.setBackgroundResource(R.drawable.bg_incident_warning)
                binding.viewIndicatorBar.setBackgroundResource(R.drawable.bg_indicator_warning)

                // Icon (battery or warning/bell)
                binding.frameIconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                if (isBattery) {
                    binding.ivIncidentIcon.setImageResource(R.drawable.ic_battery)
                } else {
                    binding.ivIncidentIcon.setImageResource(R.drawable.ic_warning)
                }
                binding.ivIncidentIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))

                // Badge
                binding.tvBadge.text = "ADVERTENCIA"
                binding.tvBadge.setBackgroundResource(R.drawable.bg_badge_warning)
                binding.tvBadge.setTextColor(Color.parseColor("#D97706"))

                // Action Button
                binding.btnIncidentAction.text = "Ver información"
                binding.btnIncidentAction.setBackgroundResource(R.drawable.bg_btn_warning)
                binding.btnIncidentAction.setTextColor(Color.parseColor("#00796B"))
            }

            // Time, Title, Description
            binding.tvTime.text = evento.tiempoTexto
            binding.tvIncidentTitle.text = evento.titulo
            binding.tvIncidentDesc.text = evento.descripcion

            binding.btnIncidentAction.setOnClickListener {
                onVerIncidente(evento)
            }
        }
    }

    class EventoDiffCallback : DiffUtil.ItemCallback<Evento>() {
        override fun areItemsTheSame(oldItem: Evento, newItem: Evento): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Evento, newItem: Evento): Boolean =
            oldItem == newItem
    }
}
