package com.example.airqualitytracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, vm: LatestChartViewModel = viewModel()) {
    val systemUiController = rememberSystemUiController()

    val latest = vm.latestSnapshot
    val tempText = latest?.let { "%.1f".format(it.temperatureF) } ?: "--"
    val humText  = latest?.let { "%.1f".format(it.humidity) } ?: "--"
    val flammText= latest?.let { "%.2f".format(it.flammable) } ?: "--"
    val tvocText = latest?.let { "%.2f".format(it.tvoc) } ?: "--"
    val coText   = latest?.let { "%.2f".format(it.co) } ?: "--"
    val battText = latest?.batteryPct?.toString() ?: "--"
    val timeText = latest?.eventLocalTime ?: "--"

    if (vm.isLoading){
        LoadingHomeOverlay()
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Maroon,
            darkIcons = true
        )
    }

    val now = LocalDateTime.now()
    val currentZone = now.atZone(ZoneId.systemDefault())
    val timeOnly = currentZone.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    val ampm = currentZone.format(DateTimeFormatter.ofPattern("a", Locale.US))
    val latestTime = Pair(timeOnly, ampm)

    val context = LocalContext.current

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> if (!isGranted) Log.d("Notification", "Permission denied") }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Start background monitoring service
        BackgroundMonitoringService.startMonitoring(context)
        Log.d("HomeScreen", "Background monitoring service started")
    }

    val temperatureState = remember { mutableStateOf("...") }
    val humidityState    = remember { mutableStateOf("...") }
    val particleState    = remember { mutableStateOf("...") }
    val TVOCState        = remember { mutableStateOf("...") }
    val COState          = remember { mutableStateOf("...") }
    val jsonState        = remember { mutableStateOf("...") }
    var showDialog by remember { mutableStateOf(false) }

    // Refresh interval state (in seconds)
    var refreshIntervalSeconds by remember { mutableStateOf(30) }
    var refreshIntervalInput by remember { mutableStateOf("30") }

    // Auto-refresh effect that runs only while on this page
    // When you navigate away, this LaunchedEffect is cancelled automatically
    // Restart when refreshIntervalSeconds changes
    LaunchedEffect(refreshIntervalSeconds) {
        Log.d("HomeScreen", "Auto-refresh started with ${refreshIntervalSeconds}s interval")

        // Initial fetch after 1 second
        delay(1000L)
        Log.d("HomeScreen", "Initial fetch")
        vm.fetch()

        // Auto-refresh at the specified interval
        while (true) {
            delay(refreshIntervalSeconds * 1000L) // Convert seconds to milliseconds
            Log.d("HomeScreen", "Auto-refresh fetch (${refreshIntervalSeconds}s interval)")
            vm.fetchLatestOnly()
        }
    }

    val tempFloat = vm.tempPoints.lastOrNull()?.y
    val humidityFloat = vm.humidityPoints.lastOrNull()?.y

    LaunchedEffect(tempFloat) {
        if (tempFloat != null && tempFloat < 60) {
            showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 66°F")
        } else if (tempFloat != null && tempFloat > 80) {
            showSystemNotification(context, "High Temperature Alert", "Temperature Above 80°F")
        }
    }

    LaunchedEffect(humidityFloat) {
        if (humidityFloat != null && humidityFloat > 60) {
            showSystemNotification(context, "High Humidity", "Humidity is above 60%")
        }
    }

    LaunchedEffect(Unit) {
        MqttManager.startListening(temperatureState, humidityState, particleState, TVOCState, COState, jsonState)
    }

    data class BottomItem(
        val label: String,
        val route: String,
        val icon: @Composable () -> Unit
    )

    val items = listOf(
        BottomItem("Home", Routes.HomeScreen, { Icon(Icons.Filled.Home, contentDescription = "Home") }),
        BottomItem("Charts", Routes.ChartsScreen, {
            Icon(
                painter = painterResource(R.drawable.baricon),
                contentDescription = "Charts",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }),
        /*BottomItem("Prediction", Routes.PredictionScreen, {
            Icon(
                painter = painterResource(R.drawable.predictionicon),
                contentDescription = "Prediction",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }),*/
        BottomItem("Specs", Routes.SpecScreen, { Icon(Icons.Filled.Info, contentDescription = "Alerts") }),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.fetchLatestOnly() },
                containerColor = Maroon,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reload", modifier = Modifier.size(28.dp))
            }
        },
        bottomBar = {
            Surface(
                color = Maroon,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    }
                                }
                            },
                            icon = { item.icon() },
                            label = { Text(item.label) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                indicatorColor = Black
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Air Quality",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-Time Monitoring",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Maroon.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Last Updated",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = timeText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Temperature & Humidity Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val tempValue = tempText.toFloatOrNull() ?: 0f
                    val isTempConcerning = tempValue > 75f || tempValue < 60f

                    MetricCard(
                        label = "Temperature",
                        value = tempText,
                        unit = "°F",
                        modifier = Modifier.weight(1f),
                        isConcerning = isTempConcerning && tempText != "--",
                        warningIcon = if (isTempConcerning && tempText != "--") "⚠️" else null
                    )

                    val humValue = humText.toFloatOrNull() ?: 0f
                    val isHumConcerning = humValue > 60f || humValue < 30f

                    MetricCard(
                        label = "Humidity",
                        value = humText,
                        unit = "%",
                        modifier = Modifier.weight(1f),
                        isConcerning = isHumConcerning && humText != "--",
                        warningIcon = if (isHumConcerning && humText != "--") "⚠️" else null
                    )
                }

                // Gas Readings Section
                Text(
                    text = "Gas Concentrations",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp, top = 8.dp)
                )

                val flammValue = flammText.toFloatOrNull() ?: 0f
                val isFlammConcerning = flammValue > 0.1f

                DetailedMetricCard(
                    label = "Flammable Gases",
                    value = flammText,
                    unit = "ppm",
                    isConcerning = isFlammConcerning && flammText != "--"
                )
                Spacer(Modifier.height(12.dp))

                val tvocValue = tvocText.toFloatOrNull() ?: 0f
                val isTvocConcerning = tvocValue > 50f

                DetailedMetricCard(
                    label = "Total VOC",
                    value = tvocText,
                    unit = "ppb",
                    isConcerning = isTvocConcerning && tvocText != "--"
                )
                Spacer(Modifier.height(12.dp))

                val coValue = coText.toFloatOrNull() ?: 0f
                val isCoConcerning = coValue > 2f

                DetailedMetricCard(
                    label = "Carbon Monoxide",
                    value = coText,
                    unit = "ppm",
                    isConcerning = isCoConcerning && coText != "--"
                )
                Spacer(Modifier.height(12.dp))

                // Battery Status
                Text(
                    text = "Device Status",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp, top = 8.dp)
                )

                val battValue = battText.toIntOrNull() ?: 100
                val isBattConcerning = battValue < 20

                DetailedMetricCard(
                    label = "Battery Level",
                    value = battText,
                    unit = "%",
                    isConcerning = isBattConcerning && battText != "--"
                )

                Spacer(Modifier.height(24.dp))

                // Refresh Interval Control Section
                Text(
                    text = "Auto-Refresh Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Maroon.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Refresh Rate: ${refreshIntervalSeconds}s",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.OutlinedTextField(
                                value = refreshIntervalInput,
                                onValueChange = { newValue ->
                                    // Only allow digits
                                    if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                        refreshIntervalInput = newValue
                                    }
                                },
                                label = { Text("Seconds", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Maroon,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    cursorColor = Maroon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )

                            Button(
                                onClick = {
                                    val newInterval = refreshIntervalInput.toIntOrNull()
                                    if (newInterval != null && newInterval > 0) {
                                        refreshIntervalSeconds = newInterval
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

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Enter refresh interval (1-9999 seconds)",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Background Monitoring Section
                Text(
                    text = "Background Monitoring",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                BackgroundMonitoringCard()

                Spacer(Modifier.height(80.dp))
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Low Temperature Alert") },
                    text = { Text("Warning: Temperature is below 33°F!") },
                    confirmButton = {
                        Button(onClick = { showDialog = false }) { Text("OK") }
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    isConcerning: Boolean = false,
    warningIcon: String? = null
) {
    Card(
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConcerning) 4.dp else 4.dp),
        border = if (isConcerning) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B6B)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isConcerning)
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    else Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isConcerning && warningIcon != null) {
                    Text(
                        text = warningIcon,
                        fontSize = 16.sp
                    )
                }
            }
            Column {
                Text(
                    text = value,
                    color = if (isConcerning) Color(0xFFFF6B6B) else Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = unit,
                        color = Maroon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isConcerning) {
                        Text(
                            text = "⚠",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedMetricCard(
    label: String,
    value: String,
    unit: String,
    isConcerning: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isConcerning) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B6B)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isConcerning)
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    else Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                )
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isConcerning) {
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = if (isConcerning) Color(0xFFFF6B6B) else Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingHomeOverlay(
    message: String = "Loading sensor data…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 40.dp)
        ) {
            CircularProgressIndicator(
                color = Maroon,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}