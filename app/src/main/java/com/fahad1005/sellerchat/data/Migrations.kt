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

/**
 * Room 3 → 4 (SellerChat v2.6). Two kinds of change, both
 * non-destructive — no table is dropped, renamed or rebuilt, and
 * no row is deleted or rewritten:
 *
 *  1. CREATE-only new tables: businesses, khata_entries,
 *     channel_handles, expenses.
 *  2. ADD COLUMN ONLY on three existing tables: invoices,
 *     invoice_lines and products each gain businessId TEXT NOT
 *     NULL DEFAULT 'default'. This is the one relaxation of the
 *     old "never alter a table" rule, and it is safe for the same
 *     reason the v2.0 migration was: ADD COLUMN with a default
 *     backfills every existing row without touching any other
 *     column, so a v2.5 user's invoices and catalog land under
 *     the "default" business untouched.
 *
 * Column types/defaults must match the entities EXACTLY or Room's
 * schema validation fails on first open: enums are TEXT (name),
 * Long is INTEGER, strings TEXT, nullable columns have no NOT
 * NULL. Columns whose entity declares
 * @ColumnInfo(defaultValue = "default") — businessId on all five
 * money tables — carry DEFAULT 'default' in the SQL below. The
 * unique index on channel_handles is part of the schema — Room
 * validates indices too, and its generated name is
 * index_channel_handles_customerNumber_channel.
 *
 * The businesses seed row ('default', 'My Business', 'USD', 0)
 * is a migration marker, not a real currency choice: the
 * migration cannot read SharedPreferences, so first v2.6 launch
 * adopts the seller's Settings currency onto it (see
 * MainViewModel.ensureDefaultBusiness — createdAt == 0 means
 * "adopt me"). Fresh installs never run this migration; Room
 * creates the tables from the entities and the VM inserts the
 * default business with the Settings currency directly.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ---- multi-business: the businesses table + seed row ----
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `businesses` (" +
                "`id` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`currency` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT OR IGNORE INTO `businesses` " +
                "(`id`, `name`, `currency`, `createdAt`) VALUES " +
                "('default', 'My Business', 'USD', 0)"
        )
        // ---- money tables gain their shop (backfilled 'default') ----
        db.execSQL(
            "ALTER TABLE invoices ADD COLUMN businessId TEXT NOT NULL DEFAULT 'default'"
        )
        db.execSQL(
            "ALTER TABLE invoice_lines ADD COLUMN businessId TEXT NOT NULL DEFAULT 'default'"
        )
        db.execSQL(
            "ALTER TABLE products ADD COLUMN businessId TEXT NOT NULL DEFAULT 'default'"
        )
        // ---- brand-new tables (businessId at creation) ----
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `khata_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`businessId` TEXT NOT NULL DEFAULT 'default', " +
                "`customerNumber` TEXT NOT NULL, " +
                "`customerName` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`method` TEXT, " +
                "`amount` INTEGER NOT NULL, " +
                "`note` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`dueAt` INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `channel_handles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`customerNumber` TEXT NOT NULL, " +
                "`channel` TEXT NOT NULL, " +
                "`username` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_channel_handles_customerNumber_channel` ON " +
                "`channel_handles` (`customerNumber`, `channel`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`businessId` TEXT NOT NULL DEFAULT 'default', " +
                "`amount` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

/**
 * Room 2 → 3 (SellerChat v2.5). CREATE-only: the three new areas
 * (invoices, catalog, scheduled messages) are brand-new tables, so
 * no existing row is touched and no ALTER is needed. Column
 * types/defaults must match BusinessEntities.kt EXACTLY or Room's
 * schema validation fails on first open: enums are TEXT (name),
 * booleans are INTEGER (0/1), Long/Int are INTEGER, strings TEXT.
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoices` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`number` TEXT NOT NULL, " +
                "`customerName` TEXT NOT NULL, " +
                "`customerNumber` TEXT NOT NULL, " +
                "`recentNumber` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL DEFAULT 'UNPAID', " +
                "`notes` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `invoice_lines` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`invoiceId` INTEGER NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`qty` INTEGER NOT NULL, " +
                "`amount` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`price` INTEGER NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`active` INTEGER NOT NULL DEFAULT 1, " +
                "`sortOrder` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scheduled_messages` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`customerName` TEXT NOT NULL, " +
                "`customerNumber` TEXT NOT NULL, " +
                "`appPackage` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`scheduledAt` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL DEFAULT 'PENDING')"
        )
    }
}
