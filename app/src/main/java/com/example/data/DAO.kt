package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT COUNT(*) FROM contacts")
    suspend fun getContactCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Update
    suspend fun updateContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)

    @Query("SELECT * FROM alert_logs ORDER BY timestamp DESC")
    fun getAllAlertLogsFlow(): Flow<List<AlertLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertLog(log: AlertLog): Long

    @Update
    suspend fun updateAlertLog(log: AlertLog)

    @Query("DELETE FROM alert_logs")
    suspend fun clearAlertLogs()

    @Query("SELECT * FROM safety_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SafetySetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SafetySetting)
}
