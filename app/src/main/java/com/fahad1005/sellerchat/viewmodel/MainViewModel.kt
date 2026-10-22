package com.fahad1005.sellerchat.viewmodel

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fahad1005.sellerchat.data.AppDatabase
import com.fahad1005.sellerchat.data.Countries
import com.fahad1005.sellerchat.data.Country
import com.fahad1005.sellerchat.data.Recent
import com.fahad1005.sellerchat.data.RecentStatus
import com.fahad1005.sellerchat.data.SeedData
import com.fahad1005.sellerchat.data.SettingsStore
import com.fahad1005.sellerchat.data.Teammate
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.notify.FollowUps
import com.fahad1005.sellerchat.util.CurrencyUtils
import com.fahad1005.sellerchat.util.PhoneUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WhatsAppPackages {
    const val WHATSAPP = "com.whatsapp"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun displayName(pkg: String): String =
        if (pkg == WHATSAPP_BUSINESS) "WhatsApp Business" else "WhatsApp"
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val templateDao = db.templateDao()
    private val recentDao = db.recentDao()
    val settings = SettingsStore(app)

    // ---- Templates & recents (Room) ----

    val templates: StateFlow<List<Template>> = templateDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Unfiltered recents — the Pulse dashboard counts ALL customers,
     *  never the search-filtered subset. */
    val recentsAll: StateFlow<List<Recent>> = recentDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val recents: StateFlow<List<Recent>> = recentSearchQuery
        .flatMapLatest { q -> recentDao.observeFiltered(q.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Home state ----

    val homeNumber = MutableStateFlow("")
    val homeLabel = MutableStateFlow("")

    val selectedCountry = MutableStateFlow(resolveInitialCountry())
    val selectedAppPackage = MutableStateFlow<String?>(null)
    val selectedTemplateId = MutableStateFlow(settings.defaultTemplateId)

    /** Strict per-country validation of the Home number field
     *  (v2.4 task 1): derived LIVE from the typed number and the
     *  selected country, recomputed on every change of either. Home
     *  shows the inline error and disables every number-consuming
     *  action while this is Invalid, and the send paths use the
     *  Valid state's normalized number — what was validated is what
     *  gets sent. */
    val homeNumberValidation: StateFlow<PhoneUtils.NumberValidation> =
        combine(homeNumber, selectedCountry) { number, country ->
            PhoneUtils.validateForCountry(number, country)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PhoneUtils.NumberValidation.Empty
        )
    val installedWhatsApp = MutableStateFlow(false)
    val installedWhatsAppBusiness = MutableStateFlow(false)

    /** One-shot message for the Home snackbar (launch failures etc.). */
    val homeMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            // v1 seed pass (fresh installs only).
            if (templateDao.count() == 0) {
                templateDao.insertAll(SeedData.templates())
            }
            // v2 seed pass (v2 spec §3C): exactly once per install —
            // fresh installs and v1 upgraders both get the 12 new
            // templates appended after the current library order.
            if (!settings.seedsV2Done) {
                val startOrder = (templateDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
                templateDao.insertAll(SeedData.v2Templates(startOrder))
                settings.seedsV2Done = true
            }
            // v2.2 seed correction pass: seeds never rewrite rows on
            // their own, so the five corrected v2.2 defaults are applied
            // explicitly — and ONLY to a row whose body still equals
            // the old default byte-for-byte. A template the user has
            // edited is never clobbered.
            if (!settings.seedsV3Done) {
                val all = templateDao.getAll()
                for ((oldBody, newBody) in SeedData.seedCorrections()) {
                    all.filter { it.body == oldBody }.forEach { row ->
                        templateDao.update(row.copy(body = newBody))
                    }
                }
                settings.seedsV3Done = true
            }
            // v2.3 seed pass: the 12 French / Spanish / Arabic /
            // Chinese templates, appended exactly once and only AFTER
            // the v2 pass and the v2.2 corrections above — an upgrader
            // from v2.0 or v2.1 therefore runs V2 → V3 → V4 in order
            // on their first v2.3 launch. The title+language guard
            // also makes a lost flag harmless: a template that is
            // already in the library is never inserted twice.
            if (!settings.seedsV4Done) {
                val existing = templateDao.getAll()
                val existingKeys = existing.map { it.title to it.language }.toSet()
                val startOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val newTemplates = SeedData.v4Templates(startOrder)
                    .filterNot { (it.title to it.language) in existingKeys }
                if (newTemplates.isNotEmpty()) {
                    templateDao.insertAll(newTemplates)
                }
                settings.seedsV4Done = true
            }
            // v2.4 fragment correction pass: the v2.2 pass above only
            // rewrote rows still matching the old defaults
            // byte-for-byte, so a seller who had EDITED an order
            // confirmation kept the doubled "ka {item} ka order"
            // fragment (and its Urdu twin) forever. This pass rewrites
            // the fragment itself wherever it survives — the fragment
            // is never intentional text — and preserves everything
            // around it, the seller's edits included. Runs last in
            // the chain: V2 → V3 → V4 → V5.
            if (!settings.seedsV5Done) {
                for (row in templateDao.getAll()) {
                    var body = row.body
                    for ((bad, good) in SeedData.seedFragmentCorrections()) {
                        body = body.replace(bad, good)
                    }
                    if (body != row.body) {
                        templateDao.update(row.copy(body = body))
                    }
                }
                settings.seedsV5Done = true
            }
            // Smart default (v2 spec §2B): the default template may not
            // be "No template". If none was ever chosen, pre-select
            // Price Inquiry — for brand-new and upgraded installs alike.
            if (settings.defaultTemplateId == -1L) {
                val price = templateDao.findByTitle("Price inquiry reply")
                    ?: templateDao.getAll().firstOrNull()
                if (price != null) {
                    settings.defaultTemplateId = price.id
                    selectedTemplateId.value = price.id
                }
            }
        }
        refreshInstalledApps()
    }

    private fun resolveInitialCountry(): Country {
        // v2.3: the one-time region pass runs before anything reads
        // the country, so Home and First Run open on the detected (or
        // previously saved) values — never on a mid-init placeholder.
        initializeRegionIfNeeded()
        return Countries.byIso(settings.countryIso) ?: Countries.fallback()
    }

    /**
     * Global Smart Localization (v2.3): ONE detection pass per install,
     * gated by [SettingsStore.regionInitialized]. It runs for fresh
     * installs and for upgraders on their first v2.3 launch, persists
     * country + currency + preferred template language, sets the flag,
     * and never runs again — after this, the seller's manual Region &
     * Currency settings are sovereign.
     *
     * Precedence — country: SIM ISO → device Locale country → the
     * previously saved country → the built-in fallback. Currency: the
     * detected country through [CurrencyUtils.codeForCountry] (the
     * PK/IN/BD/NG/AE hand table wins; other countries resolve through
     * java.util.Currency). Language: the device Locale language mapped
     * to a template language; anything unsupported lands on EN, set
     * explicitly so Home prioritizes English.
     *
     * Only permission-free APIs are used (TelephonyManager SIM country
     * and Locale need no runtime permission), and every lookup is
     * wrapped: an exotic SIM or Locale can cost the user a default,
     * never a crash on first launch.
     */
    private fun initializeRegionIfNeeded() {
        if (settings.regionInitialized) return
        try {
            val ctx = getApplication<Application>()
            val simIso = try {
                val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                tm?.simCountryIso
            } catch (e: Exception) {
                null
            }
            val locale = try {
                java.util.Locale.getDefault()
            } catch (e: Exception) {
                java.util.Locale.US
            }
            val detectedCountry = Countries.byIso(simIso)
                ?: Countries.byIso(locale.country)
                ?: Countries.byIso(settings.countryIso)
                ?: Countries.fallback()
            settings.countryIso = detectedCountry.iso
            settings.currencyCode = CurrencyUtils.codeForCountry(detectedCountry)
            settings.preferredTemplateLanguage = when (locale.language.lowercase()) {
                "ur" -> TemplateLanguage.UR
                "hi" -> TemplateLanguage.HI
                "ta" -> TemplateLanguage.TA
                "te" -> TemplateLanguage.TE
                "mr" -> TemplateLanguage.MR
                "fr" -> TemplateLanguage.FR
                "es" -> TemplateLanguage.ES
                "ar" -> TemplateLanguage.AR
                "zh" -> TemplateLanguage.ZH
                else -> TemplateLanguage.EN
            }
        } catch (e: Exception) {
            // Crash-proof fallback: keep whatever country exists, and
            // land on USD + English rather than breaking first launch.
            if (Countries.byIso(settings.countryIso) == null) {
                settings.countryIso = Countries.fallback().iso
            }
            settings.currencyCode = SettingsStore.DEFAULT_CURRENCY_CODE
            settings.preferredTemplateLanguage = TemplateLanguage.EN
        } finally {
            settings.regionInitialized = true
        }
    }

    fun setCountry(country: Country) {
        selectedCountry.value = country
        settings.countryIso = country.iso
    }

    /** Package visibility only: <queries> in the manifest makes these two
     *  packages visible to getPackageInfo on Android 11+. */
    fun refreshInstalledApps() {
        val pm = getApplication<Application>().packageManager
        installedWhatsApp.value = isInstalled(pm, WhatsAppPackages.WHATSAPP)
        installedWhatsAppBusiness.value = isInstalled(pm, WhatsAppPackages.WHATSAPP_BUSINESS)
        val current = selectedAppPackage.value
        if (current == null || !isInstalled(pm, current)) {
            selectedAppPackage.value = when {
                settings.defaultAppPackage == WhatsAppPackages.WHATSAPP && installedWhatsApp.value ->
                    WhatsAppPackages.WHATSAPP
                settings.defaultAppPackage == WhatsAppPackages.WHATSAPP_BUSINESS && installedWhatsAppBusiness.value ->
                    WhatsAppPackages.WHATSAPP_BUSINESS
                installedWhatsApp.value -> WhatsAppPackages.WHATSAPP
                installedWhatsAppBusiness.value -> WhatsAppPackages.WHATSAPP_BUSINESS
                else -> null
            }
        }
    }

    private fun isInstalled(pm: PackageManager, pkg: String): Boolean {
        return try {
            pm.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /** Shared-text growth path: pre-fill Home with the extracted number. */
    fun onSharedText(text: String) {
        val found = PhoneUtils.extractNumber(text) ?: return
        homeNumber.value = found
    }

    // ---- Template filling (v2.1: smart auto-fill) ----

    /**
     * THE template fill — the single code path behind the Home preview
     * AND every send (Open chat, Copy wa.me link, Show QR, Try Demo),
     * so what the seller sees in the preview is byte-for-byte what the
     * customer receives.
     *
     * The customer label is parsed as "Name — item": it is split on
     * "—", "–", " - " or ",", the first segment becomes {name} and the
     * second becomes {item} ("Ayesha — kurta" → Ayesha / kurta). No
     * separator → the whole label is {name} and {item} is blank.
     *
     * Pipeline (v2.4 task 3 rewrote the middle; all deterministic —
     * no grammar engine):
     *  1. Empty-price sentence fallbacks: there is no price input
     *     anywhere in the app, so {price} always resolves empty, and
     *     the order-confirmation "total" clauses would read as broken
     *     fragments. Each language's clause is swapped wholesale for a
     *     complete sentence (three explicit shapes below). They run
     *     FIRST so the replacement sentences survive the drop pass —
     *     they contain no placeholders.
     *  2. Sentence-drop pass (v2.4): the text is split into sentences
     *     (boundaries `.` `!` `?` and the Urdu `۔`, delimiters kept
     *     attached). A sentence that still carries an unfilled
     *     CRITICAL placeholder — {price}, {payment method}, or {item}
     *     while the label gave no item — is dropped WHOLE instead of
     *     being hollowed out into broken grammar ("The is.", "send
     *     to."). {name} is NOT critical: a greeting clause survives a
     *     missing name. Never-empty guard: if dropping would leave
     *     nothing, the drop is skipped and step 3 runs on the
     *     post-step-1 text — an imperfect message beats an empty one.
     *  3. The existing cleanup pipeline on what remains, in its
     *     original order: currency-orphan rule (a marker — "Rs",
     *     "Rs.", "₹", "روپے", "$", "£", "€", or the symbol of the
     *     currency code configured in Settings — hugging the empty
     *     {price} slot is removed together with the placeholder),
     *     value substitution, bracket rule (any remaining [...]
     *     segment is a hint written for the SELLER, never text for
     *     the customer — removed entirely), leftover-token rule (ANY
     *     remaining {...} token, single- or multi-word, is stripped —
     *     a customer never sees a raw placeholder), tidy pass
     *     (collapse runs of spaces, pull punctuation back onto the
     *     word before it, trim the ends).
     */
    fun filledTemplateText(template: Template?): String {
        if (template == null) return ""
        val parts = homeLabel.value.trim()
            .split(Regex("\\s*[—–]\\s*|\\s+-\\s+|\\s*,\\s*"))
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val item = parts.getOrNull(1)?.trim().orEmpty()
        var out = template.body

        // Step 1 — empty-price sentence fallbacks for the three
        // order-confirmation total clauses (exact shapes, swapped
        // wholesale; v2.2 Rule 1, verbatim). The Roman Urdu sentence
        // is the founder's exact wording. No price input exists in
        // the app, so {price} always resolves empty and these
        // clauses would otherwise read as broken fragments. The
        // replacement sentences carry no placeholders, so the
        // sentence-drop pass below can never eat them.
        out = out
            .replace(
                "Total: Rs {price}.",
                "I'll confirm the total amount on reply."
            )
            .replace(
                "Total Rs {price} hai.",
                "Total amount confirm karne ke liye reply karein."
            )
            .replace(
                "کل رقم {price} روپے ہے۔",
                "کل رقم جواب میں کنفرم کر دوں گا۔"
            )

        // Step 2 — sentence-drop pass (v2.4 task 3): a sentence that
        // still carries an unfilled CRITICAL placeholder is removed
        // whole rather than hollowed out ("The {item} is Rs
        // {price}." must not degrade to "The is."). Critical =
        // {price} and {payment method} (both always unfilled at send
        // time) and {item} while the label gave no item. {name} is
        // deliberately NOT critical — greeting clauses survive a
        // missing name.
        val keptSentences = splitSentences(out).filterNot { sentence ->
            sentence.contains("{price}") ||
                sentence.contains("{payment method}") ||
                (item.isEmpty() && sentence.contains("{item}"))
        }
        val afterDrop = keptSentences.joinToString("")
        // Never-empty guard: if the drop would leave nothing, skip
        // it — the old strip pipeline below runs on the post-Step-1
        // text, so the customer still gets a (flawed) message.
        if (afterDrop.isNotBlank()) {
            out = afterDrop
        }

        // Step 3 — the existing cleanup pipeline, original order.
        // Currency-orphan rule: a marker left hugging the empty
        // {price} slot is removed together with the placeholder.
        // The marker set is the static core list plus the symbol of
        // the seller's configured currency code (Settings → Region &
        // Currency), quoted so a symbol can never act as regex
        // syntax. (After a successful drop pass no {price} remains
        // and this is a no-op; it still guards the never-empty path.)
        val configuredSymbol = CurrencyUtils.symbolForCode(settings.currencyCode)
        val markerPattern = listOf(
            "Rs\\.?", "₹", "روپے", "\\$", "£", "€",
            Regex.escape(configuredSymbol)
        ).distinct().joinToString("|")
        out = out.replace(Regex("($markerPattern)\\s*\\{price\\}"), "")
        out = out.replace(Regex("\\{price\\}\\s*($markerPattern)"), "")

        out = out
            .replace("{name}", name)
            .replace("{item}", item)
            .replace("{price}", "")
            .replace("{qty}", "")
        // Bracket rule — unfilled [...] hints are removed whole.
        out = out.replace(Regex("\\[[^\\]]*\\]"), "")
        // Leftover-token rule — any remaining {...} token is stripped.
        out = out.replace(Regex("\\{[^}]*\\}"), "")
        // Tidy — space runs collapse, no space before punctuation
        // (. , ! ? and the Urdu ، ۔), trim the ends.
        out = out.replace(Regex(" {2,}"), " ")
        out = out.replace(Regex(" +([.,!?،۔])"), "$1")
        return out.trim()
    }

    /** Split into sentences on `.` `!` `?` and the Urdu full stop
     *  `۔`, keeping each delimiter attached to its sentence. A
     *  trailing fragment without a delimiter is kept as a final
     *  sentence. (v2.4 task 3, step 2.) */
    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        for (ch in text) {
            current.append(ch)
            if (ch == '.' || ch == '!' || ch == '?' || ch == '۔') {
                sentences += current.toString()
                current.clear()
            }
        }
        if (current.isNotEmpty()) {
            sentences += current.toString()
        }
        return sentences
    }

    // ---- Chat launching ----

    /**
     * Open the chat for [e164] in [pkg] with [text] pre-filled (never
     * auto-sent). Returns true when WhatsApp accepted the intent.
     * There is deliberately NO "is this number on WhatsApp" pre-check —
     * those checks produce the false errors this app exists to avoid.
     */
    fun openChat(pkg: String, e164: String, text: String): Boolean {
        val ctx = getApplication<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PhoneUtils.waMeLink(e164, text))).apply {
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * "Try Demo" (v2 spec §2A): pre-fill the fake number + the Price
     * Inquiry template on Home and launch WhatsApp immediately, so a
     * new user sees the whole trick before any setup. Nothing is
     * recorded to Recents — it is a demo, not a customer. Returns
     * false when no WhatsApp app is installed or the launch failed.
     */
    fun tryDemo(): Boolean {
        refreshInstalledApps()
        val pkg = selectedAppPackage.value ?: return false
        homeNumber.value = "+92 300 1234567"
        val template = templates.value.firstOrNull { it.title == "Price inquiry reply" }
            ?: templates.value.firstOrNull()
        if (template != null) {
            selectedTemplateId.value = template.id
        }
        val pk = Countries.byIso("PK") ?: selectedCountry.value
        // v2.4: the demo is a send path like any other — it uses the
        // strict validator's normalized number, not loose normalize.
        val e164 = when (val v = PhoneUtils.validateForCountry("+92 300 1234567", pk)) {
            is PhoneUtils.NumberValidation.Valid -> v.waNumber
            else -> return false
        }
        val text = filledTemplateText(template)
        return openChat(pkg, e164, text)
    }

    /**
     * "Forward to Team" (v2.2, renamed + richer message in v2.4):
     * hand [recent]'s customer over to a saved teammate. Opens
     * WhatsApp on the TEAMMATE's number — never the customer's —
     * with the exact hand-over text, through the same
     * package-targeted launch path as Open Chat: the seller's default
     * app first (Settings default, else the current Home selection),
     * then the other installed app as fallback.
     *
     * Deliberately records NO Recent and touches no CRM state: the
     * teammate is not a customer conversation. Returns true when a
     * chat opened.
     */
    fun forwardToTeammate(recent: Recent, teammate: Teammate): Boolean {
        val e164 = PhoneUtils.normalize(teammate.number, selectedCountry.value) ?: return false
        val customerLabel = recent.label.ifBlank { recent.numberE164 }
        val context = recent.templateTitle?.takeIf { it.isNotBlank() } ?: "General"
        val text = "Customer Name: $customerLabel, Number: ${recent.numberE164}, " +
            "Context: $context. Please take over this chat."
        refreshInstalledApps()
        val pm = getApplication<Application>().packageManager
        val candidates = listOfNotNull(
            settings.defaultAppPackage,
            selectedAppPackage.value,
            WhatsAppPackages.WHATSAPP,
            WhatsAppPackages.WHATSAPP_BUSINESS
        ).distinct().filter { isInstalled(pm, it) }
        for (pkg in candidates) {
            if (openChat(pkg, e164, text)) return true
        }
        return false
    }

    /** Record a successful launch. Re-messaging an existing customer
     *  updates the contact facts but PRESERVES their v2 CRM state
     *  (status, memo, follow-up, payment) — the upsert is a REPLACE,
     *  so the existing row is loaded and merged, never rebuilt blank. */
    fun recordRecent(e164: String, label: String, pkg: String, templateTitle: String?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = recentDao.getByNumber(e164)
            if (existing != null) {
                recentDao.upsert(
                    existing.copy(
                        label = label,
                        appPackage = pkg,
                        templateTitle = templateTitle,
                        timestamp = now
                    )
                )
            } else {
                val status = settings.defaultStatus
                recentDao.upsert(
                    Recent(
                        numberE164 = e164,
                        label = label,
                        appPackage = pkg,
                        templateTitle = templateTitle,
                        timestamp = now,
                        status = status,
                        statusChangedAt = now
                    )
                )
            }
        }
    }

    // ---- Template actions ----

    fun addTemplate(title: String, body: String, language: TemplateLanguage) {
        viewModelScope.launch {
            val nextOrder = (templates.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            templateDao.insert(Template(title = title, body = body, language = language, sortOrder = nextOrder))
        }
    }

    fun updateTemplate(template: Template) {
        viewModelScope.launch { templateDao.update(template) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { templateDao.deleteById(id) }
    }

    fun duplicateTemplate(template: Template) {
        viewModelScope.launch {
            val nextOrder = (templates.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            templateDao.insert(
                template.copy(id = 0, title = template.title + " (copy)", sortOrder = nextOrder)
            )
        }
    }

    fun reorderTemplates(orderedIds: List<Long>) {
        viewModelScope.launch {
            val byId = templates.value.associateBy { it.id }
            orderedIds.forEachIndexed { index, id ->
                val t = byId[id]
                if (t != null && t.sortOrder != index) {
                    templateDao.update(t.copy(sortOrder = index))
                }
            }
        }
    }

    // ---- Recent actions ----

    fun renameRecent(recent: Recent, newLabel: String) {
        viewModelScope.launch { recentDao.upsert(recent.copy(label = newLabel)) }
    }

    fun deleteRecent(number: String) {
        viewModelScope.launch {
            FollowUps.cancel(getApplication(), number)
            recentDao.deleteByNumber(number)
        }
    }

    fun clearRecents() {
        viewModelScope.launch {
            recentsAll.value.forEach { FollowUps.cancel(getApplication(), it.numberE164) }
            recentDao.clearAll()
        }
    }

    /** Move a customer to [status]. Moving INTO Paid stamps paidAt and
     *  stores the (optional, whole-unit) amount; [paidAmount] null =
     *  the user skipped the amount prompt. statusChangedAt always
     *  moves: the waiting clock restarts from the change. */
    fun setRecentStatus(recent: Recent, status: RecentStatus, paidAmount: Long? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = if (status == RecentStatus.PAID) {
                recent.copy(
                    status = status,
                    statusChangedAt = now,
                    paidAmountMinor = paidAmount,
                    paidAt = now
                )
            } else {
                recent.copy(status = status, statusChangedAt = now)
            }
            recentDao.upsert(updated)
        }
    }

    fun setRecentMemo(recent: Recent, memo: String) {
        viewModelScope.launch {
            recentDao.upsert(recent.copy(memo = memo.trim().ifBlank { null }))
        }
    }

    /** "Follow-up in 2 days" (v2 spec §4). [withNotification] is the
     *  outcome of the in-context POST_NOTIFICATIONS ask: true → a real
     *  WorkManager notification is scheduled; false → in-app reminder
     *  only (and any previously scheduled job is cancelled so the row
     *  never claims less than what will actually happen). */
    fun setFollowUp(recent: Recent, withNotification: Boolean) {
        viewModelScope.launch {
            val at = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
            val ctx = getApplication<Application>()
            if (withNotification) {
                FollowUps.schedule(ctx, recent.numberE164, recent.appPackage, recent.label, at)
            } else {
                FollowUps.cancel(ctx, recent.numberE164)
            }
            recentDao.upsert(
                recent.copy(followUpAt = at, followUpNotified = withNotification)
            )
        }
    }

    fun templateById(id: Long): Template? = templates.value.firstOrNull { it.id == id }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                MainViewModel(checkNotNull(app))
            }
        }
    }
}
