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
