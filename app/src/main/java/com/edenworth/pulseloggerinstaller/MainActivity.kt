package com.edenworth.pulseloggerinstaller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.edenworth.pulseloggerinstaller.ui.screens.WakeUpAndScanScreen
import com.edenworth.pulseloggerinstaller.ui.screens.QrScannerScreen
import com.edenworth.pulseloggerinstaller.ui.screens.BluetoothDiscoveryScreen
import com.edenworth.pulseloggerinstaller.ui.screens.MeterCaptureScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "welcome"
                    ) {
                        composable("welcome") {
                            WelcomeScreen {
                                navController.navigate("wakeup")
                            }
                        }
                        
                        composable("wakeup") {
                            WakeUpAndScanScreen(
                                onActivated = {
                                    // This will be called when the user clicks "I've Activated the Logger"
                                },
                                onScanQrCode = {
                                    navController.navigate("qr_scan")
                                }
                            )
                        }

                        composable("qr_scan") {
                            QrScannerScreen(
                                onQrCodeScanned = { qrCode ->
                                    navController.navigate("bluetooth_discovery")
                                },
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("bluetooth_discovery") {
                            BluetoothDiscoveryScreen(
                                onDeviceSelected = { navController.navigate("meter_capture") }
                            )
                        }

                        composable("meter_capture") {
                            MeterCaptureScreen()
                        }
                    }
                }
            }
        }
    }
}