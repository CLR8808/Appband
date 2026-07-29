package com.safestep.appband.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.safestep.appband.R
import com.safestep.appband.databinding.ItemWifiNetworkBinding
import com.safestep.appband.models.SignalLevel
import com.safestep.appband.models.WifiNetwork

/**
 * Adapter RecyclerView para la lista de redes Wi-Fi escaneadas.
 * Migrado desde componentes de wifi.component.html
 */
class WifiAdapter(
    private var redes: List<WifiNetwork> = emptyList(),
    private val onItemClick: (WifiNetwork) -> Unit
) : RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

    fun submitList(nuevaLista: List<WifiNetwork>) {
        redes = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val binding = ItemWifiNetworkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WifiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WifiViewHolder, position: Int) {
        holder.bind(redes[position])
    }

    override fun getItemCount(): Int = redes.size

    inner class WifiViewHolder(private val binding: ItemWifiNetworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(net: WifiNetwork) {
            binding.tvSsid.text = net.ssid
            binding.tvFrequency.text = net.frequency
            binding.tvSecurity.text = "🔒 ${net.security}"
            binding.tvTagCurrent.visibility = if (net.isCurrent) View.VISIBLE else View.GONE

            val iconRes = when (net.signalLevel) {
                SignalLevel.EXCELENTE, SignalLevel.BUENA -> R.drawable.ic_mail
                else -> R.drawable.ic_mail
            }
            binding.ivSignal.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onItemClick(net)
            }
        }
    }
}
