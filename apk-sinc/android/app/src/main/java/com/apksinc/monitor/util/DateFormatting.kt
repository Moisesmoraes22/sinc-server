package com.apksinc.monitor.util

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun parseIsoFlexible(iso: String): Instant? = try {
    OffsetDateTime.parse(iso).toInstant()
} catch (e: DateTimeParseException) {
    try {
        Instant.parse(if (iso.endsWith("Z")) iso else "${iso}Z")
    } catch (e2: DateTimeParseException) {
        null
    }
}

fun formatTimeOnly(iso: String?): String {
    if (iso == null) return "--"
    val instant = parseIsoFlexible(iso) ?: return "--"
    return timeFormatter.withZone(ZoneId.systemDefault()).format(instant)
}

/** Minutos decorridos em forma curta para destaque numérico (ex.: "41 min", "2h 5min"). */
fun formatElapsedMinutesShort(minutes: Double?): String {
    if (minutes == null || minutes < 0) return "--"
    val totalMinutes = minutes.toLong()
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val remainder = totalMinutes % 60
    return if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}min"
}

fun formatDuration(iso: String?): String {
    if (iso == null) return "--"
    val instant = parseIsoFlexible(iso) ?: return "--"
    val duration = Duration.between(instant, Instant.now())
    val minutes = duration.toMinutes()
    return when {
        minutes < 1 -> "menos de 1 minuto"
        minutes < 60 -> "$minutes minuto${if (minutes > 1) "s" else ""}"
        minutes < 60 * 24 -> {
            val hours = minutes / 60
            "$hours hora${if (hours > 1) "s" else ""}"
        }
        else -> {
            val days = minutes / (60 * 24)
            "$days dia${if (days > 1) "s" else ""}"
        }
    }
}
