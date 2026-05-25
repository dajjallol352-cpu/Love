package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lua_scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val category: String, // e.g. "Delta", "Codex", "Universal", "Custom"
    val description: String,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
