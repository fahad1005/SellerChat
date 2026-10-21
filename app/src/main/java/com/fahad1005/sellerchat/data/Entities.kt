package com.fahad1005.sellerchat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Template language tag. UR is Urdu script; ROMAN_UR is Roman Urdu. */
enum class TemplateLanguage { EN, UR, ROMAN_UR }

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val language: TemplateLanguage,
    val sortOrder: Int
)

/** One row per customer number: re-messaging a number UPDATES its row
 *  (spec §6 duplicate rule) instead of creating a second entry. */
@Entity(tableName = "recents")
data class Recent(
    @PrimaryKey val numberE164: String,
    val label: String,
    val appPackage: String,
    val templateTitle: String?,
    val timestamp: Long
)
