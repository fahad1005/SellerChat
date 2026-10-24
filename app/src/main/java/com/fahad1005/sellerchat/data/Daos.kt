package com.fahad1005.sellerchat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Template>>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Query("SELECT * FROM templates WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): Template?

    @Query("SELECT * FROM templates ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Template>

    @Insert
    suspend fun insert(template: Template): Long

    @Insert
    suspend fun insertAll(templates: List<Template>)

    @Update
    suspend fun update(template: Template)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Recent>>

    /** v2 micro-fix (founder's v1.0 phone test): search matches the
     *  template title too — searching "order" must find the entry that
     *  used the "Order confirm" template, not just labels/numbers. */
    @Query(
        "SELECT * FROM recents WHERE label LIKE '%' || :query || '%' " +
            "OR numberE164 LIKE '%' || :query || '%' " +
            "OR templateTitle LIKE '%' || :query || '%' ORDER BY timestamp DESC"
    )
    fun observeFiltered(query: String): Flow<List<Recent>>

    @Query("SELECT * FROM recents WHERE numberE164 = :number LIMIT 1")
    suspend fun getByNumber(number: String): Recent?

    /** Insert-or-replace on the number primary key = the duplicate rule. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recent: Recent)

    @Query("DELETE FROM recents WHERE numberE164 = :number")
    suspend fun deleteByNumber(number: String)

    @Query("DELETE FROM recents")
    suspend fun clearAll()
}

// ---- v2.5 business tools ----

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun observeByBusiness(businessId: String): Flow<List<Invoice>>

    @Query("SELECT COUNT(*) FROM invoices WHERE businessId = :businessId")
    suspend fun countForBusiness(businessId: String): Int

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY id ASC")
    fun observeLines(invoiceId: Long): Flow<List<InvoiceLine>>

    @Query("SELECT * FROM invoice_lines WHERE invoiceId = :invoiceId ORDER BY id ASC")
    suspend fun getLines(invoiceId: Long): List<InvoiceLine>

    @Query("SELECT MAX(id) FROM invoices")
    suspend fun maxId(): Long?

    @Insert
    suspend fun insert(invoice: Invoice): Long

    @Insert
    suspend fun insertLines(lines: List<InvoiceLine>)

    @Update
    suspend fun update(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM invoice_lines WHERE invoiceId = :invoiceId")
    suspend fun deleteLines(invoiceId: Long)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE businessId = :businessId ORDER BY sortOrder ASC, id ASC")
    fun observeByBusiness(businessId: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId")
    suspend fun countForBusiness(businessId: String): Int

    @Query("SELECT * FROM products ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// ---- v2.6 multi-business + khata ledger, channel handles, expenses ----

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<Business>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Business?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(business: Business)

    @Update
    suspend fun update(business: Business)

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface KhataDao {
    @Query("SELECT * FROM khata_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KhataEntry>>

    /** Business-scoped list for the Money tab. */
    @Query(
        "SELECT * FROM khata_entries WHERE businessId = :businessId " +
            "ORDER BY createdAt DESC"
    )
    fun observeByBusiness(businessId: String): Flow<List<KhataEntry>>

    @Query("SELECT * FROM khata_entries WHERE customerNumber = :number ORDER BY createdAt DESC")
    fun observeForCustomer(number: String): Flow<List<KhataEntry>>

    /** One customer's ledger inside one shop (customers are
     *  global, entries are per business). */
    @Query(
        "SELECT * FROM khata_entries WHERE customerNumber = :number " +
            "AND businessId = :businessId ORDER BY createdAt DESC"
    )
    fun observeForCustomerInBusiness(number: String, businessId: String): Flow<List<KhataEntry>>

    @Query("SELECT * FROM khata_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): KhataEntry?

    /** One-shot read for the due-date deep link: the reminder text
     *  is built from the LIVE ledger at tap time, so a payment made
     *  after scheduling is reflected (and a settled debt sends
     *  nothing at all). */
    @Query(
        "SELECT * FROM khata_entries WHERE customerNumber = :number " +
            "AND businessId = :businessId ORDER BY createdAt DESC"
    )
    suspend fun getEntriesForCustomerInBusiness(
        number: String,
        businessId: String
    ): List<KhataEntry>

    @Query("SELECT COUNT(*) FROM khata_entries WHERE businessId = :businessId")
    suspend fun countForBusiness(businessId: String): Int

    @Insert
    suspend fun insert(entry: KhataEntry): Long

    @Query("DELETE FROM khata_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ChannelHandleDao {
    @Query(
        "SELECT * FROM channel_handles WHERE customerNumber = :number " +
            "AND channel = :channel LIMIT 1"
    )
    suspend fun get(number: String, channel: String): ChannelHandle?

    /** One username per (customer, channel): the unique index makes
     *  a re-save replace the old username instead of duplicating. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(handle: ChannelHandle)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun observeByBusiness(businessId: String): Flow<List<Expense>>

    @Query("SELECT COUNT(*) FROM expenses WHERE businessId = :businessId")
    suspend fun countForBusiness(businessId: String): Int

    @Insert
    suspend fun insert(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ScheduledMessageDao {
    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledAt ASC")
    fun observeAll(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduledMessage?

    @Insert
    suspend fun insert(message: ScheduledMessage): Long

    @Update
    suspend fun update(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
