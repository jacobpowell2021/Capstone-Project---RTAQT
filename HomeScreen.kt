package com.example.airqualitytracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController, vm: LatestChartViewModel = viewModel(), vmp: PredictionChartViewModel = viewModel()) {
    val systemUiController = rememberSystemUiController()
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
    // For current local time (not database)
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

    // --- MQTT state ---
    val temperatureState = remember { mutableStateOf("...") }
    val humidityState    = remember { mutableStateOf("...") }
    val particleState    = remember { mutableStateOf("...") }
    val TVOCState        = remember { mutableStateOf("...") }
    val COState          = remember { mutableStateOf("...") }
    val jsonState        = remember { mutableStateOf("...") }
    var showDialog by remember { mutableStateOf(false) }

    MqttManager.handleTemperatureUpdate(context, temperatureState) // optional

    val tempFloat = temperatureState.value.toFloatOrNull()
    val humidityFloat = humidityState.value.toFloatOrNull()

    LaunchedEffect(tempFloat) {
        if (tempFloat != null && tempFloat < 33) {
            showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 33°F")
        } else if (tempFloat != null && tempFloat > 100) {
            showSystemNotification(context, "High Temperature Alert", "Temperature Above 100°F")
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

        BottomItem("Charts", Routes.DataScreen, {
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
                    text = "Home",
                    color = Color.White,
                    fontSize = 30.sp
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center

            ) {
                var checkedTemperature by remember { mutableStateOf(true) }
                var checkedHumidity    by remember { mutableStateOf(true) }
                var checkedParticle    by remember { mutableStateOf(true) }
                var checkedTVOC        by remember { mutableStateOf(true) }
                var checkedCO          by remember { mutableStateOf(true) }

                TelemetryRow(
                    label = "Temperature:",
                    value = vm.tempPoints.lastOrNull()?.y.toString(),
                    unit  = "°F",
                    checked = checkedTemperature
                ) {}

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "Humidity:",
                    value = vm.humidityPoints.lastOrNull()?.y.toString(),
                    unit  = "%",
                    checked = checkedHumidity
                ) {}

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "Flammable Gases:",
                    value = vm.flammablePoints.lastOrNull()?.y.toString(),
                    unit  = "%",
                    checked = checkedParticle
                ) {
                    checkedParticle = it
                    MqttManager.publish("sensor/particle_switch", it.toString())
                }

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "TVOC:",
                    value = vm.tvocPoints.lastOrNull()?.y.toString(),
                    unit  = "ppm",
                    checked = checkedTVOC
                ) {
                    checkedTVOC = it
                    MqttManager.publish("sensor/TVOC_switch", it.toString())
                }

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "CO:",
                    value = vm.coPoints.lastOrNull()?.y.toString(),
                    unit  = "ppm",
                    checked = checkedCO
                ) {
                    checkedCO = it
                    MqttManager.publish("sensor/CO_Switch", it.toString())
                }

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "Battery Life: ",
                    value = vm.latestBattery.toString(),
                    unit  = "%",
                    checked = checkedCO
                ) {
                    checkedCO = it
                    MqttManager.publish("sensor/CO_Switch", it.toString())
                }

                Spacer(Modifier.height(16.dp))

                TelemetryRow(
                    label = "Time Last Checked:",
                    value = latestTime.first, // "05:40:01"
                    unit  = latestTime.second, // "PM" or "AM"
                    checked = checkedCO
                ) {
                    checkedCO = it
                    MqttManager.publish("sensor/CO_Switch", it.toString())
                }

                Button(
                    onClick = {
                        vm.fetch()

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Reload")
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
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit

) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape) // rounds the border corners
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape) // 👈 outline
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
    message: String = "Preparing charts…",
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
}

//Old Code that still works. DO NOT DELETE
/*import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.example.airqualitytracker.ui.theme.Maroon
import kotlin.text.toFloatOrNull
//for push notifications
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.airqualitytracker.MqttManager.handleTemperatureUpdate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Composable
fun HomeScreen(navController: NavController){//have to pass navController to each screen in order to recognize route
        val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(//check for request permission
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (!isGranted) {
                Log.d("Notification", "Permission denied")
            }
        }
    )
    // Check & ask for permission if not granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
        val temperatureState = remember { mutableStateOf("...") }
        val humidityState = remember { mutableStateOf("...") }
        val particleState = remember { mutableStateOf("...") }
        var showDialog by remember { mutableStateOf(false) }

        handleTemperatureUpdate(context,temperatureState)//debugging
        // Trying to convert temperature string to Float to check for alerts
        val tempFloat = temperatureState.value.toFloatOrNull()
        val humidityFloat = humidityState.value.toFloatOrNull()

        // Show dialog if temp < 5
        LaunchedEffect(tempFloat) {
            if (tempFloat != null && tempFloat < 33) {//alerts of low freezing temperature
                showSystemNotification(context, "Low Temperature Alert", "Temperature dropped below 33°F")
            }
            else if (tempFloat != null && tempFloat > 100){//alerts of high temperature
                showSystemNotification(context, "High Temperature Alert", "Temperature Above 100°F")

            }
        }
        LaunchedEffect(humidityFloat) {
            if (humidityFloat != null && humidityFloat > 60) {//alerts of low freezing temperature
                showSystemNotification(context, "High Humidity", "Humidity is above 60%")
            }
        }

        // Listen for MQTT values
        LaunchedEffect(Unit) {
            MqttManager.startListening(temperatureState, humidityState, particleState)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) { Column (
            Modifier.fillMaxSize()
                .padding(vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top)
        {
            Text(
                text = "Home",
                color = Color.White,
                fontSize = 30.sp
            )
        }
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center

            ) {


                var checkedTemperature by remember { mutableStateOf(true) }//(3/4/25)adds variables for individual states of switches
                var checkedHumidity by remember { mutableStateOf(true) }
                var checkedParticle by remember { mutableStateOf(true) }

                Row (
                        modifier = Modifier//spaces the two text boxes and switch evenly
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ){//separates data from switch

                    Text(//This text will take the the server data and display it
                        text = "Temperature:",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "${temperatureState.value}°F",
                        color = Color.White
                    )
                    Switch(//adds switch to the home screen for turning off and on of sensors. NOTE(2/24/2025): Still have to implement for the switch to communicate that to Server COMPLETED
                        //have to add memory outside from past login
                        checked = checkedTemperature,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Green
                        ),
                        onCheckedChange = {
                            var currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            checkedTemperature = it
                            MqttManager.publish("sensor/temperature_switch",it.toString() + " $currentTime")//publishes the boolean to this topic
                        }
                    )
                }
                Row (
                    modifier = Modifier//spaces the two text boxes and switch evenly
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ){//separates text, data and switch
                    Text(//This text will take the the server data and display it
                        text = "Humidity:",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "${humidityState.value}%",
                        color = Color.White
                    )
                    Switch(//adds switch to the home screen for turning off and on of sensors. NOTE: Still have to implement for the switch to communicate that to Server
                        checked = checkedHumidity,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Green
                        ),
                        onCheckedChange = {
                            checkedHumidity = it
                            MqttManager.publish("sensor/Humidity_switch",it.toString())//publishes the boolean to this topic
                        }
                    )
                }
                Row (
                    modifier = Modifier//spaces the two text boxes and switch evenly
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ){//separates text, data and switch
                    Text(//This text will take the the server data and display it
                        text = "Particle:",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                    Text(
                        text = "${particleState.value}%",
                        color = Color.White
                    )
                    Switch(//adds switch to the home screen for turning off and on of sensors. NOTE: Still have to implement for the switch to communicate that to Server
                        checked = checkedParticle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.Green
                        ),
                        onCheckedChange = {
                            checkedParticle = it
                            MqttManager.publish("sensor/particle_switch",it.toString())//publishes the boolean to this topic
                        }
                    )
                }
            }
            if (showDialog) {//detects the change in showDialog for temperature
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Low Temperature Alert") },
                    text = { Text("Warning: Temperature is below 33°F!") },
                    confirmButton = {
                        Button(onClick = { showDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            Button(onClick = {
                navController.navigate(Routes.NavigationScreen)
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White       // Text color
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()

            ) {
                Text(
                    text = "Go to Navigation Screen",
                    color = Color.White,
                )
            }
        }
    }
*/