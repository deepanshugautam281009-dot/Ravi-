package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val relationship: String, // "Parent", "Relative", "Friend"
    val isPrimary: Boolean = true
) : Serializable

@Entity(tableName = "alert_logs")
data class AlertLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val escalatedToPolice: Boolean = false,
    val latitude: Double,
    val longitude: Double,
    val status: String = "ACTIVE" // "ACTIVE", "ESCALATED", "RESOLVED"
) : Serializable

@Entity(tableName = "safety_settings")
data class SafetySetting(
    @PrimaryKey val key: String,
    val value: String
)
