package com.fahad1005.sellerchat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** v2.5 business tools. Money follows the Mark Paid / Pulse convention
 *  exactly: whole units of the seller's currency stored as Long
 *  (e.g. 12500 = Rs 12,500) — deliberately NOT minor/paisa units, so
 *  invoice totals feed Pulse sums without conversion. The column name
 *  `paidAmountMinor` on Recents is the same convention under its
 *  historical name; invoice/product amounts use the plain name. */

/** Invoice status. UNPAID invoices appear in the "Unpaid" filter and in
 *  the "Who owes me" view; PAID invoices move to "Paid". */
enum class InvoiceStatus { UNPAID, PAID }

/** One invoice. [number] is the display sequence ("INV-001") computed
 *  from the max row id at creation — never reused after a delete, so
 *  numbers stay unique even when an invoice is removed. [recentNumber]
 *  links the invoice to a Recent row (its numberE164) when the invoice
 *  was created for an existing customer; null = typed-in customer. */
@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** v2.6: which shop this invoice belongs to. The defaultValue
     *  matches migration 3 → 4's ADD COLUMN DEFAULT exactly. */
    @ColumnInfo(defaultValue = "default")
    val businessId: String = "default",
    val number: String,
    val customerName: String,
    val customerNumber: String,
    val recentNumber: String? = null,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "UNPAID")
    val status: InvoiceStatus = InvoiceStatus.UNPAID,
    val notes: String? = null
)

/** One line on an invoice. [amount] is the line TOTAL in whole
 *  currency units (qty already applied by the seller at entry time —
 *  the form shows qty × unit price and stores the result, so totals
 *  are a plain sum, never a recomputation). */
@Entity(tableName = "invoice_lines")
data class InvoiceLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** v2.6: inherits the parent invoice's shop at creation. */
    @ColumnInfo(defaultValue = "default")
    val businessId: String = "default",
    val invoiceId: Long,
    val description: String,
    val qty: Int,
    val amount: Long
)

/** A product in the seller's own price list. [active] = false hides
 *  the product from the Home picker without deleting it (and without
 *  losing its sort position). [sortOrder] is the seller's manual
 *  order; new products append at the end. */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** v2.6: which shop's price list this product belongs to. */
    @ColumnInfo(defaultValue = "default")
    val businessId: String = "default",
    val name: String,
    /** Whole currency units, same convention as invoices. */
    val price: Long,
    val description: String = "",
    @ColumnInfo(defaultValue = "1")
    val active: Boolean = true,
    val sortOrder: Int
)

/** Reminder-to-send state. PENDING = waiting for its time (or overdue
 *  and still unsent — the list shows overdue items as due, it never
 *  auto-sends); SENT = the seller tapped Send (in-app or from the
 *  notification) and WhatsApp opened; CANCELLED = the seller removed
 *  it. The WorkManager job is keyed by the row id. */
enum class ScheduledStatus { PENDING, SENT, CANCELLED }

/** A scheduled message. [text] is the FINAL text to open in WhatsApp
 *  (template already filled at creation time through the shared fill
 *  function, or the seller's custom text, sanitized) — at fire time
 *  the app does not re-fill, so what the seller approved is exactly
 *  what opens. [appPackage] is the WhatsApp variant chosen at
 *  creation. */
@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerNumber: String,
    val appPackage: String,
    val text: String,
    val scheduledAt: Long,
    @ColumnInfo(defaultValue = "PENDING")
    val status: ScheduledStatus = ScheduledStatus.PENDING
)
