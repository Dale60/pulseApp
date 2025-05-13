package com.edenworth.pulseloggerinstaller.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.edenworth.pulseloggerinstaller.R

@Composable
fun CommissioningIntroScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Image(
            painter = painterResource(id = R.drawable.grey_watermark_logo),
            contentDescription = "Optima Logger",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Text(
            text = "Commissioning",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Wake the device using a magnet near the logo area. Observe the flashing LED, then scan the QR code on the device.",
            fontSize = 16.sp,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Button(onClick = {
            // Navigate to QR scan screen (adjust route name if needed)
            navController.navigate("qr_scan")
        }) {
            Text("Scan QR Code")
        }
    }
}
