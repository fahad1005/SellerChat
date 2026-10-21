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
import com.fahad1005.sellerchat.data.SeedData
import com.fahad1005.sellerchat.data.SettingsStore
import com.fahad1005.sellerchat.data.Template
import com.fahad1005.sellerchat.util.PhoneUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
            if (templateDao.count() == 0) {
                templateDao.insertAll(SeedData.templates())
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

    fun recordRecent(e164: String, label: String, pkg: String, templateTitle: String?) {
        viewModelScope.launch {
            recentDao.upsert(
                Recent(
                    numberE164 = e164,
                    label = label,
                    appPackage = pkg,
                    templateTitle = templateTitle,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // ---- Template actions ----

    fun addTemplate(title: String, body: String, language: com.fahad1005.sellerchat.data.TemplateLanguage) {
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
        viewModelScope.launch { recentDao.deleteByNumber(number) }
    }

    fun clearRecents() {
        viewModelScope.launch { recentDao.clearAll() }
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
