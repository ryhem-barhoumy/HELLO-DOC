package com.example.hellodoc.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val businessZone = ZoneId.of("Africa/Tunis")
private val format = DateTimeFormatter.ofPattern("dd MMM yyyy  ·  HH:mm", Locale.FRENCH)
private val clockFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
fun formatInstant(iso: String?): String = try {
    if (iso.isNullOrBlank()) "Non définie" else Instant.parse(iso).atZone(businessZone).format(format)
} catch (_: Exception) { iso ?: "Non définie" }
fun formatClock(iso: String): String = try {
    Instant.parse(iso).atZone(businessZone).format(clockFormat)
} catch (_: Exception) { iso }
fun Context.toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
fun showDatePicker(context: Context, onDate: (String) -> Unit) {
    val today = LocalDate.now(businessZone)
    val dlg = DatePickerDialog(context, { _, year, month, day ->
        onDate(LocalDate.of(year, month + 1, day).toString())
    }, today.year, today.monthValue - 1, today.dayOfMonth)
    dlg.datePicker.minDate = today.atStartOfDay(businessZone).toInstant().toEpochMilli()
    dlg.datePicker.maxDate = today.plusDays(60).atTime(23, 59).atZone(businessZone).toInstant().toEpochMilli()
    dlg.show()
}
fun statusLabel(status: String) = when (status) {
    "PENDING" -> "En attente"
    "CONFIRMED" -> "Confirmé"
    "TO_RESCHEDULE" -> "À reprogrammer"
    "COMPLETED" -> "Terminé"
    "REJECTED" -> "Refusé"
    "CANCELLED" -> "Annulé"
    "NO_SHOW" -> "Absent"
    else -> status
}
