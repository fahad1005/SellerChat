package com.fahad1005.sellerchat.util

import android.net.Uri
import com.fahad1005.sellerchat.data.Countries
import com.fahad1005.sellerchat.data.Country

/** Number normalization to E.164 digits (no '+'), validation, and wa.me
 *  link building. Deliberately dependency-free (no libphonenumber — size). */
object PhoneUtils {

    /** Pull the most plausible phone number out of shared text: the longest
     *  run of digits/spaces/dashes/brackets with at least 7 digits. */
    fun extractNumber(text: String): String? {
        val candidates = Regex("[+\\d][\\d\\s\\-()]{5,}\\d").findAll(text).toList()
        var best: String? = null
        var bestDigits = 0
        for (m in candidates) {
            val d = m.value.count { it.isDigit() }
            if (d in 7..15 && d > bestDigits) {
                best = m.value
                bestDigits = d
            }
        }
        return best
    }

    /**
     * Normalize [raw] into E.164 digit form using [country] as the default
     * country. Returns null when the result cannot be a real number.
     *
     * Handles: "+92 300 1234567", "0092...", "03001234567" (trunk zero),
     * "3001234567" (bare national), "923001234567" (already international).
     */
    fun normalize(raw: String, country: Country): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val hadPlus = trimmed.startsWith("+")
        var digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null

        val e164: String = when {
            hadPlus -> digits
            digits.startsWith("00") -> digits.drop(2)
            else -> {
                val existingDial = Countries.dialCodesLongestFirst.firstOrNull { d ->
                    digits.startsWith(d) && digits.length - d.length in 6..12
                }
                when {
                    existingDial != null -> digits
                    digits.startsWith("0") -> country.dial + digits.trimStart('0')
                    else -> country.dial + digits
                }
            }
        }
        return if (isValidE164(e164)) e164 else null
    }

    fun isValidE164(e164: String): Boolean {
        if (e164.length !in 8..15) return false
        if (!e164.all { it.isDigit() }) return false
        val dial = Countries.dialCodesLongestFirst.firstOrNull { e164.startsWith(it) }
            ?: return false
        val national = e164.substring(dial.length)
        return national.length in 6..12 && !national.startsWith("0")
    }

    /** Build a wa.me link. [text] empty → clean number-only link. */
    fun waMeLink(e164: String, text: String = ""): String {
        val base = "https://wa.me/$e164"
        return if (text.isBlank()) base else base + "?text=" + Uri.encode(text)
    }

    /** Replace template placeholders. {name} gets the label; {price},
     *  {item}, {qty} are blanked — raw placeholders never reach a customer. */
    fun applyTemplate(body: String, label: String): String {
        var out = body
            .replace("{name}", label)
            .replace("{price}", "")
            .replace("{item}", "")
            .replace("{qty}", "")
        out = out.replace(Regex(" {2,}"), " ").trim()
        return out
    }
}
