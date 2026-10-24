package com.fahad1005.sellerchat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Template language tag. The enum NAMES of the v1 values (EN, UR,
 *  ROMAN_UR) are stored verbatim in the v1 database — do not rename
 *  them or existing libraries break. v2 adds HI / TA / TE / MR;
 *  v2.3 adds FR / ES / AR / ZH. */
enum class TemplateLanguage { EN, UR, ROMAN_UR, HI, TA, TE, MR, FR, ES, AR, ZH }

/** What a template is FOR. Seeds are tagged; user templates default to
 *  OTHER. Shown as a small tag on template rows (v2 spec §3). */
enum class TemplatePurpose { PRICE, ORDER, PAYMENT, DELIVERY, OTHER }

/** Where a customer sits in the seller's pipeline. The Recents status
 *  chip cycles INQUIRY → ORDERED → PACKED → SHIPPED → DELIVERED →
 *  PAID → INQUIRY (v2.6 inserts PACKED and DELIVERED; PAID keeps its
 *  amount prompt). Old rows stored INQUIRY / ORDERED / PAID /
 *  SHIPPED by name, so they keep working — enum values are only
 *  ever added, never renamed or removed. */
enum class RecentStatus { INQUIRY, ORDERED, PACKED, SHIPPED, DELIVERED, PAID }

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val language: TemplateLanguage,
    val sortOrder: Int,
    /** v2 column (Migration 1→2, default 'OTHER'). */
    @ColumnInfo(defaultValue = "OTHER")
    val purpose: TemplatePurpose = TemplatePurpose.OTHER
)

/** One row per customer number: re-messaging a number UPDATES its row
 *  (spec §6 duplicate rule) instead of creating a second entry. The v2
 *  CRM fields (status / memo / follow-up / payment) live on the same
 *  row and must be preserved when a number is re-messaged — see
 *  MainViewModel.recordRecent. */
@Entity(tableName = "recents")
data class Recent(
    @PrimaryKey val numberE164: String,
    val label: String,
    val appPackage: String,
    val templateTitle: String?,
    val timestamp: Long,
    /** v2 columns below (Migration 1→2). */
    @ColumnInfo(defaultValue = "INQUIRY")
    val status: RecentStatus = RecentStatus.INQUIRY,
    val memo: String? = null,
    /** When [status] last changed — the waiting clock counts from here. */
    @ColumnInfo(defaultValue = "0")
    val statusChangedAt: Long = 0L,
    /** Scheduled follow-up time; null = no follow-up set. */
    val followUpAt: Long? = null,
    /** True when a real system notification was scheduled (WorkManager);
     *  false = in-app reminder only (permission denied / unavailable). */
    @ColumnInfo(defaultValue = "0")
    val followUpNotified: Boolean = false,
    /** Payment amount in WHOLE units of the user's currency (e.g. 12500
     *  = Rs 12,500) — deliberately whole units, not minor/paisa, to keep
     *  the amount prompt and the Pulse sum simple and exact. The column
     *  name follows the v2 spec; the unit choice is documented in
     *  BUILD_NOTES. */
    val paidAmountMinor: Long? = null,
    /** When the payment was marked — the Pulse "This Week" sum keys on it. */
    val paidAt: Long? = null
)
