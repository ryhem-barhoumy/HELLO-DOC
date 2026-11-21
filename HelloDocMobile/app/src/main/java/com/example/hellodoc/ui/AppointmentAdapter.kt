package com.example.hellodoc.ui

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.example.hellodoc.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.hellodoc.databinding.ItemAppointmentBinding
import com.example.hellodoc.model.Appointment

class AppointmentAdapter(private val onEdit: (Appointment) -> Unit,
                         private val onCancel: (Appointment) -> Unit) : RecyclerView.Adapter<AppointmentAdapter.Holder>() {
    private var items: List<Appointment> = emptyList()
    fun submit(data: List<Appointment>) { items = data; notifyDataSetChanged() }
    inner class Holder(private val b: ItemAppointmentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Appointment) {
            b.tvDoctor.text = "Dr ${a.doctorName.orEmpty()}"
            b.tvService.text = a.serviceName + (a.servicePrice?.let { "  ·  $it" } ?: "")
            b.tvDate.text = formatInstant(a.startsAt)
            b.tvStatus.text = statusLabel(a.status)
            // Le statut reste visible même pour les rendez-vous annulés/refusés.
            val isCancelled = a.status in setOf("CANCELLED", "REJECTED", "NO_SHOW")
            val isPending = a.status in setOf("PENDING", "TO_RESCHEDULE")
            val background = when {
                isCancelled -> R.color.hd_error_light
                isPending -> R.color.hd_warning_light
                else -> R.color.hd_mint
            }
            val foreground = when {
                isCancelled -> R.color.hd_error
                isPending -> R.color.hd_warning
                else -> R.color.hd_green
            }
            b.tvStatus.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(b.root.context, background)
            )
            b.tvStatus.setTextColor(ContextCompat.getColor(b.root.context, foreground))
            b.tvPlace.text = a.location?.ifBlank { "Cabinet médical" } ?: "Cabinet médical"
            val active = a.status in setOf("PENDING", "CONFIRMED", "TO_RESCHEDULE")
            b.btnEdit.isVisible = active
            b.btnCancel.isVisible = active
            b.btnEdit.setOnClickListener { onEdit(a) }
            b.btnCancel.setOnClickListener { onCancel(a) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
}
