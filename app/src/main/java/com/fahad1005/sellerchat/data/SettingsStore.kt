package com.fahad1005.sellerchat.data

import android.content.Context
import android.content.SharedPreferences

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

    companion object {
        private const val KEY_FIRST_RUN_DONE = "first_run_done"
        private const val KEY_COUNTRY_ISO = "country_iso"
        private const val KEY_DEFAULT_APP = "default_app_package"
        private const val KEY_DEFAULT_TEMPLATE = "default_template_id"
        private const val KEY_MY_NUMBER = "my_number_e164"
        private const val KEY_PREFERRED_LANGUAGE = "preferred_template_language"
        private const val KEY_DEFAULT_STATUS = "default_status"
        private const val KEY_HOME_TOOLTIP_SHOWN = "home_tooltip_shown"
        private const val KEY_SEEDS_V2_DONE = "seeds_v2_done"

        /** Future "Remove ads" IAP switch — declared, marked, and
         *  deliberately NOT implemented in v1 (spec §8). When the IAP
         *  ships, read this flag before showing the Home banner. */
        const val REMOVE_ADS_IAP_ENABLED: Boolean = false
    }
}
