package com.fahad1005.sellerchat.util

import com.fahad1005.sellerchat.data.Country
import java.util.Currency
import java.util.Locale

/** Currency display rules (v2 spec §3B): the symbol follows the
 *  seller's selected country, and India gets Indian digit grouping
 *  (lakh / crore style). Amounts are whole units end-to-end. */
object CurrencyUtils {

    /** Spec'd symbols for the core markets; anything else falls back
     *  to the platform currency table for the country, then the ISO
     *  code itself (never a crash, never a wrong guess). */
    fun symbolFor(country: Country): String = when (country.iso) {
        "PK" -> "Rs"
        "IN" -> "₹"
        "BD" -> "৳"
        "AE" -> "AED"
        "NG" -> "₦"
        else -> try {
            Currency.getInstance(Locale("", country.iso)).getSymbol(Locale.ENGLISH)
                ?: country.iso
        } catch (e: Exception) {
            country.iso
        }
    }

    fun isIndia(country: Country): Boolean = country.iso == "IN"

    /** "12,500" normally; "12,500" / "1,25,000" Indian-style for IN. */
    fun formatAmount(amount: Long, country: Country): String {
        val raw = amount.toString()
        val negative = raw.startsWith("-")
        val digits = if (negative) raw.drop(1) else raw
        val grouped = if (isIndia(country)) groupIndian(digits) else groupWestern(digits)
        return if (negative) "-$grouped" else grouped
    }

    /** "Rs 12,500" / "₹ 1,25,000". */
    fun formatWithSymbol(amount: Long, country: Country): String =
        "${symbolFor(country)} ${formatAmount(amount, country)}"

    private fun groupWestern(digits: String): String {
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        var i = digits.length % 3
        if (i > 0) sb.append(digits, 0, i)
        while (i < digits.length) {
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(digits, i, i + 3)
            i += 3
        }
        return sb.toString()
    }

    /** Indian grouping: last 3 digits, then pairs (thousand, lakh, crore…). */
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = mutableListOf<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }
}
