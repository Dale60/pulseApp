package com.edenworth.pulseloggerinstaller.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log

data class BLEDevice(
    val device: BluetoothDevice,
    val rssi: Int,
    val scanRecord: ByteArray? = null
)

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDiscoveryScreen(onDeviceSelected: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf(listOf<BLEDevice>()) }
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BLEDevice?>(null) }
    var showDeviceDetailsDialog by remember { mutableStateOf(false) }

    // Permissions for Android 12+
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            permissionGranted = results.values.all { it }
            permissionRequested = true
            permanentlyDenied = results.any { !it.value && !shouldShowRationale(context, it.key) }
        }
    )

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = BLEDevice(
                    device = result.device,
                    rssi = result.rssi,
                    scanRecord = result.scanRecord?.bytes
                )
                devices = devices.filter { it.device.address != device.device.address } + device
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BLEScan", "Scan failed with error: $errorCode")
                isScanning = false
            }
        }
    }

    fun startDiscovery() {
        if (bluetoothLeScanner != null) {
            devices = emptyList()
            bluetoothLeScanner.startScan(scanCallback)
            isScanning = true
        }
    }

    fun stopDiscovery() {
        bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BLE Device Discovery", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        
        Button(onClick = { showFilterDialog = true }, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Filter: ${if (filter.isBlank()) "(none)" else filter}")
        }
        
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Set Device Name Filter") },
                text = {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        label = { Text("Device name contains...") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        filter = ""
                        showFilterDialog = false
                    }) {
                        Text("Clear")
                    }
                }
            )
        }

        if (!permissionGranted) {
            if (permanentlyDenied) {
                Text("Bluetooth and Location permissions are permanently denied. Please enable them in app settings.", modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open App Settings")
                }
            } else {
                Text("Bluetooth and Location permissions are required for device discovery.", modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Grant Bluetooth Permissions")
                }
            }
        } else {
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Button(onClick = { startDiscovery() }, enabled = !isScanning) {
                    Text("Start BLE Scan")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { stopDiscovery() }, enabled = isScanning) {
                    Text("Stop Scan")
                }
            }

            if (devices.isEmpty()) {
                Text("No BLE devices found yet.")
            } else {
                val filteredDevices = devices.filter { device ->
                    val name = device.device.name ?: "Unknown"
                    filter.isBlank() || name.contains(filter, ignoreCase = true)
                }
                
                if (filteredDevices.isEmpty()) {
                    Text("No devices match the filter.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(filteredDevices) { device ->
                            Button(
                                onClick = {
                                    selectedDevice = device
                                    showDeviceDetailsDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                Column {
                                    Text("${device.device.name ?: "Unknown"}")
                                    Text("Signal: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                                    Text("Address: ${device.device.address}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeviceDetailsDialog && selectedDevice != null) {
        BLEDeviceDetailsDialog(
            device = selectedDevice!!,
            onDismiss = { showDeviceDetailsDialog = false },
            onNext = {
                showDeviceDetailsDialog = false
                onDeviceSelected()
            }
        )
    }
}

@Composable
fun BLEDeviceDetailsDialog(device: BLEDevice, onDismiss: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val hasPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("BLE Device Details") },
        text = {
            Column {
                Text("Name: " + if (hasPermission) (device.device.name ?: "Unknown") else "Unknown")
                Text("Address: ${device.device.address}")
                Text("Signal Strength: ${device.rssi} dBm")
                Text("Bond State: ${device.device.bondState}")
                Text("Type: ${device.device.type}")
                if (device.scanRecord != null) {
                    Text("Advertisement Data: ${device.scanRecord.joinToString(", ") { String.format("%02X", it) }}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNext) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun shouldShowRationale(context: Context, permission: String): Boolean {
    return true
} 