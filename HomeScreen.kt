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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    LaunchedEffect(Unit) {
        delay(1000L)
        vm.fetch()
    }

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
    }

    val temperatureState = remember { mutableStateOf("...") }
    val humidityState    = remember { mutableStateOf("...") }
    val particleState    = remember { mutableStateOf("...") }
    val TVOCState        = remember { mutableStateOf("...") }
    val COState          = remember { mutableStateOf("...") }
    val jsonState        = remember { mutableStateOf("...") }
    var showDialog by remember { mutableStateOf(false) }

    val tempFloat = vm.tempPoints.lastOrNull()?.y
    val humidityFloat = vm.humidityPoints.lastOrNull()?.y

    LaunchedEffect(tempFloat) {
        if (tempFloat != null && tempFloat < 33) {
            showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 33°F")
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
                    MetricCard(
                        label = "Temperature",
                        value = tempText,
                        unit = "°F",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Humidity",
                        value = humText,
                        unit = "%",
                        modifier = Modifier.weight(1f)
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

                DetailedMetricCard(
                    label = "Flammable Gases",
                    value = flammText,
                    unit = "ppm"
                )
                Spacer(Modifier.height(12.dp))

                DetailedMetricCard(
                    label = "Total VOC",
                    value = tvocText,
                    unit = "ppb"
                )
                Spacer(Modifier.height(12.dp))

                DetailedMetricCard(
                    label = "Carbon Monoxide",
                    value = coText,
                    unit = "ppm"
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

                DetailedMetricCard(
                    label = "Battery Level",
                    value = battText,
                    unit = "%"
                )

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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    color = Maroon,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DetailedMetricCard(
    label: String,
    value: String,
    unit: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    color = Color.White,
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

//OLD CODE BEFORE UI UPDATE
/*package com.example.airqualitytracker

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

@Composable
fun HomeScreen(navController: NavController, vm: LatestChartViewModel = viewModel()) {
    val systemUiController = rememberSystemUiController()

    val latest = vm.latestSnapshot
    // Show placeholders if null
    val tempText = latest?.let { "%.1f".format(it.temperatureF) } ?: "Loading "
    val humText  = latest?.let { "%.2f".format(it.humidity) } ?: "Loading "
    val flammText= latest?.let { "%.2f".format(it.flammable) } ?: "Loading "
    val tvocText = latest?.let { "%.2f".format(it.tvoc) } ?: "Loading "
    val coText   = latest?.let { "%.2f".format(it.co) } ?: "Loading "
    val battText = latest?.batteryPct?.toString() ?: "Loading "
    val timeText = latest?.eventLocalTime ?: "Loading "

    LaunchedEffect(Unit) {
        delay(1000L)
        vm.fetch()
    }

    if (vm.isLoading){
        LoadingHomeOverlay()
    }//TRYING ADD THE HOME LOADING SCREEN
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Maroon,   // your custom color
            darkIcons = true // false = white icons (good for dark bars)
        )
    }
    // For current local time
    val now = LocalDateTime.now()
    val currentZone = now.atZone(ZoneId.systemDefault())

    // Extract formatted time and AM/PM
    val timeOnly = currentZone.format(DateTimeFormatter.ofPattern("hh:mm", Locale.US))
    val ampm = currentZone.format(DateTimeFormatter.ofPattern("a", Locale.US))

    val latestTime = Pair(timeOnly, ampm)


    val context = LocalContext.current

    // --- Notification permission ---
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
    }

    // --- state ---
    val temperatureState = remember { mutableStateOf("...") }
    val humidityState    = remember { mutableStateOf("...") }
    val particleState    = remember { mutableStateOf("...") }
    val TVOCState        = remember { mutableStateOf("...") }
    val COState          = remember { mutableStateOf("...") }
    val jsonState        = remember { mutableStateOf("...") }
    var showDialog by remember { mutableStateOf(false) }


    val tempFloat = vm.tempPoints.lastOrNull()?.y
    val humidityFloat = vm.humidityPoints.lastOrNull()?.y

    LaunchedEffect(tempFloat) {
        if (tempFloat != null && tempFloat < 33) {
            showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 33°F")
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
        // JSON-only listener that fans values into these states
        MqttManager.startListening(temperatureState, humidityState, particleState, TVOCState, COState, jsonState)
    }

    // ----- Bottom Navigation Items -----
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
            tint = Color.Unspecified)}),

        BottomItem("Prediction", Routes.PredictionScreen , {Icon(
            painter = painterResource(R.drawable.predictionicon),
            contentDescription = "Prediction",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified)}),

        BottomItem("Specs", Routes.SpecScreen , { Icon(Icons.Filled.Info, contentDescription = "Alerts") }),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Maroon,
        bottomBar = {
            // Wrap NavigationBar in a Surface to give it shape & elevation
            Surface(
                color = Maroon,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp) // gives spacing for a "floating" look
            ) {
                NavigationBar(
                    containerColor = Color.Transparent, // surface already has color
                    contentColor = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White,
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
                .background(Color.Black)
                .padding(innerPadding)

        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Real-Time Sensor Data",
                    color = Color.White,
                    fontSize = 30.sp
                )


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            color = Color.LightGray, // outline color
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        TelemetryRow(label = "Temperature:", value = tempText, unit = "°F")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "Humidity:", value = humText, unit = "%")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "Flammable Gases:", value = flammText, unit = "ppm")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "TVOC:", value = tvocText, unit = "ppb")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "CO:", value = coText, unit = "ppm")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "Battery Life:", value = battText, unit = "%")
                        Spacer(Modifier.height(20.dp))

                        TelemetryRow(label = "Time Last Checked:", value = timeText, unit = null)
                        Spacer(Modifier.height(20.dp))

                        Button(
                            onClick = {
                                vm.fetchLatestOnly()

                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Reload")
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
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

/** Reusable aligned row so labels/values/switches line up perfectly. */
@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    unit: String? = null,

) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape) // rounds the border corners
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape) //
            .background(Maroon) // or a subtle fill if you want
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Label (left aligned, wider)
        Text(
            text = label,
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.weight(1.2f)
        )

        // Column 2: Value (right aligned)
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )

        // Column 3: Unit (fixed width to keep switches aligned)
        if (unit != null) {
            Text(
                text = unit,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.width(48.dp)
            )
        } else {
            Spacer(Modifier.width(48.dp))
        }

    }
}
@Composable
fun LoadingHomeOverlay(
    message: String = "Preparing Values…",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .background(Color.Black, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // Placeholder for your app mark
            Box(
                Modifier
            )

            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(
                color = Maroon
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White)
        }
    }
}*/