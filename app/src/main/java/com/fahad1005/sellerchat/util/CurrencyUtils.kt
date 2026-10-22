package com.fahad1005.sellerchat.util

import com.fahad1005.sellerchat.data.Country
import java.util.Currency
import java.util.Locale

/** One manually selectable currency in Settings → Region & Currency. */
data class CurrencyOption(val code: String, val symbol: String)

/** Currency display rules: the v2 rules followed the seller's selected
 *  country; v2.3 keeps country detection as the one-time starting
 *  point, then a persisted ISO currency code is sovereign. India gets
 *  Indian digit grouping (lakh / crore style) whenever the effective
 *  currency is INR. Amounts are whole units end-to-end. */
object CurrencyUtils {

    /** Curated Settings list, in the founder's order. Codes whose
     *  Latin symbol would be ambiguous (AED, SAR) display as the code
     *  itself — the same convention v2 already used for AED. */
    val supportedCurrencies: List<CurrencyOption> = listOf(
        CurrencyOption("PKR", "Rs"),
        CurrencyOption("INR", "₹"),
        CurrencyOption("BDT", "৳"),
        CurrencyOption("USD", "$"),
        CurrencyOption("GBP", "£"),
        CurrencyOption("EUR", "€"),
        CurrencyOption("AED", "AED"),
        CurrencyOption("SAR", "SAR"),
        CurrencyOption("NGN", "₦")
    )

    /** ISO 4217 code for [country]. The hand table wins for the core
     *  markets; every other country is resolved through the platform
     *  table from an ISO-country Locale. Any failure falls back to
     *  USD — detection must never crash first launch. */
    fun codeForCountry(country: Country): String = when (country.iso) {
        "PK" -> "PKR"
        "IN" -> "INR"
        "BD" -> "BDT"
        "NG" -> "NGN"
        "AE" -> "AED"
        else -> try {
            Currency.getInstance(Locale("", country.iso)).currencyCode ?: "USD"
        } catch (e: Exception) {
            "USD"
        }
    }

    /** Display symbol for an ISO [code]: the curated list first, then
     *  the platform table, then the code itself as the last resort. */
    fun symbolForCode(code: String): String {
        val normalized = code.uppercase()
        return supportedCurrencies.firstOrNull { it.code == normalized }?.symbol
            ?: try {
                Currency.getInstance(normalized).getSymbol(Locale.ENGLISH) ?: normalized
            } catch (e: Exception) {
                normalized
            }
    }

    /** Country-derived symbol (kept for the country-only call sites):
     *  the hand-table code and symbol now flow through the same two
     *  functions the manual currency setting uses. */
    fun symbolFor(country: Country): String = symbolForCode(codeForCountry(country))

    fun isIndia(country: Country): Boolean = country.iso == "IN"

    /** "12,500" normally; "12,500" / "1,25,000" Indian-style for INR. */
    fun formatAmount(amount: Long, currencyCode: String): String {
        val raw = amount.toString()
        val negative = raw.startsWith("-")
        val digits = if (negative) raw.drop(1) else raw
        val grouped = if (currencyCode.uppercase() == "INR") {
            groupIndian(digits)
        } else {
            groupWestern(digits)
        }
        return if (negative) "-$grouped" else grouped
    }

    /** Country overload retained: grouping now follows the country's
     *  currency code, so IN still groups Indian-style. */
    fun formatAmount(amount: Long, country: Country): String =
        formatAmount(amount, codeForCountry(country))

    /** "Rs 12,500" / "₹ 1,25,000" for the configured currency code. */
    fun formatWithSymbol(amount: Long, currencyCode: String): String =
        "${symbolForCode(currencyCode)} ${formatAmount(amount, currencyCode)}"

    /** "Rs 12,500" / "₹ 1,25,000" for a country's currency. */
    fun formatWithSymbol(amount: Long, country: Country): String =
        formatWithSymbol(amount, codeForCountry(country))

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
