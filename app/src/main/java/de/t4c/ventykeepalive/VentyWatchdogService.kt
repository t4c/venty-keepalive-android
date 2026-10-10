package de.t4c.ventykeepalive

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class VentyWatchdogService : Service() {

    companion object {
        private const val TAG = "VentyService"
        private const val NOTIFICATION_ID = 1337

        const val ACTION_START = "de.t4c.ventykeepalive.START"
        const val ACTION_STOP = "de.t4c.ventykeepalive.STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): VentyWatchdogService = this@VentyWatchdogService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var commCharacteristic: BluetoothGattCharacteristic? = null

    private var wakeLock: PowerManager.WakeLock? = null

    private val _state = MutableStateFlow(VentyState())
    val state: StateFlow<VentyState> = _state.asStateFlow()

    // Config settings
    var thresholdSeconds: Int = 30
    var autoDisableVibration: Boolean = true

    private var isScanning = false
    private var isSequenceInProgress = false
    private var lastKeepAliveTime = 0L

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VentyKeepAlive::ServiceLock")
        wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24h fallback
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopWatchdog()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, buildNotification("Keep Alive active, searching Venty..."))
                startWatchdog()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopWatchdog()
        serviceScope.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    fun startWatchdog() {
        if (_state.value.connected || isScanning) return
        startBleScan()
        startPollingJob()
    }

    fun stopWatchdog() {
        stopBleScan()
        disconnectGatt()
        _state.value = VentyState(statusMessage = "Watchdog stopped")
        updateNotification()
    }

    private fun startBleScan() {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _state.value = _state.value.copy(statusMessage = "Bluetooth disabled")
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.value = _state.value.copy(statusMessage = "BLE Scanner unavailable")
            return
        }

        isScanning = true
        _state.value = _state.value.copy(statusMessage = "Scanning for S&B VY...")
        updateNotification()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val filters = listOf(
            ScanFilter.Builder().build()
        )

        try {
            scanner.startScan(filters, settings, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}")
            isScanning = false
        }
    }

    private fun stopBleScan() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = device.name ?: result.scanRecord?.deviceName
                if (name != null && name.startsWith(VentyConstants.DEVICE_NAME_PREFIX)) {
                    Log.i(TAG, "Found target Venty device: $name (${device.address})")
                    stopBleScan()
                    connectToDevice(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with code $errorCode")
            isScanning = false
            _state.value = _state.value.copy(statusMessage = "Scan failed ($errorCode)")
            // Retry scan after delay
            serviceScope.launch {
                delay(4000)
                if (!_state.value.connected) startBleScan()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        _state.value = _state.value.copy(
            deviceName = device.name ?: "Venty",
            deviceAddress = device.address,
            statusMessage = "Connecting to ${device.name ?: device.address}..."
        )
        updateNotification()

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }
    }

    private fun disconnectGatt() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT: ${e.message}")
        }
        bluetoothGatt = null
        commCharacteristic = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "GATT connected, discovering services...")
                _state.value = _state.value.copy(
                    connected = true,
                    statusMessage = "Connected, discovering services..."
                )
                updateNotification()
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "GATT disconnected.")
                commCharacteristic = null
                _state.value = _state.value.copy(
                    connected = false,
                    statusMessage = "Disconnected. Rescanning..."
                )
                updateNotification()
                disconnectGatt()
                serviceScope.launch {
                    delay(3000)
                    startBleScan()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(VentyConstants.SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(VentyConstants.COMM_CHAR_UUID)
                    if (characteristic != null) {
                        commCharacteristic = characteristic
                        enableNotifications(gatt, characteristic)
                        _state.value = _state.value.copy(
                            connected = true,
                            deviceName = gatt.device?.name ?: _state.value.deviceName,
                            deviceAddress = gatt.device?.address ?: _state.value.deviceAddress,
                            statusMessage = "Connected & active"
                        )
                        updateNotification()

                        if (autoDisableVibration) {
                            serviceScope.launch {
                                delay(300)
                                writeCharacteristic(VentyConstants.createDisableVibrationFrame())
                            }
                        }
                        return
                    }
                }
                Log.e(TAG, "Venty service/characteristic not found")
                _state.value = _state.value.copy(statusMessage = "Incompatible BLE Services")
            }
        }

        @Deprecated("Deprecated for SDK >= 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { parsePayload(it) }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parsePayload(value)
        }
    }

    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(VentyConstants.CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    private fun writeCharacteristic(payload: ByteArray) {
        val gatt = bluetoothGatt ?: return
        val char = commCharacteristic ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, payload, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            char.value = payload
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    private fun parsePayload(data: ByteArray) {
        if (data.size < 15) return
        if (data[0].toInt() == 0x01) {
            val curTempRaw = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
            val tgtTempRaw = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)

            val curTemp = if (curTempRaw == 0x8000) 0.0f else curTempRaw / 10.0f
            val tgtTemp = tgtTempRaw / 10.0f
            val batt = data[8].toInt() and 0xFF
            val timer = (data[9].toInt() and 0xFF) + (data[10].toInt() and 0xFF)
            val modeCode = data[11].toInt() and 0xFF
            val heaterMode = HeaterMode.fromCode(modeCode)

            _state.value = _state.value.copy(
                actualTemp = curTemp,
                targetTemp = tgtTemp,
                battery = batt,
                timerSeconds = timer,
                heaterMode = heaterMode
            )
            updateNotification()

            if (timer <= thresholdSeconds && heaterMode != HeaterMode.STANDBY && !isSequenceInProgress) {
                val now = System.currentTimeMillis()
                if (now - lastKeepAliveTime > 15000L) {
                    lastKeepAliveTime = now
                    executeAdaptiveReset(heaterMode)
                }
            }
        }
    }

    private fun executeAdaptiveReset(currentMode: HeaterMode) {
        if (isSequenceInProgress) return
        isSequenceInProgress = true

        serviceScope.launch {
            Log.i(TAG, "Executing Keep-Alive Reset for mode $currentMode (Timer <= $thresholdSeconds)")
            when (currentMode) {
                HeaterMode.HEATING -> {
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(2))
                    delay(300)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(0))
                    delay(150)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(1))
                }
                HeaterMode.BOOST -> {
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(0))
                    delay(150)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(1))
                    delay(200)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(2))
                }
                HeaterMode.SUPERBOOST -> {
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(0))
                    delay(150)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(1))
                    delay(200)
                    writeCharacteristic(VentyConstants.createHeaterModeFrame(3))
                }
                HeaterMode.STANDBY -> {}
            }
            delay(300)
            _state.value = _state.value.copy(
                resetCount = _state.value.resetCount + 1,
                lastResetTimeMillis = System.currentTimeMillis(),
                statusMessage = "Keep-Alive bounce executed!"
            )
            updateNotification()
            isSequenceInProgress = false
        }
    }

    private fun startPollingJob() {
        serviceScope.launch {
            while (isActive) {
                if (_state.value.connected && !isSequenceInProgress) {
                    writeCharacteristic(VentyConstants.STATUS_REQUEST_FRAME)
                }
                delay(1000)
            }
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VentyWatchdogService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VentyApp.CHANNEL_ID)
            .setContentTitle("Venty Keep Alive")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val s = _state.value
        val summary = if (s.connected) {
            "${s.deviceName}: Target: ${s.targetTemp}°C | ${s.battery}% | Shutoff: ${s.timerSeconds}s [${s.heaterMode.label}]"
        } else {
            s.statusMessage
        }
        val notification = buildNotification(summary)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }
}
