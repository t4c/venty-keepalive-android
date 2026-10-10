package de.t4c.ventykeepalive

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var watchdogService: VentyWatchdogService? = null
    private var isBound = false

    private val serviceState = mutableStateOf(VentyState())
    private val thresholdState = mutableFloatStateOf(30f)
    private val vibrationState = mutableStateOf(true)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as VentyWatchdogService.LocalBinder
            val s = binder.getService()
            watchdogService = s
            isBound = true

            // Sync initial state
            thresholdState.floatValue = s.thresholdSeconds.toFloat()
            vibrationState.value = s.autoDisableVibration

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    s.state.collect { newState ->
                        serviceState.value = newState
                    }
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            watchdogService = null
            isBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startWatchdogService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Bind if service is already running
        val intent = Intent(this, VentyWatchdogService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    primary = Color(0xFFFF9800), // Venty Orange
                    secondary = Color(0xFF03DAC6),
                    onBackground = Color(0xFFE0E0E0),
                    onSurface = Color(0xFFEEEEEE)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                    ) {
                        VentyDashboard(
                            state = serviceState.value,
                            threshold = thresholdState.floatValue,
                            autoVibrate = vibrationState.value,
                            onThresholdChange = { newVal ->
                                thresholdState.floatValue = newVal
                                watchdogService?.thresholdSeconds = newVal.toInt()
                            },
                            onVibrateToggle = { newVal ->
                                vibrationState.value = newVal
                                watchdogService?.autoDisableVibration = newVal
                            },
                            onStartClick = { checkAndStart() },
                            onStopClick = { stopWatchdogService() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        super.onDestroy()
    }

    private fun checkAndStart() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        } else {
            startWatchdogService()
        }
    }

    private fun startWatchdogService() {
        val intent = Intent(this, VentyWatchdogService::class.java).apply {
            action = VentyWatchdogService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun stopWatchdogService() {
        val intent = Intent(this, VentyWatchdogService::class.java).apply {
            action = VentyWatchdogService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
fun VentyDashboard(
    state: VentyState,
    threshold: Float,
    autoVibrate: Boolean,
    onThresholdChange: (Float) -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "About Venty Keep Alive",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "written by Milan 't4c' Berger",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Venty auto-shutoff timer bypass daemon via BLE state bounce.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )

                    HorizontalDivider(color = Color(0xFF333333))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Web:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        TextButton(onClick = { uriHandler.openUri("https://www.ghcif.de") }) {
                            Text("https://www.ghcif.de", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GitHub:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        TextButton(onClick = { uriHandler.openUri("https://github.com/t4c/venty-keepalive-android") }) {
                            Text("github.com/t4c/venty-keepalive-android", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Email:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        TextButton(onClick = { uriHandler.openUri("mailto:t4c@ghcif.de") }) {
                            Text("t4c@ghcif.de", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VENTY KEEP ALIVE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 2.sp
        )

        // Device Connection & Status Badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (state.connected) Color(0xFF1B3820) else Color(0xFF262626)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (state.connected) Color(0xFF4CAF50) else Color(0xFF888888),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                Text(
                    text = if (state.connected && state.deviceName.isNotBlank()) {
                        "${state.deviceName} • ${state.statusMessage}"
                    } else if (state.deviceName.isNotBlank()) {
                        "${state.deviceName} • ${state.statusMessage}"
                    } else {
                        state.statusMessage
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.connected) Color(0xFF81C784) else Color(0xFFCCCCCC),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Telemetry Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TARGET TEMPERATURE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${state.targetTemp} °C", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("BATTERY", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${state.battery}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C2C))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SHUTOFF TIMER", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            "${state.timerSeconds}s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (state.timerSeconds <= threshold.toInt()) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("HEATER MODE", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(state.heaterMode.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF2C2C2C))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Executed Resets: ${state.resetCount}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    val devLabel = if (state.deviceName.isNotBlank()) state.deviceName else if (state.deviceAddress.isNotBlank()) state.deviceAddress else "Not Connected"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        Text(
                            text = devLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("CONFIGURATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reset Threshold: ${threshold.toInt()} seconds", style = MaterialTheme.typography.bodyMedium)
                }

                Slider(
                    value = threshold,
                    onValueChange = onThresholdChange,
                    valueRange = 10f..60f,
                    steps = 9
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mute vibration on connect", style = MaterialTheme.typography.bodyMedium)
                        Text("Prevents haptic buzz during keep-alive bounce", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(
                        checked = autoVibrate,
                        onCheckedChange = onVibrateToggle
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStartClick,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("START KEEP ALIVE", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onStopClick,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("STOP", fontWeight = FontWeight.Bold)
            }
        }

        // About Button (unten rechts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showAboutDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("About", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
