package com.fahad1005.sellerchat.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One saved teammate for "Forward to Teammate" (v2.2). [number] is
 *  stored exactly as the seller typed it; it is normalized to E.164
 *  with the default country when a forward is launched. */
data class Teammate(val name: String, val number: String)

/** Small key-value settings in SharedPreferences — no extra dependency
 *  (DataStore would cost more size for the same job). Templates and
 *  recents live in Room; only simple choices live here. */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("sellerchat_settings", Context.MODE_PRIVATE)

    var firstRunDone: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN_DONE, value).apply()

    /** ISO country code the user picked (or the SIM default on first run). */
    var countryIso: String?
        get() = prefs.getString(KEY_COUNTRY_ISO, null)
        set(value) = prefs.edit().putString(KEY_COUNTRY_ISO, value).apply()

    /** "com.whatsapp" or "com.whatsapp.w4b"; null = no explicit default. */
    var defaultAppPackage: String?
        get() = prefs.getString(KEY_DEFAULT_APP, null)
        set(value) = prefs.edit().putString(KEY_DEFAULT_APP, value).apply()

    /** Template id used by default on Home; -1 = none chosen yet.
     *  v2: the Settings picker no longer offers "No template" as a
     *  default, and the v2 seed pass pre-selects Price Inquiry when
     *  this is still -1 (spec §2B smart default). Per-send "No
     *  template" on Home is unaffected. */
    var defaultTemplateId: Long
        get() = prefs.getLong(KEY_DEFAULT_TEMPLATE, -1L)
        set(value) = prefs.edit().putLong(KEY_DEFAULT_TEMPLATE, value).apply()

    /** The seller's own WhatsApp number (E.164 digits) for the status link. */
    var myNumberE164: String?
        get() = prefs.getString(KEY_MY_NUMBER, null)
        set(value) = prefs.edit().putString(KEY_MY_NUMBER, value).apply()

    // ---- v2 additions ----

    /** Preferred template language: Home sorts these chips first.
     *  Stored as the enum name; null = "Any" (no preference). */
    var preferredTemplateLanguage: TemplateLanguage?
        get() = prefs.getString(KEY_PREFERRED_LANGUAGE, null)?.let { name ->
            TemplateLanguage.values().firstOrNull { it.name == name }
        }
        set(value) = prefs.edit().putString(KEY_PREFERRED_LANGUAGE, value?.name).apply()

    /** Status every NEW recent starts with (spec §5). Default INQUIRY. */
    var defaultStatus: RecentStatus
        get() = prefs.getString(KEY_DEFAULT_STATUS, null)?.let { name ->
            RecentStatus.values().firstOrNull { it.name == name }
        } ?: RecentStatus.INQUIRY
        set(value) = prefs.edit().putString(KEY_DEFAULT_STATUS, value.name).apply()

    /** One-time Home tooltip ("Paste a number → …") shown flag. Once
     *  true, the banner never appears again (spec §2A). */
    var homeTooltipShown: Boolean
        get() = prefs.getBoolean(KEY_HOME_TOOLTIP_SHOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_HOME_TOOLTIP_SHOWN, value).apply()

    /** One-time v2 seed pass done flag: the 12 v2 templates (3 Hinglish
     *  + 9 Tamil/Telugu/Marathi) are inserted exactly once per install,
     *  for fresh installs AND v1 upgraders alike, never duplicated. */
    var seedsV2Done: Boolean
        get() = prefs.getBoolean(KEY_SEEDS_V2_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDS_V2_DONE, value).apply()

    // ---- v2.3 additions ----

    /** One-time Global Smart Localization flag. While unset, the first
     *  v2.3 launch detects country / currency / preferred template
     *  language once and persists them. Once set, detection never runs
     *  again — the seller's manual Region & Currency choices are
     *  sovereign. */
    var regionInitialized: Boolean
        get() = prefs.getBoolean(KEY_REGION_INITIALIZED, false)
        set(value) = prefs.edit().putBoolean(KEY_REGION_INITIALIZED, value).apply()

    /** ISO 4217 currency code chosen for amount prompts, the Pulse
     *  dashboard and the fill function's currency-orphan rule. The
     *  one-time region pass initializes it from the detected country;
     *  after that it is a manual Settings choice. Defaults to USD only
     *  as a crash-proof read fallback before initialization writes a
     *  detected value.
     *
     *  v2.4 task 4: the value is ALSO mirrored in [currencyCodeFlow],
     *  updated on every write. Before v2.4 the Recents Pulse read
     *  this bare SharedPreferences property once at composition —
     *  SharedPreferences gives Compose no change signal, so the
     *  Pulse symbol only refreshed by accident (a route teardown or
     *  the 60-second clock happening to recompose the screen). The
     *  flow is the observation path; the plain property remains for
     *  one-shot reads (e.g. the fill function). */
    var currencyCode: String
        get() = _currencyCodeFlow.value
        set(value) {
            val normalized = value.uppercase()
            prefs.edit().putString(KEY_CURRENCY_CODE, normalized).apply()
            _currencyCodeFlow.value = normalized
        }

    private val _currencyCodeFlow = MutableStateFlow(loadCurrencyCode())

    /** Observable currency code: collectors (the Recents Pulse and
     *  the Mark Paid prompt) re-render in the same frame as a
     *  Settings write — no restart, no manual refresh. */
    val currencyCodeFlow: StateFlow<String> = _currencyCodeFlow.asStateFlow()

    private fun loadCurrencyCode(): String =
        prefs.getString(KEY_CURRENCY_CODE, null)
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()
            ?: DEFAULT_CURRENCY_CODE

    /** One-time v2.3 seed pass done flag: the 12 French / Spanish /
     *  Arabic / Chinese templates are appended exactly once per
     *  install, after the v2 pass and v2.2 corrections have run. */
    var seedsV4Done: Boolean
        get() = prefs.getBoolean(KEY_SEEDS_V4_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDS_V4_DONE, value).apply()

    // ---- v2.2 additions ----

    /** One-time v2.2 seed correction pass done flag: the five corrected
     *  default bodies are written over stored rows ONLY where the row
     *  still matches the old default verbatim (see SeedData). */
    var seedsV3Done: Boolean
        get() = prefs.getBoolean(KEY_SEEDS_V3_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDS_V3_DONE, value).apply()

    /** One-time v2.4 fragment correction pass done flag: the doubled
     *  "ka {item} ka order" fragment (and its Urdu-script twin) is
     *  rewritten wherever it survives in a stored template body —
     *  including bodies the seller edited, which the byte-for-byte
     *  v2.2 pass could not touch (see SeedData). */
    var seedsV5Done: Boolean
        get() = prefs.getBoolean(KEY_SEEDS_V5_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDS_V5_DONE, value).apply()

    // ---- v2.6 additions: multi-business ----

    /** Id of the currently selected business ("default" = My
     *  Business). Money figures, khata, invoices, products and
     *  expenses all filter on this value; customers, templates and
     *  scheduled messages ignore it (they are global). */
    var selectedBusinessId: String
        get() = prefs.getString(KEY_SELECTED_BUSINESS, null) ?: "default"
        set(value) = prefs.edit().putString(KEY_SELECTED_BUSINESS, value).apply()

    // ---- v2.5 additions: assisted Greeting / Away ----

    /** One-time v2.5 seed pass done flag: the editable built-in
     *  "Greeting" and "Away" templates are appended to the library
     *  exactly once (see MainViewModel init). They are ordinary
     *  templates afterwards — the seller may edit or delete them. */
    var seedsV6Done: Boolean
        get() = prefs.getBoolean(KEY_SEEDS_V6_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDS_V6_DONE, value).apply()

    /** Assisted greeting: when on, opening a chat for a customer with
     *  no prior Recent suggests the greeting text on Home for
     *  one-tap sending. This is a SUGGESTION, never an auto-send —
     *  see the boundary note in BUILD_NOTES (v2.5). */
    var greetingEnabled: Boolean
        get() = prefs.getBoolean(KEY_GREETING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GREETING_ENABLED, value).apply()

    var greetingMessage: String
        get() = prefs.getString(KEY_GREETING_MESSAGE, null)
            ?: DEFAULT_GREETING_MESSAGE
        set(value) = prefs.edit().putString(KEY_GREETING_MESSAGE, value).apply()

    /** Assisted away mode: when on and the current hour is outside
     *  [awayOpenHour] (inclusive) .. [awayCloseHour] (exclusive),
     *  Home shows an "Away mode" banner and offers the away text
     *  for one-tap sending. Hours are 0–23; an open hour AFTER the
     *  close hour means overnight hours (e.g. 20 → 2). */
    var awayEnabled: Boolean
        get() = prefs.getBoolean(KEY_AWAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AWAY_ENABLED, value).apply()

    var awayMessage: String
        get() = prefs.getString(KEY_AWAY_MESSAGE, null)
            ?: DEFAULT_AWAY_MESSAGE
        set(value) = prefs.edit().putString(KEY_AWAY_MESSAGE, value).apply()

    var awayOpenHour: Int
        get() = prefs.getInt(KEY_AWAY_OPEN_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_AWAY_OPEN_HOUR, value.coerceIn(0, 23)).apply()

    var awayCloseHour: Int
        get() = prefs.getInt(KEY_AWAY_CLOSE_HOUR, 17)
        set(value) = prefs.edit().putInt(KEY_AWAY_CLOSE_HOUR, value.coerceIn(0, 23)).apply()

    /** Up to [MAX_TEAMMATES] teammates for "Forward to Teammate"
     *  (Recents sheet). Stored in fixed slots team_name_1..3 /
     *  team_number_1..3 — three scalars are all a hand-over list
     *  needs, so no new Room entity. The number is kept exactly as
     *  entered and normalized (with the default country) only when a
     *  forward is launched. */
    fun getTeammates(): List<Teammate> {
        val out = mutableListOf<Teammate>()
        for (slot in 1..MAX_TEAMMATES) {
            val name = prefs.getString(teamNameKey(slot), null)?.trim().orEmpty()
            val number = prefs.getString(teamNumberKey(slot), null)?.trim().orEmpty()
            if (name.isNotEmpty() && number.isNotEmpty()) out += Teammate(name, number)
        }
        return out
    }

    /** Append a teammate; false when all slots are already full. */
    fun addTeammate(name: String, number: String): Boolean {
        val current = getTeammates()
        if (current.size >= MAX_TEAMMATES) return false
        saveTeammates(current + Teammate(name.trim(), number.trim()))
        return true
    }

    fun removeTeammate(teammate: Teammate) {
        saveTeammates(getTeammates() - teammate)
    }

    private fun saveTeammates(list: List<Teammate>) {
        val editor = prefs.edit()
        for (slot in 1..MAX_TEAMMATES) {
            val mate = list.getOrNull(slot - 1)
            editor.putString(teamNameKey(slot), mate?.name)
            editor.putString(teamNumberKey(slot), mate?.number)
        }
        editor.apply()
    }

    private fun teamNameKey(slot: Int) = "team_name_$slot"
    private fun teamNumberKey(slot: Int) = "team_number_$slot"

    companion object {
        private const val KEY_SELECTED_BUSINESS = "selected_business_id"
        private const val KEY_FIRST_RUN_DONE = "first_run_done"
        private const val KEY_COUNTRY_ISO = "country_iso"
        private const val KEY_DEFAULT_APP = "default_app_package"
        private const val KEY_DEFAULT_TEMPLATE = "default_template_id"
        private const val KEY_MY_NUMBER = "my_number_e164"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_template_language"
        private const val KEY_DEFAULT_STATUS = "default_status"
        private const val KEY_HOME_TOOLTIP_SHOWN = "home_tooltip_shown"
        private const val KEY_SEEDS_V2_DONE = "seeds_v2_done"
        private const val KEY_SEEDS_V3_DONE = "seeds_v3_done"
        private const val KEY_SEEDS_V5_DONE = "seeds_v5_done"
        private const val KEY_SEEDS_V6_DONE = "seeds_v6_done"
        private const val KEY_GREETING_ENABLED = "greeting_enabled"
        private const val KEY_GREETING_MESSAGE = "greeting_message"
        private const val KEY_AWAY_ENABLED = "away_enabled"
        private const val KEY_AWAY_MESSAGE = "away_message"
        private const val KEY_AWAY_OPEN_HOUR = "away_open_hour"
        private const val KEY_AWAY_CLOSE_HOUR = "away_close_hour"

        /** Default assisted texts (v2.5). Plain, no placeholders —
         *  placeholders in a greeting/away text would be stripped by
         *  the send cleanup anyway; the seller can edit both in
         *  Settings and as library templates. */
        const val DEFAULT_GREETING_MESSAGE =
            "Hello! Thanks for reaching out — how can I help you today?"
        const val DEFAULT_AWAY_MESSAGE =
            "Thanks for your message! We're currently closed and will reply during business hours."
        private const val KEY_REGION_INITIALIZED = "region_initialized"
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_SEEDS_V4_DONE = "seeds_v4_done"

        /** Read fallback only — the v2.3 one-time detection normally
         *  writes a country-derived code before Settings is opened. */
        const val DEFAULT_CURRENCY_CODE = "USD"

        /** "Forward to Teammate" cap (v2.2): a hand-over list, not an address book. */
        const val MAX_TEAMMATES = 3

        /** Future "Remove ads" IAP switch — declared, marked, and
         *  deliberately NOT implemented in v1 (spec §8). When the IAP
         *  ships, read this flag before showing the Home banner. */
        const val REMOVE_ADS_IAP_ENABLED: Boolean = false
    }
}
