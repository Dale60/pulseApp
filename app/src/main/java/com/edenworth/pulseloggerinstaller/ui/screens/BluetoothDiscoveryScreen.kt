package com.edenworth.pulseloggerinstaller.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDiscoveryScreen(onDeviceSelected: () -> Unit) {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var isScanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionRequested by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
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
            // If any permission is denied and shouldShowRequestPermissionRationale is false, it's permanently denied
            permanentlyDenied = results.any { !it.value && !shouldShowRationale(context, it.key) }
        }
    )

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (devices.none { it.address == device.address }) {
                            devices = devices + device
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    fun startDiscovery() {
        devices = emptyList()
        bluetoothAdapter?.startDiscovery()
        isScanning = true
    }

    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        isScanning = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bluetooth Device Discovery", modifier = Modifier.padding(bottom = 16.dp))
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
                    Text("Start Scan")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { stopDiscovery() }, enabled = isScanning) {
                    Text("Stop Scan")
                }
            }
            if (devices.isEmpty()) {
                Text("No devices found yet.")
            } else {
                val filteredDevices = devices.filter { device ->
                    val name = device.name ?: "Unknown"
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
                                Text("${device.name ?: "Unknown"} - ${device.address}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeviceDetailsDialog && selectedDevice != null) {
        DeviceDetailsDialog(
            device = selectedDevice!!,
            onDismiss = { showDeviceDetailsDialog = false },
            onNext = {
                showDeviceDetailsDialog = false
                onDeviceSelected()
            }
        )
    }
}

private fun shouldShowRationale(context: Context, permission: String): Boolean {
    // This is a stub for demo purposes. In a real app, you would use ActivityCompat.shouldShowRequestPermissionRationale
    // but in Compose, you may need to pass an Activity reference or use Accompanist permissions.
    return true // Always return true for now (so we don't mark as permanently denied by accident)
}

@Composable
fun DeviceDetailsDialog(device: BluetoothDevice, onDismiss: () -> Unit, onNext: () -> Unit) {
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
        title = { Text("Device Details") },
        text = {
            Column {
                Text("Name: " + if (hasPermission) (device.name ?: "Unknown") else "Unknown")
                Text("Address: ${device.address}")
                Text("Bond State: ${device.bondState}")
                Text("Type: ${device.type}")
                // Add more attributes as needed
            }
        },
        confirmButton = {
            TextButton(onClick = onNext) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
} 