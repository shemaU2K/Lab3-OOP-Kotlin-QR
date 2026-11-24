package com.qrvault.scanner.scanner

import android.net.Uri

data class ClassifiedQr(
    val type: String,
    val title: String
)

object QrPayloadClassifier {

    fun classify(rawValue: String): ClassifiedQr {
        val trimmed = rawValue.trim()
        return when {
            trimmed.startsWith("WIFI:", ignoreCase = true) -> {
                val ssid = Regex("(?:^|;)S:([^;]+)").find(trimmed.removePrefix("WIFI:"))?.groupValues?.get(1)
                ClassifiedQr("WIFI", ssid ?: "Wi-Fi Network")
            }
            trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                val fn = Regex("^FN(?:;[^:]*)?:(.*)$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
                    .find(trimmed)?.groupValues?.get(1)?.trim()
                ClassifiedQr("CONTACT", fn ?: "vCard Contact")
            }
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                val host = Uri.parse(trimmed).host ?: trimmed
                ClassifiedQr("URL", host.removePrefix("www."))
            }
            trimmed.startsWith("mailto:", ignoreCase = true) -> {
                ClassifiedQr("EMAIL", trimmed.removePrefix("mailto:").substringBefore("?"))
            }
            trimmed.startsWith("tel:", ignoreCase = true) -> {
                ClassifiedQr("PHONE", trimmed.removePrefix("tel:"))
            }
            trimmed.startsWith("geo:", ignoreCase = true) -> {
                ClassifiedQr("GEO", trimmed.removePrefix("geo:").substringBefore("?"))
            }
            else -> {
                val preview = trimmed.lineSequence().firstOrNull().orEmpty().take(48)
                ClassifiedQr("TEXT", preview.ifBlank { "Text QR" })
            }
        }
    }
}
