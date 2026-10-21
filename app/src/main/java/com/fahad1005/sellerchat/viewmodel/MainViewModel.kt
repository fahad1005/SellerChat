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
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.data.TemplateLanguage
import com.fahad1005.sellerchat.notify.FollowUps
import com.fahad1005.sellerchat.util.PhoneUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        val saved = Countries.byIso(settings.countryIso)
        if (saved != null) return saved
        val ctx = getApplication<Application>()
        val simIso = try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.simCountryIso
        } catch (e: Exception) {
            null
        }
        val detected = Countries.byIso(simIso)
            ?: Countries.byIso(java.util.Locale.getDefault().country)
            ?: Countries.fallback()
        settings.countryIso = detected.iso
        return detected
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
        val e164 = PhoneUtils.normalize("+92 300 1234567", pk) ?: return false
        val text = template?.let { PhoneUtils.applyTemplate(it.body, "") } ?: ""
        return openChat(pkg, e164, text)
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
