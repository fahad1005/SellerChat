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
import com.fahad1005.sellerchat.data.Business
import com.fahad1005.sellerchat.data.ChannelHandle
import com.fahad1005.sellerchat.data.Countries
import com.fahad1005.sellerchat.data.Country
import com.fahad1005.sellerchat.data.Expense
import com.fahad1005.sellerchat.data.Invoice
import com.fahad1005.sellerchat.data.InvoiceLine
import com.fahad1005.sellerchat.data.InvoiceStatus
import com.fahad1005.sellerchat.data.KhataEntry
import com.fahad1005.sellerchat.data.KhataEntryType
import com.fahad1005.sellerchat.data.PaymentMethod
import com.fahad1005.sellerchat.data.Product
import com.fahad1005.sellerchat.data.Recent
import com.fahad1005.sellerchat.data.RecentStatus
import com.fahad1005.sellerchat.data.ScheduledMessage
import com.fahad1005.sellerchat.data.ScheduledStatus
import com.fahad1005.sellerchat.data.SeedData
import com.fahad1005.sellerchat.data.SettingsStore
import com.fahad1005.sellerchat.data.Teammate
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.notify.FollowUps
import com.fahad1005.sellerchat.notify.KhataReminders
import com.fahad1005.sellerchat.notify.ScheduledMessages
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
    private val invoiceDao = db.invoiceDao()
    private val productDao = db.productDao()
    private val scheduledDao = db.scheduledMessageDao()
    private val businessDao = db.businessDao()
    private val khataDao = db.khataDao()
    private val channelHandleDao = db.channelHandleDao()
    private val expenseDao = db.expenseDao()
    val settings = SettingsStore(app)

    // ---- v2.6 multi-business: selection + scoped flows ----
    //
    // Scoping rule: money flows (invoices, products, khata,
    // expenses) switch with the selected business via flatMapLatest;
    // recents, templates and scheduled messages stay global and
    // are NOT switched here. Every screen that collects one of
    // these flows therefore shows the selected shop automatically
    // — no screen carries its own filter.

    val selectedBusinessId = MutableStateFlow(settings.selectedBusinessId)

    val businesses: StateFlow<List<Business>> = businessDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedBusiness: StateFlow<Business?> =
        combine(businesses, selectedBusinessId) { list, id ->
            list.firstOrNull { it.id == id } ?: list.firstOrNull { it.id == "default" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectBusiness(id: String) {
        selectedBusinessId.value = id
        settings.selectedBusinessId = id
    }

    /** One-shot note for the Manage-businesses section (delete
     *  blocked / deleted). The screen shows it, then clears it. */
    val businessManageNote = MutableStateFlow<String?>(null)

    fun addBusiness(name: String, currency: String) {
        viewModelScope.launch {
            val id = "biz_${System.currentTimeMillis()}"
            businessDao.insert(
                Business(
                    id = id,
                    name = name.trim(),
                    currency = currency.uppercase(),
                    createdAt = System.currentTimeMillis()
                )
            )
            selectBusiness(id)
        }
    }

    fun renameBusiness(business: Business, newName: String) {
        viewModelScope.launch {
            businessDao.update(business.copy(name = newName.trim()))
        }
    }

    /** Delete ONLY when the shop is empty: zero khata entries,
     *  invoices, products and expenses. Otherwise the note explains
     *  why, and nothing is deleted — deleting a shop with history
     *  would orphan the seller's own book. If the deleted shop was
     *  selected, selection falls back to "default". */
    fun deleteBusiness(business: Business) {
        viewModelScope.launch {
            val khata = khataDao.countForBusiness(business.id)
            val invoices = invoiceDao.countForBusiness(business.id)
            val products = productDao.countForBusiness(business.id)
            val expenses = expenseDao.countForBusiness(business.id)
            val total = khata + invoices + products + expenses
            if (total > 0) {
                businessManageNote.value =
                    "Cannot delete \"${business.name}\" — it still has " +
                        "$total records (khata, invoices, products or expenses). " +
                        "Move or remove those first."
                return@launch
            }
            businessDao.deleteById(business.id)
            if (selectedBusinessId.value == business.id) {
                selectBusiness("default")
            }
            businessManageNote.value = "\"${business.name}\" deleted."
        }
    }

    /**
     * Default-business adoption (v2.6 first launch). Fresh installs:
     * the businesses table is empty, so the default shop is created
     * with the seller's Settings currency. Upgraders: migration 3 →
     * 4 seeded ('default', 'My Business', 'USD', 0) — createdAt == 0
     * marks that seed, so its currency is adopted from Settings
     * here and the marker cleared. Either way a v2.5 user opens v2.6
     * and finds every existing row under "My Business". Also
     * repairs a selection pointing at a deleted shop.
     */
    private fun ensureDefaultBusiness() {
        viewModelScope.launch {
            val existing = businessDao.getById("default")
            if (existing == null) {
                businessDao.insert(
                    Business(
                        id = "default",
                        name = "My Business",
                        currency = settings.currencyCode,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else if (existing.createdAt == 0L) {
                businessDao.update(
                    existing.copy(
                        currency = settings.currencyCode,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            if (businessDao.getById(selectedBusinessId.value) == null) {
                selectBusiness("default")
            }
        }
    }

    // ---- v2.6 winner pack (Room, business-scoped) ----

    @OptIn(ExperimentalCoroutinesApi::class)
    val khataEntries: StateFlow<List<KhataEntry>> = selectedBusinessId
        .flatMapLatest { id -> khataDao.observeByBusiness(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun khataForCustomer(number: String): StateFlow<List<KhataEntry>> =
        selectedBusinessId
            .flatMapLatest { id -> khataDao.observeForCustomerInBusiness(number, id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<Expense>> = selectedBusinessId
        .flatMapLatest { id -> expenseDao.observeByBusiness(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Discount % typed on Home for the picked product (v2.6). 0 =
     *  no discount. Read by the shared fill so the preview and the
     *  send always agree; the scheduled path fills with no product
     *  and is therefore never discounted. */
    val discountPercent = MutableStateFlow(0)

    // ---- v2.5 business tools (Room) ----
    // v2.6: invoices and products are business-scoped (they follow
    // the Money-tab switcher); scheduled messages stay global.

    @OptIn(ExperimentalCoroutinesApi::class)
    val invoices: StateFlow<List<Invoice>> = selectedBusinessId
        .flatMapLatest { id -> invoiceDao.observeByBusiness(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun invoiceLines(invoiceId: Long): StateFlow<List<InvoiceLine>> =
        invoiceDao.observeLines(invoiceId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = selectedBusinessId
        .flatMapLatest { id -> productDao.observeByBusiness(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val scheduledMessages: StateFlow<List<ScheduledMessage>> =
        scheduledDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Product picked on Home: its name fills {item} and its price
     *  fills {price} in the shared fill below — the picker never
     *  writes text itself, so preview and send cannot diverge. */
    val selectedProduct = MutableStateFlow<Product?>(null)

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
        ensureDefaultBusiness()
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
            // v2.5 seed pass: the editable built-in "Greeting" and
            // "Away" templates for the assisted greeting/away feature.
            // They are ordinary library rows afterwards (editable,
            // deletable). Title-guarded like the v2.3 pass, so a lost
            // flag can never duplicate them.
            if (!settings.seedsV6Done) {
                val existing = templateDao.getAll()
                val existingTitles = existing.map { it.title }.toSet()
                val startOrder = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
                val builtIns = listOf(
                    Template(
                        title = "Greeting",
                        body = SettingsStore.DEFAULT_GREETING_MESSAGE,
                        language = TemplateLanguage.EN,
                        sortOrder = startOrder
                    ),
                    Template(
                        title = "Away",
                        body = SettingsStore.DEFAULT_AWAY_MESSAGE,
                        language = TemplateLanguage.EN,
                        sortOrder = startOrder + 1
                    )
                ).filterNot { it.title in existingTitles }
                if (builtIns.isNotEmpty()) {
                    templateDao.insertAll(builtIns)
                }
                settings.seedsV6Done = true
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

    fun filledTemplateText(template: Template?): String {
        if (template == null) return ""
        return fillTemplate(template, homeLabel.value, selectedProduct.value)
    }

    /** Fill a template for a given label WITHOUT touching Home state —
     *  used by the Scheduled screen, which fills at creation time so
     *  the seller approves exactly what will open later. No product:
     *  {price} stays empty, exactly as a product-less Home send. */
    fun filledTemplateTextFor(template: Template?, labelValue: String): String {
        if (template == null) return ""
        return fillTemplate(template, labelValue, null)
    }

    /**
     * THE template fill — the single code path behind the Home preview
     * AND every send (Open chat, Copy wa.me link, Show QR, Try Demo,
     * scheduled creation), so what the seller sees in the preview is
     * byte-for-byte what the customer receives.
     *
     * The customer label is parsed as "Name — item": it is split on
     * "—", "–", " - " or ",", the first segment becomes {name} and the
     * second becomes {item} ("Ayesha — kurta" → Ayesha / kurta). No
     * separator → the whole label is {name} and {item} is blank.
     *
     * v2.5 product branch: when [product] is picked on Home, its name
     * fills {item} (only when the label gave no item) and its price
     * fills {price} as plain whole-unit digits. With a real price the
     * pipeline changes in exactly the ways a price makes safe: the
     * empty-price total-clause swaps are skipped (so "Total: Rs
     * {price}." becomes "Total: Rs 500."), {price} sentences are no
     * longer dropped, and the currency-orphan rule is skipped (it
     * exists only to clean up an EMPTY price slot). Without a product
     * every rule behaves exactly as in v2.4.
     *
     * Pipeline (all deterministic — no grammar engine):
     *  1. Empty-price sentence fallbacks (skipped with a product):
     *     each language's order-confirmation "total" clause is swapped
     *     wholesale for a complete sentence. They run FIRST so the
     *     replacement sentences survive the drop pass — they contain
     *     no placeholders.
     *  2. Sentence-drop pass: the text is split into sentences
     *     (boundaries `.` `!` `?`, the Urdu `۔`, the Arabic `؟`,
     *     and the Chinese fullwidth `。` `！` `？` — delimiters kept
     *     attached). A sentence that still carries an unfilled
     *     CRITICAL placeholder — {payment method} (always unfilled),
     *     {price} (only without a product), or {item} (only while
     *     no item is known) — is dropped WHOLE instead of being
     *     hollowed out into broken grammar. {name} is NOT critical:
     *     a greeting clause survives a missing name. Never-empty
     *     guard: if dropping would leave nothing, the drop is
     *     skipped and step 3 runs on the post-step-1 text.
     *  3. The cleanup pipeline on what remains, in its original
     *     order: currency-orphan rule (skipped with a product; a
     *     marker — "Rs", "Rs.", "₹", "روپے", "$", "£", "€", "元", or
     *     the symbol of the currency code configured in Settings —
     *     hugging the empty {price} slot is removed together with
     *     the placeholder), value substitution, bracket rule (any
     *     remaining [...] segment is a hint written for the SELLER,
     *     never text for the customer — removed entirely),
     *     leftover-token rule (ANY remaining {...} token, single- or
     *     multi-word, is stripped — a customer never sees a raw
     *     placeholder), tidy pass (collapse runs of spaces, pull
     *     punctuation back onto the word before it, trim the ends).
     */
    private fun fillTemplate(
        template: Template,
        labelValue: String,
        product: Product?
    ): String {
        val parts = labelValue.trim()
            .split(Regex("\\s*[—–]\\s*|\\s+-\\s+|\\s*,\\s*"))
        val name = parts.getOrNull(0)?.trim().orEmpty()
        val labelItem = parts.getOrNull(1)?.trim().orEmpty()
        val item = labelItem.ifEmpty { product?.name ?: "" }
        // v2.6 discount: a picked product with a 1–99% discount fills
        // {price} with the DISCOUNTED whole-unit price (integer math,
        // rounded down — the seller sees the exact figure on Home
        // before sending). No product, or 0%, behaves exactly as v2.5.
        val priceText = when {
            product == null -> ""
            discountPercent.value in 1..99 ->
                (product.price * (100 - discountPercent.value) / 100).toString()
            else -> product.price.toString()
        }
        var out = template.body

        // Step 1 — empty-price sentence fallbacks (v2.2 Rule 1,
        // verbatim). Skipped with a product: a real price makes the
        // original clauses correct, so they are kept and filled.
        if (priceText.isEmpty()) {
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
        }

        // Step 2 — sentence-drop pass (v2.4 task 3, product-aware).
        val keptSentences = splitSentences(out).filterNot { sentence ->
            sentence.contains("{payment method}") ||
                (priceText.isEmpty() && sentence.contains("{price}")) ||
                (item.isEmpty() && sentence.contains("{item}"))
        }
        val afterDrop = keptSentences.joinToString("")
        if (afterDrop.isNotBlank()) {
            out = afterDrop
        }

        // Step 3 — cleanup pipeline, original order. The orphan rule
        // runs only without a product: with a price it would eat the
        // real "Rs 500" it is supposed to protect.
        if (priceText.isEmpty()) {
            val configuredSymbol = CurrencyUtils.symbolForCode(settings.currencyCode)
            val markerPattern = listOf(
                "Rs\\.?", "₹", "روپے", "\\$", "£", "€", "元",
                Regex.escape(configuredSymbol)
            ).distinct().joinToString("|")
            out = out.replace(Regex("($markerPattern)\\s*\\{price\\}"), "")
            out = out.replace(Regex("\\{price\\}\\s*($markerPattern)"), "")
        }

        out = out
            .replace("{name}", name)
            .replace("{item}", item)
            .replace("{price}", priceText)
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

    /** The outgoing-text guarantee, shared by invoice text, product
     *  messages and custom scheduled text: no raw [...] seller hint
     *  and no raw {...} placeholder ever reaches a customer, and
     *  spacing/punctuation is tidied. These texts are built from
     *  seller-typed data (never from a template), so a seller who
     *  types "{price}" into a product description still cannot leak
     *  a placeholder to a customer. */
    fun sanitizeOutgoing(text: String): String {
        var out = text
        out = out.replace(Regex("\\[[^\\]]*\\]"), "")
        out = out.replace(Regex("\\{[^}]*\\}"), "")
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
            if (ch == '.' || ch == '!' || ch == '?' || ch == '۔' ||
                ch == '؟' || ch == '。' || ch == '！' || ch == '？'
            ) {
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

    // ---- v2.5: invoices ----

    /** Invoice display number from the max row id: "INV-001",
     *  "INV-002", … — deleting an invoice never reuses its number. */
    private suspend fun nextInvoiceNumber(): String {
        val next = (invoiceDao.maxId() ?: 0L) + 1
        return "INV-" + next.toString().padStart(3, '0')
    }

    fun createInvoice(
        customerName: String,
        customerNumber: String,
        recentNumber: String?,
        notes: String?,
        lines: List<Triple<String, Int, Long>>
    ) {
        viewModelScope.launch {
            val number = nextInvoiceNumber()
            val businessId = selectedBusinessId.value
            val id = invoiceDao.insert(
                Invoice(
                    businessId = businessId,
                    number = number,
                    customerName = customerName.trim(),
                    customerNumber = customerNumber.trim(),
                    recentNumber = recentNumber,
                    createdAt = System.currentTimeMillis(),
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
            invoiceDao.insertLines(
                lines.filter { it.first.isNotBlank() }.map { (desc, qty, amount) ->
                    InvoiceLine(
                        businessId = businessId,
                        invoiceId = id,
                        description = desc.trim(),
                        qty = qty.coerceAtLeast(1),
                        amount = amount.coerceAtLeast(0L)
                    )
                }
            )
        }
    }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            invoiceDao.deleteLines(invoice.id)
            invoiceDao.deleteById(invoice.id)
        }
    }

    /** The exact text sent for an invoice — built, then passed through
     *  the outgoing-text guarantee, so no seller-typed bracket or
     *  placeholder in a line description can ever reach a customer. */
    fun invoiceText(invoice: Invoice, lines: List<InvoiceLine>): String {
        val sb = StringBuilder()
        sb.append("Invoice ${invoice.number}\n")
        sb.append("Customer: ${invoice.customerName}\n")
        for (line in lines) {
            sb.append("• ${line.description} x${line.qty} — ")
            sb.append(CurrencyUtils.formatWithSymbol(line.amount, settings.currencyCode))
            sb.append("\n")
        }
        val total = lines.sumOf { it.amount }
        sb.append("Total: ")
        sb.append(CurrencyUtils.formatWithSymbol(total, settings.currencyCode))
        invoice.notes?.takeIf { it.isNotBlank() }?.let { sb.append("\n$it") }
        return sanitizeOutgoing(sb.toString())
    }

    /** Mark an invoice PAID and, when it is linked to a Recent, mark
     *  that customer PAID with the invoice total — Pulse sums
     *  paidAmountMinor, so the payment lands in "This Week" exactly
     *  like a Mark Paid from the Recents sheet. Unlinked invoices
     *  only change their own status. */
    fun markInvoicePaid(invoice: Invoice) {
        viewModelScope.launch {
            val lines = invoiceDao.getLines(invoice.id)
            val total = lines.sumOf { it.amount }
            invoiceDao.update(invoice.copy(status = InvoiceStatus.PAID))
            val recentNumber = invoice.recentNumber ?: return@launch
            val recent = recentDao.getByNumber(recentNumber) ?: return@launch
            val now = System.currentTimeMillis()
            recentDao.upsert(
                recent.copy(
                    status = RecentStatus.PAID,
                    statusChangedAt = now,
                    paidAmountMinor = total,
                    paidAt = now
                )
            )
        }
    }

    /** Send an invoice through the existing open-chat path (pre-filled,
     *  never auto-sent). Returns true when WhatsApp accepted it. */
    fun sendInvoice(pkg: String, invoice: Invoice, lines: List<InvoiceLine>): Boolean {
        return openChat(pkg, invoice.customerNumber, invoiceText(invoice, lines))
    }

    // ---- v2.5: product catalog ----

    fun addProduct(name: String, price: Long, description: String) {
        viewModelScope.launch {
            val nextOrder = (products.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            productDao.insert(
                Product(
                    businessId = selectedBusinessId.value,
                    name = name.trim(),
                    price = price.coerceAtLeast(0L),
                    description = description.trim(),
                    sortOrder = nextOrder
                )
            )
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch { productDao.update(product) }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch { productDao.deleteById(id) }
    }

    /** "Send product": name, price and description as one message,
     *  sanitized like every outgoing text. */
    fun productMessage(product: Product): String {
        val price = CurrencyUtils.formatWithSymbol(product.price, settings.currencyCode)
        val body = if (product.description.isBlank()) {
            "${product.name} — $price"
        } else {
            "${product.name} — $price\n${product.description}"
        }
        return sanitizeOutgoing(body)
    }

    // ---- v2.5: send media/files to unsaved numbers ----

    /**
     * Open WhatsApp (or WhatsApp Business) with a picked file attached
     * to the unsaved number's chat, plus [caption] as the message
     * text. The "jid" extra aims the share at exactly this number;
     * FLAG_GRANT_READ_URI_PERMISSION gives WhatsApp one-time read
     * access to the picked content Uri. Returns false on ANY failure
     * (not installed, no activity, SecurityException) — the caller
     * then falls back to opening the wa.me chat with the caption and
     * telling the seller to attach the file inside WhatsApp. The file
     * is NEVER claimed as sent from inside SellerChat: WhatsApp shows
     * it attached and the seller taps send there.
     */
    fun sendMedia(
        pkg: String,
        e164: String,
        uri: android.net.Uri,
        caption: String
    ): Boolean {
        val ctx = getApplication<Application>()
        val digits = e164.filter { it.isDigit() }
        val mime = try {
            ctx.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        } ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            setPackage(pkg)
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            putExtra("jid", "$digits@s.whatsapp.net")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

    // ---- v2.5: scheduled (reminder-to-send) messages ----

    /** Create a scheduled item and, when [withNotification] is true
     *  (the in-context permission ask already granted), arm the
     *  WorkManager job. Denied/unavailable → the row still saves and
     *  shows as due in-app at its time; nothing auto-sends either
     *  way. */
    fun scheduleMessage(
        customerName: String,
        customerNumber: String,
        pkg: String,
        text: String,
        at: Long,
        withNotification: Boolean
    ) {
        viewModelScope.launch {
            val id = scheduledDao.insert(
                ScheduledMessage(
                    customerName = customerName.trim(),
                    customerNumber = customerNumber.trim(),
                    appPackage = pkg,
                    text = sanitizeOutgoing(text),
                    scheduledAt = at
                )
            )
            if (withNotification) {
                ScheduledMessages.schedule(
                    getApplication(), id, customerNumber, pkg, customerName, text, at
                )
            }
        }
    }

    fun cancelScheduled(message: ScheduledMessage) {
        viewModelScope.launch {
            ScheduledMessages.cancel(getApplication(), message.id)
            scheduledDao.update(message.copy(status = ScheduledStatus.CANCELLED))
        }
    }

    fun markScheduledSent(message: ScheduledMessage) {
        viewModelScope.launch {
            scheduledDao.update(message.copy(status = ScheduledStatus.SENT))
        }
    }

    fun markScheduledSentById(id: Long) {
        viewModelScope.launch {
            val row = scheduledDao.getById(id) ?: return@launch
            if (row.status == ScheduledStatus.PENDING) {
                scheduledDao.update(row.copy(status = ScheduledStatus.SENT))
            }
        }
    }

    /** "Send now" from the list or the notification: opens WhatsApp
     *  with the approved text and marks the item SENT only when
     *  WhatsApp accepted the intent. */
    fun sendScheduledNow(message: ScheduledMessage): Boolean {
        val ok = openChat(message.appPackage, message.customerNumber, message.text)
        if (ok) markScheduledSent(message)
        return ok
    }

    // ---- v2.6: khata ledger ----

    /** The ONE balance rule, shared by the Money tab, the Today
     *  card and the statement text: udhaar − payments − advances.
     *  Positive = customer owes the seller; negative = the seller
     *  owes the customer goods (an advance); zero = settled. */
    fun khataBalance(entries: List<KhataEntry>): Long {
        var balance = 0L
        for (e in entries) {
            when (e.type) {
                KhataEntryType.UDHAAR -> balance += e.amount
                KhataEntryType.PAYMENT -> balance -= e.amount
                KhataEntryType.ADVANCE -> balance -= e.amount
            }
        }
        return balance
    }

    /** Human balance line: "To receive Rs 12,500", "Advance
     *  Rs 2,000" or "Settled" — the sign is always in words, never
     *  left for the seller to interpret from a minus sign.
     *  [currencyCode] defaults to Settings but Money screens pass
     *  the selected business's currency. */
    fun khataBalanceText(balance: Long, currencyCode: String = settings.currencyCode): String {
        val amount = CurrencyUtils.formatWithSymbol(
            kotlin.math.abs(balance), currencyCode
        )
        return when {
            balance > 0 -> "To receive $amount"
            balance < 0 -> "Advance $amount"
            else -> "Settled"
        }
    }

    /** Insert a khata entry; [onSaved] receives the saved row (with
     *  its id) so the caller can arm a due-date reminder for exactly
     *  this entry. */
    fun addKhataEntry(
        customerNumber: String,
        customerName: String,
        type: KhataEntryType,
        method: PaymentMethod?,
        amount: Long,
        note: String?,
        dueAt: Long?,
        onSaved: (KhataEntry) -> Unit = {}
    ) {
        viewModelScope.launch {
            val entry = KhataEntry(
                businessId = selectedBusinessId.value,
                customerNumber = customerNumber,
                customerName = customerName.trim(),
                type = type,
                method = if (type == KhataEntryType.PAYMENT) method else null,
                amount = amount.coerceAtLeast(0L),
                note = note?.trim()?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
                dueAt = if (type == KhataEntryType.UDHAAR) dueAt else null
            )
            val id = khataDao.insert(entry)
            onSaved(entry.copy(id = id))
        }
    }

    /** Delete a khata entry and cancel its due-date reminder, if one
     *  was armed — a deleted debt must never ring later. */
    fun deleteKhataEntry(entry: KhataEntry) {
        viewModelScope.launch {
            KhataReminders.cancel(getApplication(), entry.id)
            khataDao.deleteById(entry.id)
        }
    }

    /** Arm the due-date reminder for an already-saved entry.
     *  [withNotification] is the outcome of the in-context
     *  POST_NOTIFICATIONS ask: true → a real WorkManager job;
     *  false → the due date still shows in-app, only the doorbell
     *  is missing. Nothing auto-sends either way — the notification
     *  opens WhatsApp pre-filled and the seller taps send. */
    fun scheduleKhataReminder(entry: KhataEntry, withNotification: Boolean) {
        if (!withNotification) return
        val dueAt = entry.dueAt ?: return
        KhataReminders.schedule(
            getApplication(), entry.id, entry.customerNumber,
            entry.customerName, dueAt
        )
    }

    /** Deep-link text for a due-date notification tap: reads the
     *  LIVE ledger for the entry's shop. Returns null when the
     *  entry is gone or the balance is no longer positive — a debt
     *  settled after scheduling must never nag the customer. */
    suspend fun khataReminderTextForEntry(
        entryId: Long,
        number: String,
        name: String
    ): String? {
        val entry = khataDao.getById(entryId) ?: return null
        val entries = khataDao.getEntriesForCustomerInBusiness(number, entry.businessId)
        val balance = khataBalance(entries)
        if (balance <= 0) return null
        val currency = businessDao.getById(entry.businessId)?.currency
            ?: settings.currencyCode
        return khataReminderText(name, balance, currency)
    }

    /** The polite payment-reminder text for a due-date notification:
     *  built from the live balance, sanitized like every outgoing
     *  text. The seller still taps send in WhatsApp. */
    fun khataReminderText(
        customerName: String,
        balance: Long,
        currencyCode: String = settings.currencyCode
    ): String {
        val amount = CurrencyUtils.formatWithSymbol(balance, currencyCode)
        val who = customerName.ifBlank { "there" }
        return sanitizeOutgoing(
            "Hello $who, a gentle reminder: $amount is still due. " +
                "Please let me know when you can settle it. Thank you!"
        )
    }

    /** Clean per-customer statement for sharing: entries in
     *  date order (oldest first, like a paper khata), then the
     *  balance line. Shared through WhatsApp, sanitized. */
    fun khataStatementText(
        customerName: String,
        entries: List<KhataEntry>,
        currencyCode: String = settings.currencyCode
    ): String {
        val sb = StringBuilder()
        sb.append("Statement — ${customerName.ifBlank { "Customer" }}\n")
        val fmt = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
        for (e in entries.sortedBy { it.createdAt }) {
            val kind = when (e.type) {
                KhataEntryType.UDHAAR -> "Udhaar"
                KhataEntryType.PAYMENT -> "Payment" +
                    (e.method?.let { " (${it.name.lowercase()} )" } ?: "")
                KhataEntryType.ADVANCE -> "Advance"
            }
            sb.append("${fmt.format(java.util.Date(e.createdAt))} — $kind: ")
            sb.append(CurrencyUtils.formatWithSymbol(e.amount, currencyCode))
            e.note?.takeIf { it.isNotBlank() }?.let { sb.append(" ($it)") }
            sb.append("\n")
        }
        sb.append("Balance: ${khataBalanceText(khataBalance(entries), currencyCode)}")
        return sanitizeOutgoing(sb.toString())
    }

    // ---- v2.6: expenses (light log for the Today profit line) ----

    fun addExpense(amount: Long, note: String) {
        viewModelScope.launch {
            expenseDao.insert(
                Expense(
                    businessId = selectedBusinessId.value,
                    amount = amount.coerceAtLeast(0L),
                    note = note.trim(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { expenseDao.deleteById(expense.id) }
    }

    // ---- v2.6: multichannel open-chat (LINKS ONLY) ----

    /** Channel keys for the picker. TELEGRAM / MESSENGER open a
     *  username link; SMS uses the phone number itself. There is
     *  deliberately NO channel that reads or receives messages: a
     *  true inbox needs a backend + per-seller API setup and is
     *  out of scope (see BUILD_NOTES v2.6). */
    object Channels {
        const val TELEGRAM = "TELEGRAM"
        const val MESSENGER = "MESSENGER"
        const val SMS = "SMS"
    }

    suspend fun getChannelHandle(number: String, channel: String): ChannelHandle? =
        channelHandleDao.get(number, channel)

    fun saveChannelHandle(number: String, channel: String, username: String) {
        viewModelScope.launch {
            channelHandleDao.upsert(
                ChannelHandle(
                    customerNumber = number,
                    channel = channel,
                    username = username.trim().trimStart('@')
                )
            )
        }
    }

    /**
     * Open a non-WhatsApp channel for [number]. Telegram opens
     * t.me/<username>, Messenger m.me/<username> — both WITHOUT a
     * pre-filled message (those link schemes do not carry seller
     * text; the UI says so plainly). SMS opens the phone's own
     * messaging app WITH the text pre-filled (sms_body). Returns
     * false when there is no username (Telegram/Messenger) or no
     * app can handle the intent — the caller falls back honestly
     * to the wa.me chat and says so. Nothing here claims a message
     * was sent from inside SellerChat, and no reply ever arrives
     * in SellerChat.
     */
    fun openChannel(channel: String, number: String, username: String?, text: String): Boolean {
        val ctx = getApplication<Application>()
        val intent = when (channel) {
            Channels.TELEGRAM -> {
                val user = username?.trim()?.trimStart('@')?.ifBlank { null } ?: return false
                Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$user"))
            }
            Channels.MESSENGER -> {
                val user = username?.trim()?.trimStart('@')?.ifBlank { null } ?: return false
                Intent(Intent.ACTION_VIEW, Uri.parse("https://m.me/$user"))
            }
            Channels.SMS -> {
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${number.filter { it.isDigit() }}")).apply {
                    if (text.isNotBlank()) putExtra("sms_body", text)
                }
            }
            else -> return false
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    // ---- v2.6: catalog card + ad link ("Share my shop") ----

    /** Shareable shop text: active catalog products with prices,
     *  then the seller's click-to-chat link. [sellerNumber] is the
     *  seller's own E.164 digits (blank = no link line — the card
     *  still shares the products). [source] is an optional
     *  "where did you see us" tag (e.g. "Instagram") folded into
     *  the pre-filled ad message so the seller learns which ad
     *  brought the customer. No tracking, no shortener, no backend
     *  — a plain wa.me link. */
    fun catalogCardText(sellerNumber: String, source: String?): String {
        val sb = StringBuilder()
        sb.append("Our products:\n")
        val active = products.value.filter { it.active }
        if (active.isEmpty()) {
            sb.append("Message us for our latest price list.\n")
        } else {
            for (p in active) {
                sb.append("• ${p.name} — ")
                sb.append(CurrencyUtils.formatWithSymbol(p.price, settings.currencyCode))
                sb.append("\n")
            }
        }
        val digits = sellerNumber.filter { it.isDigit() }
        if (digits.isNotBlank()) {
            val adText = if (source.isNullOrBlank()) {
                "Hi! I saw your ad."
            } else {
                "Hi! I saw your ad on $source."
            }
            sb.append("Order here: ${PhoneUtils.waMeLink(digits, adText)}")
        }
        return sanitizeOutgoing(sb.toString())
    }

    // ---- v2.5: assisted greeting / away ----

    /** True when away mode is on AND the current hour is outside the
     *  configured business hours. Overnight ranges (open > close)
     *  invert the test: open 20 → 2 means closed 2 → 20. */
    fun isAwayNow(hourNow: Int = java.util.Calendar.getInstance()
        .get(java.util.Calendar.HOUR_OF_DAY)): Boolean {
        if (!settings.awayEnabled) return false
        val open = settings.awayOpenHour
        val close = settings.awayCloseHour
        if (open == close) return false
        return if (open < close) {
            hourNow < open || hourNow >= close
        } else {
            hourNow >= close && hourNow < open
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
