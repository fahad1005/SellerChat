package com.fahad1005.sellerchat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Template::class, Recent::class,
        Invoice::class, InvoiceLine::class,
        Product::class, ScheduledMessage::class,
        Business::class,
        KhataEntry::class, ChannelHandle::class, Expense::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun recentDao(): RecentDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun productDao(): ProductDao
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun businessDao(): BusinessDao
    abstract fun khataDao(): KhataDao
    abstract fun channelHandleDao(): ChannelHandleDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sellerchat.db"
                )
                    // v1 → v2 is ALTER-only and v2 → v3 / v3 → v4 are
                    // CREATE-only (see Migrations.kt): a user's data
                    // MUST survive the update, so no destructive
                    // fallback is registered.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
        }
    }
}
