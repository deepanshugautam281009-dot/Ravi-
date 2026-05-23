package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

sealed interface SosState {
    object Idle : SosState
    data class Active(
        val alertId: Int,
        val secondsRemaining: Int,
        val e2eeSessionKey: String,
        val logs: List<SosLog>,
        val currentLatitude: Double,
        val currentLongitude: Double,
        val trackingLatency: Int // in milliseconds (e.g. 12ms)
    ) : SosState
    data class PoliceEscalated(
        val alertId: Int,
        val e2eeSessionKey: String,
        val logs: List<SosLog>,
        val currentLatitude: Double,
        val currentLongitude: Double,
        val dispatchUnit: String,
        val officerName: String,
        val dispatchStatus: String, // "ASSIGNED", "EN_ROUTE", "ARRIVING", "SECURED"
        val dispatchEtaSeconds: Int,
        val trackingLatency: Int
    ) : SosState
}

data class SosLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isEncrypted: Boolean = true,
    val level: LogLevel = LogLevel.INFO
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, CRITICAL
}

class SafetyViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SafetyRepository(database.safetyDao)

    // Database Flows
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertHistory: StateFlow<List<AlertLog>> = repository.allAlertLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _sosState = MutableStateFlow<SosState>(SosState.Idle)
    val sosState: StateFlow<SosState> = _sosState.asStateFlow()

    private val _customMessage = MutableStateFlow("Emergency! Standard panic protocol activated. I am walking home and notice someone following me. Track my encrypted live location immediately!")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    private val _isE2eeEnabled = MutableStateFlow(true)
    val isE2eeEnabled: StateFlow<Boolean> = _isE2eeEnabled.asStateFlow()

    private val _selectedGpsPrecision = MutableStateFlow("HIGH (Multi-GNSS Dual Band)")
    val selectedGpsPrecision: StateFlow<String> = _selectedGpsPrecision.asStateFlow()

    // Internal simulation jobs
    private var alertJob: Job? = null
    private var gpsUpdateJob: Job? = null

    // Base mock coordinates: SF Financial District
    private var baseLat = 37.7895
    private var baseLng = -122.4014

    init {
        // Run seeding in ViewModel Scope
        viewModelScope.launch {
            repository.seedDefaultDataIfNecessary()
            // Load custom settings if any
            _customMessage.value = repository.getSetting("custom_message", _customMessage.value)
            _isE2eeEnabled.value = repository.getSetting("is_e2ee", "true").toBoolean()
            _selectedGpsPrecision.value = repository.getSetting("gps_precision", _selectedGpsPrecision.value)
        }
    }

    fun updateCustomMessage(msg: String) {
        _customMessage.value = msg
        viewModelScope.launch {
            repository.saveSetting("custom_message", msg)
        }
    }

    fun setE2eeEnabled(enabled: Boolean) {
        _isE2eeEnabled.value = enabled
        viewModelScope.launch {
            repository.saveSetting("is_e2ee", enabled.toString())
        }
    }

    fun setGpsPrecision(precision: String) {
        _selectedGpsPrecision.value = precision
        viewModelScope.launch {
            repository.saveSetting("gps_precision", precision)
        }
    }

    // Trigger Emergency SOS Panic!
    fun triggerPanicSOS() {
        if (_sosState.value !is SosState.Idle) return

        viewModelScope.launch {
            val freshKey = "GUARDIAN-GCM-" + UUID.randomUUID().toString().take(12).uppercase()
            val initialLogs = mutableListOf<SosLog>()

            initialLogs.add(SosLog(message = "SOS Triggered: Initializing ultra-reliable safe pipeline...", level = LogLevel.CRITICAL))
            if (_isE2eeEnabled.value) {
                initialLogs.add(SosLog(message = "E2EE Handshake Complete. Session key established: $freshKey", level = LogLevel.SUCCESS))
            }

            // Create AlertLog entry in SQLite
            val logId = repository.insertAlertLog(
                AlertLog(
                    message = _customMessage.value,
                    latitude = baseLat,
                    longitude = baseLng,
                    status = "ACTIVE"
                )
            ).toInt()

            // Fetch primary contacts to target
            val targetContacts = contacts.value.filter { it.isPrimary }
            initialLogs.add(SosLog(message = "Targeting ${targetContacts.size} priority emergency contacts instantly...", level = LogLevel.INFO))

            // Simulate ultra low latency cellular messaging ping
            viewModelScope.launch {
                for (contact in targetContacts) {
                    delay(Random.nextLong(150, 400)) // Low-latency broadcast simulation
                    val sendLog = SosLog(
                        message = "SOS broadcast successfully pushed via encrypted gateway to ${contact.name} (${contact.relationship}) at ${contact.phone} [Rtt: ${Random.nextInt(8, 20)}ms]",
                        level = LogLevel.SUCCESS
                    )
                    addLogToState(sendLog)
                }
                addLogToState(SosLog(message = "Immediate broadcasts confirmed! Commencing 2-Minute Parent Safe-Acknowledge Countdown...", level = LogLevel.WARNING))
            }

            _sosState.value = SosState.Active(
                alertId = logId,
                secondsRemaining = 120,
                e2eeSessionKey = freshKey,
                logs = initialLogs,
                currentLatitude = baseLat,
                currentLongitude = baseLng,
                trackingLatency = Random.nextInt(9, 15)
            )

            startAlertMonitoringCycle(logId)
        }
    }

    // Cancel SOS Alert / Resolving the crisis
    fun resolveCrisis() {
        alertJob?.cancel()
        gpsUpdateJob?.cancel()

        val currentState = _sosState.value
        if (currentState is SosState.Active) {
            viewModelScope.launch {
                updateAlertInDbAndResolve(currentState.alertId)
            }
        } else if (currentState is SosState.PoliceEscalated) {
            viewModelScope.launch {
                updateAlertInDbAndResolve(currentState.alertId)
            }
        }
        _sosState.value = SosState.Idle
        // Reset base coordinate slightly
        baseLat = 37.7895 + (Random.nextDouble(-0.002, 0.002))
        baseLng = -122.4014 + (Random.nextDouble(-0.002, 0.002))
    }

    private suspend fun updateAlertInDbAndResolve(id: Int) {
        val alert = AlertLog(
            id = id,
            message = _customMessage.value,
            latitude = baseLat,
            longitude = baseLng,
            status = "RESOLVED",
            escalatedToPolice = _sosState.value is SosState.PoliceEscalated
        )
        repository.updateAlertLog(alert)
    }

    // Fast-Forward to Police Escalation Instantly (Manual or Debug)
    fun escalateToPoliceInstantly() {
        val current = _sosState.value
        if (current is SosState.Active) {
            escalateToPolice(current.alertId, current.e2eeSessionKey, current.logs)
        }
    }

    // Fast Forward the timer by say, 15 seconds to let the user play with it easily
    fun fastForwardTimer(seconds: Int = 15) {
        val current = _sosState.value
        if (current is SosState.Active) {
            val nextSeconds = (current.secondsRemaining - seconds).coerceAtLeast(0)
            _sosState.value = current.copy(secondsRemaining = nextSeconds)
            addLogToState(SosLog(message = "Simulated manual time skip: Forwarded countdown by $seconds seconds.", level = LogLevel.WARNING))
            if (nextSeconds == 0) {
                escalateToPolice(current.alertId, current.e2eeSessionKey, current.logs)
            }
        }
    }

    // Add a contact manually
    fun addContact(name: String, phone: String, relationship: String, isPrimary: Boolean) {
        viewModelScope.launch {
            repository.insertContact(
                Contact(
                    name = name,
                    phone = phone,
                    relationship = relationship,
                    isPrimary = isPrimary
                )
            )
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAlertLogs()
        }
    }

    // Alert escalation management inside coordinate tracking loops
    private fun startAlertMonitoringCycle(alertId: Int) {
        alertJob?.cancel()
        alertJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _sosState.value
                if (current is SosState.Active) {
                    val nextSeconds = current.secondsRemaining - 1
                    if (nextSeconds <= 0) {
                        escalateToPolice(alertId, current.e2eeSessionKey, current.logs)
                        break
                    } else {
                        // Slowly drift location to simulate active commuting/walking in real-time
                        baseLat += Random.nextDouble(-0.00012, 0.00012)
                        baseLng += Random.nextDouble(-0.00012, 0.00012)

                        // Update list of logs with some mock tracking statuses or warning flags
                        val updatedLogs = current.logs.toMutableList()
                        if (nextSeconds == 90) {
                            updatedLogs.add(SosLog(message = "Crisis escalation warning: 90 seconds remaining until Police Dispatch uplink is established.", level = LogLevel.WARNING))
                        } else if (nextSeconds == 45) {
                            updatedLogs.add(SosLog(message = "CRITICAL: No Parent check-in. Pre-staging Police dispatcher uplink route key hashes...", level = LogLevel.CRITICAL))
                        } else if (nextSeconds % 12 == 0) {
                            updatedLogs.add(SosLog(message = "Encrypted telemetry ping verified. Lat: ${String.format(Locale.US, "%.5f", baseLat)}, Lng: ${String.format(Locale.US, "%.5f", baseLng)} [Pkt Loss: 0.0%]", level = LogLevel.INFO))
                        }

                        _sosState.value = current.copy(
                            secondsRemaining = nextSeconds,
                            currentLatitude = baseLat,
                            currentLongitude = baseLng,
                            trackingLatency = Random.nextInt(7, 14),
                            logs = updatedLogs
                        )
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun escalateToPolice(alertId: Int, e2eeKey: String, existingLogs: List<SosLog>) {
        alertJob?.cancel()

        val escalatedLogs = existingLogs.toMutableList()
        escalatedLogs.add(SosLog(message = "PARENT RESPONSET_TIMEOUT LIMIT EXCEEDED (2:00 mins). Auto-escalating crisis state!", level = LogLevel.CRITICAL))
        escalatedLogs.add(SosLog(message = "Establishing point-to-point encrypted channel to Law Enforcement Command.", level = LogLevel.CRITICAL))
        escalatedLogs.add(SosLog(message = "Police gateway handshaked successfully. Encrypted location telemetry streaming active.", level = LogLevel.SUCCESS))

        val unitNames = listOf("Interceptor Patrol Unit 104", "Sentinel Unit 309", "Metro Rescue Unit 22")
        val officers = listOf("Officer Henderson", "Officer Ramirez", "Officer Gallagher")

        _sosState.value = SosState.PoliceEscalated(
            alertId = alertId,
            e2eeSessionKey = e2eeKey,
            logs = escalatedLogs,
            currentLatitude = baseLat,
            currentLongitude = baseLng,
            dispatchUnit = unitNames.random(),
            officerName = officers.random(),
            dispatchStatus = "ASSIGNED",
            dispatchEtaSeconds = 180, // 3:00 mins ETA
            trackingLatency = Random.nextInt(10, 18)
        )

        // Update local database status
        viewModelScope.launch {
            val alert = AlertLog(
                id = alertId,
                message = _customMessage.value,
                latitude = baseLat,
                longitude = baseLng,
                status = "ESCALATED",
                escalatedToPolice = true
            )
            repository.updateAlertLog(alert)
        }

        // Run police tracking dispatch movements
        startPoliceDispatchSimulation()
    }

    private fun startPoliceDispatchSimulation() {
        alertJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _sosState.value
                if (current is SosState.PoliceEscalated) {
                    val nextEta = (current.dispatchEtaSeconds - 4).coerceAtLeast(0)
                    val nextStatus = when {
                        nextEta == 0 -> "SECURED"
                        nextEta < 30 -> "ARRIVING"
                        nextEta < 120 -> "EN_ROUTE"
                        else -> "ASSIGNED"
                    }

                    // Drift dispatcher/victim coordinates closer
                    baseLat += Random.nextDouble(-0.00008, 0.00008)
                    baseLng += Random.nextDouble(-0.00008, 0.00008)

                    val updatedLogs = current.logs.toMutableList()
                    if (current.dispatchStatus != nextStatus) {
                        val statusMsg = when (nextStatus) {
                            "EN_ROUTE" -> "Police Dispatch: ${current.dispatchUnit} (${current.officerName}) has departed regional base. Sirens active, heading to GPS target."
                            "ARRIVNG" -> "Police Dispatch: Visual contact imminent. Unit is less than 300 meters away."
                            "SECURED" -> "Police Safety Check: ${current.dispatchUnit} arrived on site and verified safety."
                            else -> "Dispatch status updated: $nextStatus"
                        }
                        updatedLogs.add(SosLog(message = statusMsg, level = if (nextStatus == "SECURED") LogLevel.SUCCESS else LogLevel.INFO))
                    }

                    if (nextEta % 20 == 0 && nextEta > 0) {
                        updatedLogs.add(SosLog(message = "Secure Uplink PING: Telemetry coordinates successfully received by dispatcher's vehicle hardware console.", level = LogLevel.INFO))
                    }

                    _sosState.value = current.copy(
                        dispatchEtaSeconds = nextEta,
                        dispatchStatus = nextStatus,
                        currentLatitude = baseLat,
                        currentLongitude = baseLng,
                        trackingLatency = Random.nextInt(8, 15),
                        logs = updatedLogs
                    )

                    if (nextEta == 0) {
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun addLogToState(log: SosLog) {
        val current = _sosState.value
        if (current is SosState.Active) {
            _sosState.value = current.copy(logs = current.logs + log)
        } else if (current is SosState.PoliceEscalated) {
            _sosState.value = current.copy(logs = current.logs + log)
        }
    }
}
