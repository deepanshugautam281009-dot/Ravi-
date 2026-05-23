package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import kotlin.random.Random
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSafetyScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val alertHistory by viewModel.alertHistory.collectAsState()
    val sosState by viewModel.sosState.collectAsState()
    val customMessage by viewModel.customMessage.collectAsState()
    val isE2eeEnabled by viewModel.isE2eeEnabled.collectAsState()
    val gpsPrecision by viewModel.selectedGpsPrecision.collectAsState()

    var activeTab by rememberSaveable { mutableStateOf("SOS") }
    var showAddContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Guardian Shield",
                            tint = when (sosState) {
                                is SosState.Idle -> SafeGreen
                                is SosState.Active -> EmergencyOrange
                                is SosState.PoliceEscalated -> EmergencyRed
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "GUARDIAN SOS",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralWhite,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = when (sosState) {
                                    is SosState.Idle -> "SYSTEM: ACTIVE & ENCRYPTED"
                                    is SosState.Active -> "STATUS: EMERGENCY BROADCAST"
                                    is SosState.PoliceEscalated -> "DISPATCH: POLICE INTERCEPT ACTIVE"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (sosState) {
                                    is SosState.Idle -> SafeGreen
                                    is SosState.Active -> EmergencyOrange
                                    is SosState.PoliceEscalated -> EmergencyRed
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = NeutralWhite
                ),
                actions = {
                    // Quick network stats badge
                    Surface(
                        color = BorderColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (sosState is SosState.Idle) SafeGreen else EmergencyRed)
                            )
                            Text(
                                text = "UPLINK: 99.99%",
                                fontSize = 9.sp,
                                color = SecondaryMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag("primary_navigation")
            ) {
                listOf(
                    Triple("SOS", Icons.Filled.Emergency, Icons.Outlined.Emergency),
                    Triple("CONTACTS", Icons.Filled.People, Icons.Outlined.People),
                    Triple("DISPATCH", Icons.Filled.LocalPolice, Icons.Outlined.LocalPolice),
                    Triple("SETUP", Icons.Filled.Settings, Icons.Outlined.Settings)
                ).forEach { (tab, filledIcon, outlinedIcon) ->
                    val isSelected = activeTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = "$tab Tab",
                                tint = if (isSelected) {
                                    if (tab == "SOS") EmergencyRed else PrimaryTeal
                                } else SecondaryMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeutralWhite else SecondaryMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = BorderColor
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            // Screen switching
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenNavigator",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    "SOS" -> SosEngineTab(viewModel, onNavigateToContacts = { activeTab = "CONTACTS" })
                    "CONTACTS" -> ContactsTab(
                        contacts = contacts,
                        onAddClick = { showAddContactDialog = true },
                        onDeleteClick = { viewModel.deleteContact(it) }
                    )
                    "DISPATCH" -> DispatchTab(viewModel)
                    "SETUP" -> SettingsTab(
                        viewModel = viewModel,
                        alertHistory = alertHistory
                    )
                }
            }
        }

        if (showAddContactDialog) {
            AddContactSheet(
                onDismiss = { showAddContactDialog = false },
                onSave = { name, phone, relationship, isPrimary ->
                    viewModel.addContact(name, phone, relationship, isPrimary)
                    showAddContactDialog = false
                }
            )
        }
    }
}

@Composable
fun SosEngineTab(
    viewModel: SafetyViewModel,
    onNavigateToContacts: () -> Unit
) {
    val sosState by viewModel.sosState.collectAsState()
    val isE2eeEnabled by viewModel.isE2eeEnabled.collectAsState()
    val gpsPrecision by viewModel.selectedGpsPrecision.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Safe Shield Badge / Incident State
        item {
            ShieldStatusWidget(sosState = sosState)
        }

        // Active Countdown UI or Large SOS Button
        item {
            when (val state = sosState) {
                is SosState.Idle -> {
                    SosTriggerButton(onTrigger = { viewModel.triggerPanicSOS() })
                }
                is SosState.Active -> {
                    ActiveCrisisHud(
                        state = state,
                        isE2ee = isE2eeEnabled,
                        onCancel = { viewModel.resolveCrisis() },
                        onEscalate = { viewModel.escalateToPoliceInstantly() },
                        onForward = { viewModel.fastForwardTimer() }
                    )
                }
                is SosState.PoliceEscalated -> {
                    PoliceEscalatedHud(
                        state = state,
                        isE2ee = isE2eeEnabled,
                        onCancel = { viewModel.resolveCrisis() }
                    )
                }
            }
        }

        // Target broadcast status
        if (sosState is SosState.Active) {
            item {
                BroadcastStatusGrid(contacts = viewModel.contacts.collectAsState().value)
            }
        }

        // Running Logs or Secure Handshake Terminal
        item {
            TerminalLogsPanel(sosState = sosState)
        }
    }
}

@Composable
fun ShieldStatusWidget(sosState: SosState) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        when (sosState) {
                            is SosState.Idle -> SafeGreen.copy(alpha = 0.15f)
                            is SosState.Active -> EmergencyOrange.copy(alpha = 0.15f)
                            is SosState.PoliceEscalated -> EmergencyRed.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (sosState) {
                        is SosState.Idle -> Icons.Filled.GppGood
                        is SosState.Active -> Icons.Filled.WifiTethering
                        is SosState.PoliceEscalated -> Icons.Filled.Radar
                    },
                    contentDescription = "Shield Status",
                    tint = when (sosState) {
                        is SosState.Idle -> SafeGreen
                        is SosState.Active -> EmergencyOrange
                        is SosState.PoliceEscalated -> EmergencyRed
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (sosState) {
                        is SosState.Idle -> "Guardian Vault Secured"
                        is SosState.Active -> "Emergency SOS Active"
                        is SosState.PoliceEscalated -> "Emergency Escalated to Police"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralWhite
                )
                Text(
                    text = when (sosState) {
                        is SosState.Idle -> "Standing by to safeguard your transit. In emergency, tap or hold the SOS panic shield below."
                        is SosState.Active -> "Sending low-latency distress signals to designated trusted parents/relatives with live tracking."
                        is SosState.PoliceEscalated -> "Automated parent timeout reached. Active police tactical unit en route to your live coordinates."
                    },
                    fontSize = 11.sp,
                    color = SecondaryMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun SosTriggerButton(onTrigger: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(220.dp)
                .testTag("sos_trigger_box")
        ) {
            // Ripple Background representation
            Box(
                modifier = Modifier
                    .size((180 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(EmergencyRed.copy(alpha = pulseAlpha))
            )

            // Outer ring
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .border(BorderStroke(4.dp, EmergencyRed.copy(alpha = 0.4f)), CircleShape)
            )

            // Inner button
            Card(
                onClick = onTrigger,
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = EmergencyRed
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .size(145.dp)
                    .testTag("sos_trigger_btn")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmergencyShare,
                            contentDescription = "Panic Trigger",
                            tint = NeutralWhite,
                            modifier = Modifier.size(46.dp)
                        )
                        Text(
                            text = "TRIGGER\nSOS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeutralWhite,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "TAP TO INSTANTLY BROADCAST TO FAMILY & POLICE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = SecondaryMuted,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ActiveCrisisHud(
    state: SosState.Active,
    isE2ee: Boolean,
    onCancel: () -> Unit,
    onEscalate: () -> Unit,
    onForward: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, EmergencyOrange.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.HourglassBottom,
                        contentDescription = "Countdown Indicator",
                        tint = EmergencyOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "PARENT RESPONSE WINDOW",
                        fontSize = 11.sp,
                        color = SecondaryMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = EmergencyOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "ACTIVE ALERT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmergencyOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Big Timer Circular Indicator representation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                val progress = state.secondsRemaining / 120f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = BorderColor,
                        style = Stroke(width = 8.dp.toPx())
                    )
                    drawArc(
                        color = EmergencyOrange,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val mm = state.secondsRemaining / 60
                    val ss = state.secondsRemaining % 60
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mm, ss),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NeutralWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SEC REMAINING",
                        fontSize = 8.sp,
                        color = SecondaryMuted,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "If parents/friends do not respond within this window, the incident automatically escalates to immediate Police dispatch.",
                fontSize = 11.sp,
                color = SecondaryMuted,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            // Quick Actions Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onForward,
                    colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Skip Indicator", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip 15s", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEscalate,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.3f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Filled.LocalPolice, contentDescription = "Urgent Police Action", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Escalate Police", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = "Safety Confirmed")
                Spacer(modifier = Modifier.width(6.dp))
                Text("I am Safe, Dismiss SOS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = DarkBackground)
            }
        }
    }
}

@Composable
fun PoliceEscalatedHud(
    state: SosState.PoliceEscalated,
    isE2ee: Boolean,
    onCancel: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, EmergencyRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Crisis level badge",
                        tint = EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "POLICE INTERCEPT ACTIVE",
                        fontSize = 11.sp,
                        color = EmergencyRed,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = EmergencyRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "PRIORITY 1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmergencyRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Real-time dispatch telemetry details Card
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RESPONDING UNIT",
                                fontSize = 9.sp,
                                color = SecondaryMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.dispatchUnit,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeutralWhite
                            )
                        }

                        Surface(
                            color = InfoBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = state.dispatchStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = InfoBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "OFFICER IN CHARGE",
                                fontSize = 9.sp,
                                color = SecondaryMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = state.officerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralWhite
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "POLICE ARRIVAL ETA",
                                fontSize = 9.sp,
                                color = SecondaryMuted,
                                fontWeight = FontWeight.Bold
                            )
                            val mm = state.dispatchEtaSeconds / 60
                            val ss = state.dispatchEtaSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", mm, ss),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = EmergencyRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = "Safety Verification Icon")
                Spacer(modifier = Modifier.width(6.dp))
                Text("De-escalate & Resolve Crisis", fontSize = 13.sp, fontWeight = FontWeight.Black, color = DarkBackground)
            }
        }
    }
}

@Composable
fun BroadcastStatusGrid(contacts: List<Contact>) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LOW-LATENCY BROADCAST STATUS CODES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryMuted,
                letterSpacing = 0.5.sp
            )

            contacts.forEach { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (contact.relationship) {
                                "Parent" -> Icons.Filled.FamilyRestroom
                                "Relative" -> Icons.Filled.Diversity1
                                else -> Icons.Filled.Person
                            },
                            contentDescription = "Contact Relationship Type Icon",
                            tint = when (contact.relationship) {
                                "Parent" -> EmergencyRed
                                "Relative" -> EmergencyOrange
                                else -> InfoBlue
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = contact.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralWhite
                            )
                            Text(
                                text = "${contact.relationship} • ${contact.phone}",
                                fontSize = 9.sp,
                                color = SecondaryMuted
                            )
                        }
                    }

                    // Simulated rapid transmission telemetry
                    Surface(
                        color = SafeGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "UDP PUSH DELIVERED [12ms]",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SafeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TerminalLogsPanel(sosState: SosState) {
    val logs = when (sosState) {
        is SosState.Idle -> listOf(
            SosLog(message = "Ready: Encrypted transport layer waiting for security token.", level = LogLevel.SUCCESS),
            SosLog(message = "GPS Precision Core calibrating... [OK]", level = LogLevel.INFO)
        )
        is SosState.Active -> statesListToLog(sosState.logs)
        is SosState.PoliceEscalated -> statesListToLog(sosState.logs)
    }

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "Terminal details",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SECURE PROTOCOL TRANSMISSION FEED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                if (sosState is SosState.Active || sosState is SosState.PoliceEscalated) {
                    val key = if (sosState is SosState.Active) sosState.e2eeSessionKey else (sosState as SosState.PoliceEscalated).e2eeSessionKey
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock Icon", tint = SafeGreen, modifier = Modifier.size(10.dp))
                        Text(
                            text = "AES-E2E: ${key.take(9)}...",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.8f))
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                reverseLayout = true
            ) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val sdf = SimpleDateFormat("HH:mm:ss.SS", Locale.US)
                        Text(
                            text = sdf.format(Date(log.timestamp)),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SecondaryMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (log.level) {
                                LogLevel.INFO -> " [INFO] "
                                LogLevel.SUCCESS -> " [OK  ] "
                                LogLevel.WARNING -> " [WARN] "
                                LogLevel.CRITICAL -> " [FAIL] "
                            },
                            color = when (log.level) {
                                LogLevel.INFO -> InfoBlue
                                LogLevel.SUCCESS -> SafeGreen
                                LogLevel.WARNING -> EmergencyOrange
                                LogLevel.CRITICAL -> EmergencyRed
                            },
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.message,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when (log.level) {
                                LogLevel.CRITICAL -> EmergencyRed
                                LogLevel.WARNING -> EmergencyOrange
                                LogLevel.SUCCESS -> SafeGreen
                                else -> NeutralWhite
                            },
                            modifier = Modifier.weight(1f),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

fun statesListToLog(logs: List<SosLog>): List<SosLog> {
    return logs.sortedBy { it.timestamp }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    contacts: List<Contact>,
    onAddClick: () -> Unit,
    onDeleteClick: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Trusted Safe Contacts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralWhite
                )
                Text(
                    text = "Manage designated parents & friends to get priority SOS alerts.",
                    fontSize = 11.sp,
                    color = SecondaryMuted
                )
            }

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Contact")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
            }
        }

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Diversity3,
                        contentDescription = "No contacts icon",
                        tint = SecondaryMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Trusted Contacts Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralWhite
                    )
                    Text(
                        text = "Click 'Add' above to pre-insert emergency response handlers.",
                        fontSize = 12.sp,
                        color = SecondaryMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts) { contact ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (contact.relationship) {
                                                "Parent" -> EmergencyRed.copy(alpha = 0.12f)
                                                "Relative" -> EmergencyOrange.copy(alpha = 0.12f)
                                                else -> InfoBlue.copy(alpha = 0.12f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (contact.relationship) {
                                            "Parent" -> Icons.Filled.FamilyRestroom
                                            "Relative" -> Icons.Filled.Diversity1
                                            else -> Icons.Filled.Person
                                        },
                                        contentDescription = "Contact Category",
                                        tint = when (contact.relationship) {
                                            "Parent" -> EmergencyRed
                                            "Relative" -> EmergencyOrange
                                            else -> InfoBlue
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = contact.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeutralWhite
                                        )
                                        Surface(
                                            color = when (contact.relationship) {
                                                "Parent" -> EmergencyRed.copy(alpha = 0.15f)
                                                "Relative" -> EmergencyOrange.copy(alpha = 0.15f)
                                                else -> InfoBlue.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = contact.relationship.uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when (contact.relationship) {
                                                    "Parent" -> EmergencyRed
                                                    "Relative" -> EmergencyOrange
                                                    else -> InfoBlue
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Phone,
                                            contentDescription = "Phone icon",
                                            tint = SecondaryMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = contact.phone,
                                            fontSize = 12.sp,
                                            color = SecondaryMuted
                                        )
                                    }
                                }
                            }

                            // Delete button ensuring 48dp target
                            IconButton(
                                onClick = { onDeleteClick(contact) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_${contact.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete Contact Button",
                                    tint = EmergencyRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispatchTab(viewModel: SafetyViewModel) {
    val sosState by viewModel.sosState.collectAsState()
    val isE2eeEnabled by viewModel.isE2eeEnabled.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Law Enforcement Dispatch",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralWhite
                )
                Text(
                    text = "Encrypted endpoint that streams active distress events directly to emergency response coordinators.",
                    fontSize = 11.sp,
                    color = SecondaryMuted
                )
            }
        }

        // Tactical Status panel / Dispatch monitor
        item {
            TacticalRadarWidget(sosState = sosState)
        }

        item {
            TelemetryDashboardWidget(sosState = sosState, isE2ee = isE2eeEnabled)
        }

        if (sosState is SosState.Idle) {
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Shield Guard Icon",
                            tint = SecondaryMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No Live Crisis Registered",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite
                        )
                        Text(
                            text = "When an SOS alert is active and the parent response window times out (or if escalated manually), police radar visualizer tracks physical coordinates and dispatches local security units instantly.",
                            fontSize = 11.sp,
                            color = SecondaryMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalRadarWidget(sosState: SosState) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                // Circles rings
                drawCircle(color = BorderColor, radius = radius, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = BorderColor, radius = radius * 0.66f, style = Stroke(width = 1.dp.toPx()))
                drawCircle(color = BorderColor, radius = radius * 0.33f, style = Stroke(width = 1.dp.toPx()))

                // Grid lines
                drawLine(color = BorderColor, start = Offset(0f, center.y), end = Offset(size.width, center.y))
                drawLine(color = BorderColor, start = Offset(center.x, 0f), end = Offset(center.x, size.height))

                // Radar Sweeper line
                val angleRad = Math.toRadians(sweepRotation.toDouble())
                val endX = center.x + radius * cos(angleRad).toFloat()
                val endY = center.y + radius * sin(angleRad).toFloat()
                drawLine(
                    color = if (sosState is SosState.PoliceEscalated) EmergencyRed else PrimaryTeal.copy(alpha = 0.6f),
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )

                // Dispatch points mapping representation
                if (sosState is SosState.PoliceEscalated) {
                    val progress = (180 - sosState.dispatchEtaSeconds) / 180f
                    // Victim Dot
                    drawCircle(color = EmergencyRed, radius = 6.dp.toPx(), center = center)

                    // Cop unit intercepting dot
                    val copX = center.x + radius * (1f - progress) * 0.7f
                    val copY = center.y + radius * (1f - progress) * 0.3f
                    drawCircle(color = InfoBlue, radius = 5.dp.toPx(), center = Offset(copX, copY))
                }
            }

            // Radar Status Badge Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Surface(
                    color = DarkBackground.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "RADAR: ACTIVE STREAM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sosState is SosState.PoliceEscalated) EmergencyRed else PrimaryTeal,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                if (sosState is SosState.PoliceEscalated) {
                    Surface(
                        color = InfoBlue.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "COP UNIT APPROACHING",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryDashboardWidget(sosState: SosState, isE2ee: Boolean) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "ENCRYPTED GEOLOCATION TELEMETRY",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SecondaryMuted,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Latitude Column
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("LATITUDE", fontSize = 8.sp, color = SecondaryMuted, fontWeight = FontWeight.Bold)
                        val latStr = when (sosState) {
                            is SosState.Idle -> "37.78950"
                            is SosState.Active -> String.format(Locale.US, "%.5f", sosState.currentLatitude)
                            is SosState.PoliceEscalated -> String.format(Locale.US, "%.5f", sosState.currentLatitude)
                        }
                        Text(
                            text = latStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (sosState is SosState.Idle) NeutralWhite else EmergencyRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Longitude Column
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("LONGITUDE", fontSize = 8.sp, color = SecondaryMuted, fontWeight = FontWeight.Bold)
                        val lngStr = when (sosState) {
                            is SosState.Idle -> "-122.40140"
                            is SosState.Active -> String.format(Locale.US, "%.5f", sosState.currentLongitude)
                            is SosState.PoliceEscalated -> String.format(Locale.US, "%.5f", sosState.currentLongitude)
                        }
                        Text(
                            text = lngStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (sosState is SosState.Idle) NeutralWhite else EmergencyRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Connection health and latency status grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PACKET LATENCY", fontSize = 8.sp, color = SecondaryMuted)
                    Text(
                        text = if (sosState is SosState.Idle) "0ms (STANDBY)" else "${Random.nextInt(9, 15)}ms (LOW-LATENCY)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sosState is SosState.Idle) SecondaryMuted else SafeGreen
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("TRANSPORT CRYPTO", fontSize = 8.sp, color = SecondaryMuted)
                    Text(
                        text = if (isE2ee) "AES-GCM-256 (ACTIVE)" else "PLAINTEXT UNSECURED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isE2ee) PrimaryTeal else EmergencyOrange
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: SafetyViewModel,
    alertHistory: List<com.example.data.AlertLog>
) {
    val customMessage by viewModel.customMessage.collectAsState()
    val isE2eeEnabled by viewModel.isE2eeEnabled.collectAsState()
    val gpsPrecision by viewModel.selectedGpsPrecision.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Guardian Setup Console",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralWhite
                )
                Text(
                    text = "Configure critical safety behaviors, E2E options, and inspect audit logs.",
                    fontSize = 11.sp,
                    color = SecondaryMuted
                )
            }
        }

        // Custom Message Formulation
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CUSTOM SOS EMERGENCY PHRASE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SecondaryMuted,
                        letterSpacing = 0.5.sp
                    )

                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { viewModel.updateCustomMessage(it) },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_message_input"),
                        textStyle = TextStyle(fontSize = 12.sp, color = NeutralWhite),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderColor,
                            focusedBorderColor = PrimaryTeal,
                            unfocusedContainerColor = DarkBackground,
                            focusedContainerColor = DarkBackground
                        )
                    )

                    Text(
                        text = "This phrase is broadcast instantly to parents, relatives, and friends during a panic event.",
                        fontSize = 9.sp,
                        color = SecondaryMuted
                    )
                }
            }
        }

        // Encryption Options Toggle
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "END-TO-END ENCRYPTION (E2EE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeutralWhite
                        )
                        Text(
                            text = "Secures physical coordinate streams via zero-knowledge AES handshakes so interceptors cannot view your route.",
                            fontSize = 10.sp,
                            color = SecondaryMuted,
                            lineHeight = 14.sp
                        )
                    }

                    Switch(
                        checked = isE2eeEnabled,
                        onCheckedChange = { viewModel.setE2eeEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryTeal,
                            checkedTrackColor = PrimaryTeal.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("e2ee_switch")
                    )
                }
            }
        }

        // GPS Accuracy Level Selectors
        item {
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SATELLITE POSITIONING PRECISION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralWhite
                    )

                    listOf(
                        "HIGH (Multi-GNSS Dual Band)",
                        "STANDARD (GPS + GLONASS)",
                        "CELLULAR HYBRID (Power-Saving)"
                    ).forEach { option ->
                        val isSelected = gpsPrecision == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BorderColor else Color.Transparent)
                                .clickable { viewModel.setGpsPrecision(option) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.setGpsPrecision(option) }
                            )
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                color = if (isSelected) NeutralWhite else SecondaryMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Historic Incident logs from Room DB
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audit Safety Logs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeutralWhite
                )

                if (alertHistory.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear History", color = EmergencyRed, fontSize = 12.sp)
                    }
                }
            }
        }

        if (alertHistory.isEmpty()) {
            item {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No previous incidents recorded.",
                            fontSize = 12.sp,
                            color = SecondaryMuted
                        )
                    }
                }
            }
        } else {
            items(alertHistory) { log ->
                val date = Date(log.timestamp)
                val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatter.format(date),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryMuted
                            )

                            Surface(
                                color = when (log.status) {
                                    "RESOLVED" -> SafeGreen.copy(alpha = 0.15f)
                                    else -> EmergencyRed.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = log.status,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (log.status) {
                                        "RESOLVED" -> SafeGreen
                                        else -> EmergencyRed
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = log.message,
                            fontSize = 12.sp,
                            color = NeutralWhite,
                            lineHeight = 16.sp
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Locations Pin icon", tint = EmergencyRed, modifier = Modifier.size(10.dp))
                            Text(
                                text = "Incident Coordinates: ${String.format(Locale.US, "%.4f", log.latitude)}, ${String.format(Locale.US, "%.4f", log.longitude)}",
                                fontSize = 9.sp,
                                color = SecondaryMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relationship: String, isPrimary: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Parent") }
    var isPrimary by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp), // Safe keyb space
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Trusted Contact",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeutralWhite
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = SecondaryMuted,
                    unfocusedContainerColor = DarkBackground,
                    focusedContainerColor = DarkBackground
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contact_phone_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = BorderColor,
                    focusedLabelColor = PrimaryTeal,
                    unfocusedLabelColor = SecondaryMuted,
                    unfocusedContainerColor = DarkBackground,
                    focusedContainerColor = DarkBackground
                ),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RELATIONSHIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Parent", "Relative", "Friend").forEach { rel ->
                        val isSel = relationship == rel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) BorderColor else DarkBackground)
                                .border(BorderStroke(1.dp, if (isSel) PrimaryTeal else BorderColor), RoundedCornerShape(12.dp))
                                .clickable { relationship = rel }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rel,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) NeutralWhite else SecondaryMuted
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Primary SOS Broadcast",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeutralWhite
                    )
                    Text(
                        text = "Instantly sends distress packets to this target the moment any safety trigger event starts.",
                        fontSize = 10.sp,
                        color = SecondaryMuted
                    )
                }
                Switch(
                    checked = isPrimary,
                    onCheckedChange = { isPrimary = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryTeal,
                        checkedTrackColor = PrimaryTeal.copy(alpha = 0.4f)
                    )
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(name, phone, relationship, isPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_contact_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Trusted Member", fontWeight = FontWeight.Black, fontSize = 14.sp, color = DarkBackground)
            }
        }
    }
}
