package com.example.airqualitytracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.animation.Crossfade
import co.yml.charts.common.model.Point

private const val TAG = "ChartsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(navController: NavController,
                 vm: LatestChartViewModel = viewModel(),
                 predictionVm: PredictionChartViewModel = viewModel()
) {

    LaunchedEffect(Unit) {
        try {
            vm.fetch()
            delay(500L)
            predictionVm.fetchAndBuild(1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data", e)
        }
    }

    // Shared metric selection state
    var expanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(PredMetric.Temperature) }
    var daysText by remember { mutableStateOf("1.0") }

    val isLoading = vm.isLoading

    if (isLoading) {
        LoadingOverlay()
    } else {

        data class BottomItem(
            val label: String,
            val route: String,
            val icon: @Composable () -> Unit
        )

        val items = listOf(
            BottomItem(
                "Home",
                Routes.HomeScreen,
                { Icon(Icons.Filled.Home, contentDescription = "Home") }),

            BottomItem("Charts", Routes.DataScreen, {
                Icon(
                    painter = painterResource(R.drawable.baricon),
                    contentDescription = "Charts",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }),

            BottomItem("Prediction", Routes.PredictionScreen, {
                Icon(
                    painter = painterResource(R.drawable.predictionicon),
                    contentDescription = "Prediction",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }),
            BottomItem(
                "Specs",
                Routes.SpecScreen,
                { Icon(Icons.Filled.Info, contentDescription = "Alerts") }),
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            containerColor = Maroon,
            bottomBar = {
                Surface(
                    color = Maroon,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
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
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Shared Dropdown for metric selection
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            readOnly = true,
                            value = selectedMetric.label,
                            onValueChange = {},
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text("Select metric", color = Color.White) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Maroon,
                                unfocusedContainerColor = Maroon,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White,
                                cursorColor = Color.White,
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF121212))
                        ) {
                            PredMetric.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label,
                                            color = if (option == selectedMetric) Color.Cyan else Color.White
                                        )
                                    },
                                    onClick = {
                                        selectedMetric = option
                                        expanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = Color.White,
                                        disabledTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Last 12 Hours Section
                    Text(
                        "Last 12 Hours",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (!vm.isLoading) {
                        LatestChartForMetric(selectedMetric)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Prediction controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = daysText,
                            onValueChange = { daysText = it },
                            label = { Text("days") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                cursorColor = Color.White,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.White,
                                focusedContainerColor = Maroon,
                                unfocusedContainerColor = Maroon
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { daysText.toFloatOrNull()?.let(predictionVm::fetchAndBuild) },
                            enabled = !predictionVm.isLoading.value
                        ) {
                            Text(if (predictionVm.isLoading.value) "Loading…" else "Fetch")
                        }
                    }

                    // Prediction Section
                    Text(
                        "Predictions",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    predictionVm.error.value?.let { errorMsg ->
                        Text(
                            "Error: $errorMsg",
                            color = Color(0xFFFFB4A9),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (predictionVm.isLoading.value) {
                        CircularProgressIndicator(
                            color = Maroon,
                            modifier = Modifier.padding(32.dp)
                        )
                    } else {
                        PredictionChartForMetric(selectedMetric, predictionVm)
                    }
                }
            }
        }
    }
}

// Helper composable to show the latest chart for selected metric
@Composable
fun LatestChartForMetric(metric: PredMetric, vm: LatestChartViewModel = viewModel()) {
    val points = when (metric) {
        PredMetric.Temperature -> vm.tempPoints
        PredMetric.Humidity -> vm.humidityPoints
        PredMetric.Flammable -> vm.flammablePoints
        PredMetric.TVOC -> vm.tvocPoints
        PredMetric.CO -> vm.coPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = metric.label,
            points = points,
            color = metric.color,
            fixedYRange = metric.yRange,
            xLabels = vm.xLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    } else {
        Text(
            "No data available for ${metric.label}",
            color = Color.Gray,
            modifier = Modifier.padding(32.dp)
        )
    }
}

// Helper composable to show prediction chart for selected metric
@Composable
fun PredictionChartForMetric(metric: PredMetric, vm: PredictionChartViewModel) {
    val points = when (metric) {
        PredMetric.Temperature -> vm.tempPoints
        PredMetric.Humidity -> vm.humidityPoints
        PredMetric.Flammable -> vm.flammablePoints
        PredMetric.TVOC -> vm.tvocPoints
        PredMetric.CO -> vm.coPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = metric.label,
            points = points,
            color = metric.color,
            fixedYRange = metric.yRange,
            xLabels = vm.xLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    } else {
        Text(
            "Enter number of days and click Fetch to load predictions",
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 24.dp)
        )
    }
}

@Composable
fun CombinedLoadingOverlay(
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
            Box(Modifier)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Maroon)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color.White)
        }
    }
}