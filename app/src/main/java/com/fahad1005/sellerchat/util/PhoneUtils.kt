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

    /** Outcome of strict per-country validation (v2.4 task 1). */
    sealed interface NumberValidation {
        /** Safe to send: [waNumber] is the normalized E.164 digit
         *  string (dial code + NSN, no '+'). What was validated here
         *  is exactly what the send paths hand to WhatsApp. */
        data class Valid(val waNumber: String) : NumberValidation

        /** Non-empty input that cannot be a number for the selected
         *  country. [reason] is a developer-facing summary; Home
         *  renders its own fixed sentence with the selected dial code. */
        data class Invalid(val reason: String) : NumberValidation

        /** Nothing typed yet — no error is shown and the actions sit
         *  in their normal empty state. */
        data object Empty : NumberValidation
    }

    /**
     * Strict validation of a typed number against ONE selected
     * [country] (v2.4 task 1). Unlike [normalize] — which guesses the
     * country from the digits — this treats the dropdown as the
     * truth: a number that cannot belong to the selected country is
     * Invalid, so a Pakistani "0300…" typed with +1 selected is caught
     * on Home instead of opening the wrong chat in WhatsApp.
     *
     * Rules, in order:
     *  1. Digits only; a leading "00" (international prefix) is
     *     dropped. Nothing left → [NumberValidation.Empty].
     *  2. Digits starting with the country's dial code AND leaving an
     *     NSN of an allowed length → Valid, exactly as typed.
     *  3. National form: a leading '0' is the trunk — stripped
     *     (exactly one) for every country EXCEPT +1 (NANPA), where a
     *     0-leading string is never a valid national number and is
     *     simply measured unstripped. Valid iff the resulting length
     *     is an allowed NSN length; the normalized form is dial + NSN.
     *  4. Anything else → Invalid.
     */
    fun validateForCountry(rawInput: String, country: Country): NumberValidation {
        var digits = rawInput.filter { it.isDigit() }
        if (digits.startsWith("00")) digits = digits.drop(2)
        if (digits.isEmpty()) return NumberValidation.Empty

        val nsn = country.nsnLengths
        val dial = country.dial

        // Rule 2 — already in international form for THIS country.
        if (digits.startsWith(dial) && (digits.length - dial.length) in nsn) {
            return NumberValidation.Valid(digits)
        }

        // Rule 3 — national form, with the NANPA trunk exception.
        val national = if (digits.startsWith("0") && dial != "1") {
            digits.drop(1)
        } else {
            digits
        }
        return if (national.length in nsn) {
            NumberValidation.Valid(dial + national)
        } else {
            val lo = nsn.minOrNull() ?: 0
            val hi = nsn.maxOrNull() ?: 0
            val expected = if (lo == hi) "$lo-digit" else "$lo–$hi-digit"
            NumberValidation.Invalid(
                "Expected a $expected number for ${country.name} (+$dial)"
            )
        }
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

    // Template placeholder filling lives in MainViewModel.filledTemplateText
    // (v2.1): ONE shared function behind the Home preview and every send,
    // so this file deliberately has no second fill implementation.
}
