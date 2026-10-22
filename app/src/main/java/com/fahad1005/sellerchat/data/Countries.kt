package com.fahad1005.sellerchat.data

/** A country entry for the dial-code picker. The flag is computed from the
 *  ISO code as regional-indicator symbols, so no emoji/image assets ship. */
data class Country(
    val iso: String,
    val name: String,
    val dial: String
) {
    val flag: String
        get() {
            val sb = StringBuilder()
            for (c in iso.uppercase()) {
                if (c in 'A'..'Z') sb.appendCodePoint(0x1F1E6 + (c - 'A'))
            }
            return sb.toString()
        }

    /** Allowed lengths of the national significant number (NSN): the
     *  digit count that must remain AFTER the dial code is removed and
     *  any trunk '0' is stripped. Drives the strict per-country
     *  validation on Home (v2.4 task 1). Data lives in [Countries]. */
    val nsnLengths: Set<Int>
        get() = Countries.nsnLengthsFor(iso)
}

object Countries {
    val all: List<Country> = listOf(
        Country("AF", "Afghanistan", "93"),
        Country("AL", "Albania", "355"),
        Country("DZ", "Algeria", "213"),
        Country("AO", "Angola", "244"),
        Country("AR", "Argentina", "54"),
        Country("AM", "Armenia", "374"),
        Country("AU", "Australia", "61"),
        Country("AT", "Austria", "43"),
        Country("AZ", "Azerbaijan", "994"),
        Country("BH", "Bahrain", "973"),
        Country("BD", "Bangladesh", "880"),
        Country("BY", "Belarus", "375"),
        Country("BE", "Belgium", "32"),
        Country("BJ", "Benin", "229"),
        Country("BO", "Bolivia", "591"),
        Country("BA", "Bosnia and Herzegovina", "387"),
        Country("BW", "Botswana", "267"),
        Country("BR", "Brazil", "55"),
        Country("BG", "Bulgaria", "359"),
        Country("BF", "Burkina Faso", "226"),
        Country("KH", "Cambodia", "855"),
        Country("CM", "Cameroon", "237"),
        Country("CA", "Canada", "1"),
        Country("CL", "Chile", "56"),
        Country("CN", "China", "86"),
        Country("CO", "Colombia", "57"),
        Country("CD", "Congo (DRC)", "243"),
        Country("CG", "Congo (Republic)", "242"),
        Country("CR", "Costa Rica", "506"),
        Country("CI", "Côte d'Ivoire", "225"),
        Country("HR", "Croatia", "385"),
        Country("CU", "Cuba", "53"),
        Country("CY", "Cyprus", "357"),
        Country("CZ", "Czechia", "420"),
        Country("DK", "Denmark", "45"),
        Country("DJ", "Djibouti", "253"),
        Country("DO", "Dominican Republic", "1"),
        Country("EC", "Ecuador", "593"),
        Country("EG", "Egypt", "20"),
        Country("SV", "El Salvador", "503"),
        Country("ER", "Eritrea", "291"),
        Country("EE", "Estonia", "372"),
        Country("ET", "Ethiopia", "251"),
        Country("FI", "Finland", "358"),
        Country("FR", "France", "33"),
        Country("GA", "Gabon", "241"),
        Country("GM", "Gambia", "220"),
        Country("GE", "Georgia", "995"),
        Country("DE", "Germany", "49"),
        Country("GH", "Ghana", "233"),
        Country("GR", "Greece", "30"),
        Country("GT", "Guatemala", "502"),
        Country("GN", "Guinea", "224"),
        Country("HT", "Haiti", "509"),
        Country("HN", "Honduras", "504"),
        Country("HK", "Hong Kong", "852"),
        Country("HU", "Hungary", "36"),
        Country("IS", "Iceland", "354"),
        Country("IN", "India", "91"),
        Country("ID", "Indonesia", "62"),
        Country("IR", "Iran", "98"),
        Country("IQ", "Iraq", "964"),
        Country("IE", "Ireland", "353"),
        Country("IT", "Italy", "39"),
        Country("JM", "Jamaica", "1"),
        Country("JP", "Japan", "81"),
        Country("JO", "Jordan", "962"),
        Country("KZ", "Kazakhstan", "7"),
        Country("KE", "Kenya", "254"),
        Country("KW", "Kuwait", "965"),
        Country("KG", "Kyrgyzstan", "996"),
        Country("LA", "Laos", "856"),
        Country("LV", "Latvia", "371"),
        Country("LB", "Lebanon", "961"),
        Country("LR", "Liberia", "231"),
        Country("LY", "Libya", "218"),
        Country("LT", "Lithuania", "370"),
        Country("MY", "Malaysia", "60"),
        Country("MV", "Maldives", "960"),
        Country("ML", "Mali", "223"),
        Country("MR", "Mauritania", "222"),
        Country("MU", "Mauritius", "230"),
        Country("MX", "Mexico", "52"),
        Country("MD", "Moldova", "373"),
        Country("MN", "Mongolia", "976"),
        Country("MA", "Morocco", "212"),
        Country("MZ", "Mozambique", "258"),
        Country("MM", "Myanmar", "95"),
        Country("NA", "Namibia", "264"),
        Country("NP", "Nepal", "977"),
        Country("NL", "Netherlands", "31"),
        Country("NZ", "New Zealand", "64"),
        Country("NI", "Nicaragua", "505"),
        Country("NE", "Niger", "227"),
        Country("NG", "Nigeria", "234"),
        Country("KP", "North Korea", "850"),
        Country("MK", "North Macedonia", "389"),
        Country("NO", "Norway", "47"),
        Country("OM", "Oman", "968"),
        Country("PK", "Pakistan", "92"),
        Country("PS", "Palestine", "970"),
        Country("PA", "Panama", "507"),
        Country("PY", "Paraguay", "595"),
        Country("PE", "Peru", "51"),
        Country("PH", "Philippines", "63"),
        Country("PL", "Poland", "48"),
        Country("PT", "Portugal", "351"),
        Country("QA", "Qatar", "974"),
        Country("RO", "Romania", "40"),
        Country("RU", "Russia", "7"),
        Country("RW", "Rwanda", "250"),
        Country("SA", "Saudi Arabia", "966"),
        Country("SN", "Senegal", "221"),
        Country("RS", "Serbia", "381"),
        Country("SL", "Sierra Leone", "232"),
        Country("SG", "Singapore", "65"),
        Country("SK", "Slovakia", "421"),
        Country("SI", "Slovenia", "386"),
        Country("SO", "Somalia", "252"),
        Country("ZA", "South Africa", "27"),
        Country("KR", "South Korea", "82"),
        Country("SS", "South Sudan", "211"),
        Country("ES", "Spain", "34"),
        Country("LK", "Sri Lanka", "94"),
        Country("SD", "Sudan", "249"),
        Country("SE", "Sweden", "46"),
        Country("CH", "Switzerland", "41"),
        Country("SY", "Syria", "963"),
        Country("TW", "Taiwan", "886"),
        Country("TJ", "Tajikistan", "992"),
        Country("TZ", "Tanzania", "255"),
        Country("TH", "Thailand", "66"),
        Country("TG", "Togo", "228"),
        Country("TT", "Trinidad and Tobago", "1"),
        Country("TN", "Tunisia", "216"),
        Country("TR", "Türkiye", "90"),
        Country("TM", "Turkmenistan", "993"),
        Country("UG", "Uganda", "256"),
        Country("UA", "Ukraine", "380"),
        Country("AE", "United Arab Emirates", "971"),
        Country("GB", "United Kingdom", "44"),
        Country("US", "United States", "1"),
        Country("UY", "Uruguay", "598"),
        Country("UZ", "Uzbekistan", "998"),
        Country("VE", "Venezuela", "58"),
        Country("VN", "Vietnam", "84"),
        Country("YE", "Yemen", "967"),
        Country("ZM", "Zambia", "260"),
        Country("ZW", "Zimbabwe", "263")
    )

    private val byIso: Map<String, Country> = all.associateBy { it.iso }

    /** NSN lengths per ISO (v2.4 task 1) — the digit count after the
     *  dial code, trunk '0' excluded. The core markets are exact:
     *  NANPA (+1) 10 · PK 10 · IN 10 · BD 10 · NG 10 · GB 10 · AE 9 ·
     *  SA 9 · FR 9 · ES 9 · CN 11. Other entries carry their true NSN
     *  (a set where fixed-line and mobile lengths differ). Any ISO not
     *  listed here accepts [DEFAULT_NSN_LENGTHS] (7..13) rather than
     *  guessing a strict length that could reject real numbers. */
    private val nsnByIso: Map<String, Set<Int>> = mapOf(
        // +1 (NANPA) — always 10.
        "US" to setOf(10), "CA" to setOf(10), "DO" to setOf(10),
        "JM" to setOf(10), "TT" to setOf(10),
        // Core markets (exact, per the v2.4 spec).
        "PK" to setOf(10), "IN" to setOf(10), "BD" to setOf(10),
        "NG" to setOf(10), "GB" to setOf(10),
        "AE" to setOf(9), "SA" to setOf(9), "FR" to setOf(9),
        "ES" to setOf(9), "CN" to setOf(11),
        // True NSNs for the rest of the well-known list.
        "AF" to setOf(9), "DZ" to setOf(9), "AO" to setOf(9),
        "AM" to setOf(8), "AR" to setOf(10), "AU" to setOf(9),
        "AZ" to setOf(9), "BH" to setOf(8), "BE" to setOf(9),
        "BR" to setOf(10, 11), "BG" to setOf(9), "KH" to setOf(8, 9),
        "CM" to setOf(9), "CL" to setOf(9), "CO" to setOf(10),
        "CD" to setOf(9), "CR" to setOf(8), "CU" to setOf(8),
        "CY" to setOf(8), "CZ" to setOf(9), "DK" to setOf(8),
        "EG" to setOf(10), "SV" to setOf(8), "EE" to setOf(7, 8),
        "ET" to setOf(9), "FI" to setOf(9, 10), "GE" to setOf(9),
        "DE" to setOf(10, 11), "GH" to setOf(9), "GR" to setOf(10),
        "GT" to setOf(8), "HT" to setOf(8), "HN" to setOf(8),
        "HK" to setOf(8), "HU" to setOf(9), "IS" to setOf(7),
        "ID" to setOf(9, 10, 11, 12), "IQ" to setOf(10), "IE" to setOf(9),
        "IT" to setOf(9, 10), "JP" to setOf(10), "JO" to setOf(9),
        "KZ" to setOf(10), "KE" to setOf(9), "KW" to setOf(8),
        "KG" to setOf(9), "LA" to setOf(10), "LV" to setOf(8),
        "LB" to setOf(7, 8), "LY" to setOf(9), "LT" to setOf(8),
        "MY" to setOf(9, 10), "MV" to setOf(7), "ML" to setOf(8),
        "MR" to setOf(8), "MU" to setOf(8), "MX" to setOf(10),
        "MD" to setOf(8), "MN" to setOf(8), "MA" to setOf(9),
        "MZ" to setOf(9), "MM" to setOf(9, 10), "NA" to setOf(9),
        "NP" to setOf(10), "NL" to setOf(9), "NZ" to setOf(8, 9, 10),
        "NI" to setOf(8), "NE" to setOf(8), "MK" to setOf(8),
        "NO" to setOf(8), "OM" to setOf(8), "PS" to setOf(9),
        "PA" to setOf(8), "PY" to setOf(9), "PE" to setOf(9),
        "PH" to setOf(10), "PL" to setOf(9), "PT" to setOf(9),
        "QA" to setOf(8), "RO" to setOf(9), "RU" to setOf(10),
        "RW" to setOf(9), "SN" to setOf(9), "RS" to setOf(9),
        "SL" to setOf(8), "SG" to setOf(8), "SK" to setOf(9),
        "SI" to setOf(8), "ZA" to setOf(9), "KR" to setOf(10),
        "SS" to setOf(9), "LK" to setOf(9), "SD" to setOf(9),
        "SE" to setOf(9), "CH" to setOf(9), "SY" to setOf(9),
        "TW" to setOf(9), "TJ" to setOf(9), "TZ" to setOf(9),
        "TH" to setOf(9), "TG" to setOf(8), "TN" to setOf(8),
        "TR" to setOf(10), "TM" to setOf(8), "UG" to setOf(9),
        "UA" to setOf(9), "UY" to setOf(8), "UZ" to setOf(9),
        "VE" to setOf(10), "VN" to setOf(9), "YE" to setOf(9),
        "ZM" to setOf(9), "ZW" to setOf(9)
    )

    /** Fallback NSN window for countries without a pinned entry. */
    val DEFAULT_NSN_LENGTHS: Set<Int> = (7..13).toSet()

    fun nsnLengthsFor(iso: String): Set<Int> =
        nsnByIso[iso.uppercase()] ?: DEFAULT_NSN_LENGTHS

    /** All distinct dial codes, longest first — used to spot an
     *  already-international number during normalization. */
    val dialCodesLongestFirst: List<String> =
        all.map { it.dial }.distinct().sortedByDescending { it.length }

    fun byIso(iso: String?): Country? = iso?.let { byIso[it.uppercase()] }

    fun fallback(): Country = byIso["PK"] ?: all.first()
}
