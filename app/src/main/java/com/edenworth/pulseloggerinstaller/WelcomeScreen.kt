package com.edenworth.pulseloggerinstaller

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.grey_watermark_logo),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            alpha = 0.75f, // control how visible the watermark is
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.edenworth_blue_logo),
                contentDescription = "EdenWorth Logo",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp)
            )

            Text(
                text = "Pulse Logger Installer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = "Quickly configure and verify NB-IoT loggers",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .height(50.dp)
                    .width(180.dp)
            ) {
                Text("Get Started")
            }
        }
    }
}
