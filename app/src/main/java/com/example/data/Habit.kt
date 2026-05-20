package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val icon: String, // String representation of an emoji (e.g., "🏃‍♂️", "📖")
    val category: String, // e.g., "صحة", "تطوير الذات", "عبادة", "عمل"
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: String? = null, // e.g., "08:30"
    val isArchived: Boolean = false
)
