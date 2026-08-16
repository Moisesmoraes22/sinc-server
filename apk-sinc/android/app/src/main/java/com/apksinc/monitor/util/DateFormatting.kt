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
