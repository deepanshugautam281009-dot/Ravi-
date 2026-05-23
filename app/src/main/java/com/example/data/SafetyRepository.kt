package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SafetyRepository(private val safetyDao: SafetyDao) {
    val allContacts: Flow<List<Contact>> = safetyDao.getAllContactsFlow()
    val allAlertLogs: Flow<List<AlertLog>> = safetyDao.getAllAlertLogsFlow()

    suspend fun insertContact(contact: Contact) {
        safetyDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) {
        safetyDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        safetyDao.deleteContact(contact)
    }

    suspend fun deleteContactById(id: Int) {
        safetyDao.deleteContactById(id)
    }

    suspend fun insertAlertLog(log: AlertLog): Long {
        return safetyDao.insertAlertLog(log)
    }

    suspend fun updateAlertLog(log: AlertLog) {
        safetyDao.updateAlertLog(log)
    }

    suspend fun clearAlertLogs() {
        safetyDao.clearAlertLogs()
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return safetyDao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        safetyDao.insertSetting(SafetySetting(key, value))
    }

    suspend fun seedDefaultDataIfNecessary() {
        val count = safetyDao.getContactCount()
        if (count == 0) {
            val defaults = listOf(
                Contact(name = "Sarah Carter (Mom)", phone = "+1 (555) 0192", relationship = "Parent", isPrimary = true),
                Contact(name = "James Carter (Dad)", phone = "+1 (555) 0148", relationship = "Parent", isPrimary = true),
                Contact(name = "Uncle Michael", phone = "+1 (555) 0122", relationship = "Relative", isPrimary = true),
                Contact(name = "Emma Watson (Friend)", phone = "+1 (555) 0177", relationship = "Friend", isPrimary = true)
            )
            for (contact in defaults) {
                safetyDao.insertContact(contact)
            }
        }
    }
}
