package com.fahad1005.sellerchat.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 1 → 2 (SellerChat v2.0). NON-destructive by design: the v2
 * acceptance criteria require a v1 user's templates, recents and labels
 * to survive the update untouched, so every change is an ALTER TABLE
 * ADD COLUMN with a default, plus two data backfills:
 *
 *  1. recents.statusChangedAt is backfilled from the row's own
 *     timestamp, so the waiting clock starts from the last message
 *     time instead of the epoch.
 *  2. The 12 v1 seed templates get their purpose tag by exact title
 *     match (user-edited titles simply stay OTHER — no guessing).
 *
 * Column types/defaults must match the entity @ColumnInfo defaults
 * EXACTLY or Room's schema validation fails on first open:
 * enums are stored as TEXT (their name), booleans as INTEGER.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ---- templates: purpose group ----
        db.execSQL(
            "ALTER TABLE templates ADD COLUMN purpose TEXT NOT NULL DEFAULT 'OTHER'"
        )
        // Purpose backfill for the untouched v1 seeds (exact seed titles).
        val purposeByTitle = listOf(
            "Price inquiry reply" to "PRICE",
            "Order confirmation" to "ORDER",
            "Payment details" to "PAYMENT",
            "Delivery update" to "DELIVERY",
            "قیمت کا جواب" to "PRICE",
            "آرڈر کنفرم" to "ORDER",
            "ادائیگی کی تفصیل" to "PAYMENT",
            "Price reply (Roman)" to "PRICE",
            "Order confirm (Roman)" to "ORDER",
            "Payment details (Roman)" to "PAYMENT"
        )
        for ((title, purpose) in purposeByTitle) {
            db.execSQL(
                "UPDATE templates SET purpose = '$purpose' WHERE title = ?",
                arrayOf<Any>(title)
            )
        }

        // ---- recents: the mini-CRM fields ----
        db.execSQL(
            "ALTER TABLE recents ADD COLUMN status TEXT NOT NULL DEFAULT 'INQUIRY'"
        )
        db.execSQL("ALTER TABLE recents ADD COLUMN memo TEXT")
        db.execSQL(
            "ALTER TABLE recents ADD COLUMN statusChangedAt INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "UPDATE recents SET statusChangedAt = timestamp WHERE statusChangedAt = 0"
        )
        db.execSQL("ALTER TABLE recents ADD COLUMN followUpAt INTEGER")
        db.execSQL(
            "ALTER TABLE recents ADD COLUMN followUpNotified INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL("ALTER TABLE recents ADD COLUMN paidAmountMinor INTEGER")
        db.execSQL("ALTER TABLE recents ADD COLUMN paidAt INTEGER")
    }
}
