package com.example.hellodoc.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.hellodoc.databinding.ItemDoctorBinding
import com.example.hellodoc.model.Doctor

class DoctorAdapter(private val onClick: (Doctor) -> Unit) : RecyclerView.Adapter<DoctorAdapter.Holder>() {
    private var items: List<Doctor> = emptyList()
    fun submit(data: List<Doctor>) { items = data; notifyDataSetChanged() }
    inner class Holder(private val b: ItemDoctorBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(d: Doctor) {
            b.tvName.text = "Dr ${d.displayName()}"
            b.tvSpecialty.text = d.specialty ?: "Médecin"
            b.tvCity.text = listOfNotNull(d.city?.takeIf { it.isNotBlank() },
                d.location?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { "Localisation non renseignée" }
            b.tvNext.text = d.nextAvailability?.let { "Prochain créneau : ${formatInstant(it)}" }
                ?: "Consulter les disponibilités"
            b.root.setOnClickListener { onClick(d) }
            b.btnDetails.setOnClickListener { onClick(d) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        ItemDoctorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
}
