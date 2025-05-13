package com.edenworth.pulseloggerinstaller.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edenworth.pulseloggerinstaller.R

@Composable
fun WakeUpAndScanScreen(
    onActivated: () -> Unit,
    onScanQrCode: () -> Unit
) {
    var showQrStep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Step 2: Activate Your Logger",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.grey_watermark_logo),
            contentDescription = "Activation Video/Image",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Hold the magnet to the logger and look for the LED blink. This indicates the device is awake.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!showQrStep) {
            Button(onClick = {
                showQrStep = true
                onActivated()
            }) {
                Text("I've Activated the Logger")
            }
        } else {
            Text(
                text = "Now scan the QR code on the logger to identify this device.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            )

            Button(onClick = onScanQrCode) {
                Text("Scan QR Code")
            }
        }
    }
} 