package com.example.airqualitytracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airqualitytracker.ui.theme.Maroon

/**
 * Optional: Add this to your HomeScreen or Settings screen
 * to let users control background monitoring
 */
@Composable
fun BackgroundMonitoringCard() {
    val context = LocalContext.current
    var isMonitoringEnabled by remember {
        mutableStateOf(BackgroundMonitoringService.isMonitoringActive(context))
    }
    var checkIntervalSeconds by remember { mutableStateOf(30) }
    var intervalInput by remember { mutableStateOf("30") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Maroon.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Background Monitoring",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isMonitoringEnabled) "Checking every $checkIntervalSeconds sec" else "Disabled",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Switch(
                    checked = isMonitoringEnabled,
                    onCheckedChange = { enabled ->
                        isMonitoringEnabled = enabled
                        if (enabled) {
                            BackgroundMonitoringService.startMonitoring(context, checkIntervalSeconds.toLong())
                        } else {
                            BackgroundMonitoringService.stopMonitoring(context)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Maroon,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray
                    )
                )
            }

            if (isMonitoringEnabled) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Check Interval",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                intervalInput = newValue
                            }
                        },
                        label = { Text("Seconds", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Maroon,
                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = Maroon
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val newInterval = intervalInput.toIntOrNull()
                            if (newInterval != null && newInterval >= 5) { // Minimum 5 seconds for testing
                                checkIntervalSeconds = newInterval
                                BackgroundMonitoringService.startMonitoring(context, newInterval.toLong())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Apply", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "Minimum: 5 seconds (TESTING MODE - Will drain battery faster!)",
                    color = Color(0xFFFF6B6B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        BackgroundMonitoringService.triggerImmediateCheck(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon.copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Check Now", fontSize = 14.sp)
                }
            }

            Divider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = "Receive notifications for concerning sensor readings even when the app is closed",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}