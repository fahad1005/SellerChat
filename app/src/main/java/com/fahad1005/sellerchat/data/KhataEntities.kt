package com.fahad1005.sellerchat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** v2.6 multi-business. One app, several shops (e.g. a clothing
 *  shop and a food business), switched in the Money tab header.
 *
 *  SCOPING RULE (the whole system in one line): money objects are
 *  per business — khata entries, invoices, invoice lines, products,
 *  expenses carry a businessId. Customers (Recents), templates and
 *  scheduled messages stay GLOBAL: one person can buy from both
 *  shops, and a reminder is about a person, not a shop.
 *
 *  The default business has id "default" and name "My Business".
 *  Migration 3 → 4 backfills businessId = 'default' on every
 *  existing invoice / line / product row, so a v2.5 user opens v2.6
 *  and finds all their data under "My Business" with zero action.
 *  New rows stamp the currently selected business at creation. */

/** One shop. [currency] is that shop's display currency (ISO code);
 *  the default business adopts the seller's Settings currency on
 *  first v2.6 launch (see MainViewModel.ensureDefaultBusiness) —
 *  the migration itself cannot read SharedPreferences, so it seeds
 *  'USD' with createdAt = 0 as an "adopt me" marker. */
@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey val id: String,
    val name: String,
    val currency: String,
    val createdAt: Long
)

/** v2.6 khata ledger. Money follows the Mark Paid / Pulse convention
 *  exactly: whole units of the seller's currency stored as Long
 *  (e.g. 12500 = Rs 12,500) — deliberately NOT minor/paisa units.
 *
 *  Balance rule (one rule, everywhere): balance = udhaar − payments
 *  − advances. Positive = the customer owes the seller ("To
 *  receive"); negative = the seller owes the customer goods
 *  ("Advance"); zero = "Settled". Partial payments are just more
 *  PAYMENT rows — a payment never overwrites anything.
 *
 *  Deliberately NOT here: interest calculation, multi-business
 *  books, cloud sync, backup export (see BUILD_NOTES v2.6). */

/** Khata entry kind. UDHAAR = credit given (customer owes more);
 *  PAYMENT = money received (balance falls); ADVANCE = customer
 *  paid extra (seller owes goods — balance falls past zero). */
enum class KhataEntryType { UDHAAR, PAYMENT, ADVANCE }

/** How a payment arrived. Stored only on PAYMENT rows; null on
 *  UDHAAR / ADVANCE rows. */
enum class PaymentMethod { CASH, JAZZCASH, EASYPEPAISA, BANK }

@Entity(tableName = "khata_entries")
data class KhataEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Which shop this entry belongs to (see scoping rule above).
     *  The defaultValue matches migration 3 → 4's ADD COLUMN
     *  DEFAULT exactly — Room validates defaults on first open. */
    @ColumnInfo(defaultValue = "default")
    val businessId: String = "default",
    /** The customer's numberE164 — the ledger key, so the khata
     *  follows the customer even if their Recent row is renamed. */
    val customerNumber: String,
    val customerName: String,
    val type: KhataEntryType,
    val method: PaymentMethod? = null,
    /** Whole currency units, always positive; the sign comes from
     *  [type] in the balance rule, never from this value. */
    val amount: Long,
    val note: String? = null,
    val createdAt: Long,
    /** Optional payment-due date for UDHAAR rows (epoch ms); null =
     *  no due date, no reminder. Past-due rows surface in
     *  "Follow up today" while the balance is still positive. */
    val dueAt: Long? = null
)

/** A seller-typed username for a non-WhatsApp channel, keyed by the
 *  customer's number. Usernames are NOT phone numbers, so they
 *  cannot live on the Recent row — this table holds at most one
 *  username per (customer, channel). */
@Entity(
    tableName = "channel_handles",
    indices = [Index(value = ["customerNumber", "channel"], unique = true)]
)
data class ChannelHandle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerNumber: String,
    /** "TELEGRAM" or "MESSENGER" — the link channels. SMS needs no
     *  username (it uses the phone number itself). */
    val channel: String,
    val username: String
)

/** One business expense. Expenses feed the Money tab's Today
 *  profit line (sales − expenses) and nothing else — this is a
 *  light log, not accounting: no categories, no receipts. */
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Which shop this expense belongs to (scoping rule above). */
    @ColumnInfo(defaultValue = "default")
    val businessId: String = "default",
    /** Whole currency units. */
    val amount: Long,
    val note: String,
    val createdAt: Long
)
