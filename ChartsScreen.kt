package com.example.airqualitytracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

private const val TAG = "ChartsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    navController: NavController,
    vm: LatestChartViewModel = viewModel(),
    predictionVm: PredictionChartViewModel = viewModel()
) {
    // Refresh interval state (in seconds)
    var refreshIntervalSeconds by remember { mutableStateOf(30) }
    var refreshIntervalInput by remember { mutableStateOf("30") }

    LaunchedEffect(Unit) {
        try {
            vm.fetch()
            delay(500L)
            predictionVm.fetchAndBuild(1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data", e)
        }
    }

    // Auto-refresh effect with dynamic interval
    LaunchedEffect(refreshIntervalSeconds) {
        Log.d(TAG, "Auto-refresh started with ${refreshIntervalSeconds}s interval")
        while (true) {
            delay(refreshIntervalSeconds * 1000L)
            Log.d(TAG, "Auto-refresh fetch (${refreshIntervalSeconds}s interval)")
            try {
                vm.fetch()
            } catch (e: Exception) {
                Log.e(TAG, "Auto-refresh error", e)
            }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(Metric.Temperature) }
    var daysText by remember { mutableStateOf("1.0") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val isLoading = vm.isLoading

    if (isLoading) {
        CombinedLoadingOverlay()
    } else {
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
            BottomItem("Specs", Routes.SpecScreen, { Icon(Icons.Filled.Info, contentDescription = "Alerts") }),
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            containerColor = Color(0xFF0A0A0A),
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
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "Data Analytics",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Historical & Predictive Charts",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Light
                        )

                        Spacer(Modifier.height(16.dp))

                        // Auto-Refresh Settings Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Maroon.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    OutlinedTextField(
                                        value = refreshIntervalInput,
                                        onValueChange = { newValue ->
                                            if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                                refreshIntervalInput = newValue
                                            }
                                        },
                                        label = { Text("Seconds", color = Color.White.copy(alpha = 0.7f)) },
                                        modifier = Modifier.weight(1f),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            focusedBorderColor = Maroon,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                            cursorColor = Maroon,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number
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
                    }

                    // Metric Selector Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Select Metric",
                                color = Maroon,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    readOnly = true,
                                    value = selectedMetric.label,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.KeyboardArrowDown,
                                            contentDescription = "Dropdown",
                                            tint = Maroon
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Maroon,
                                        unfocusedBorderColor = Maroon.copy(alpha = 0.5f),
                                        cursorColor = Maroon,
                                        focusedContainerColor = Color(0xFF2A2A2A),
                                        unfocusedContainerColor = Color(0xFF2A2A2A)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .background(Color(0xFF2A2A2A))
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Metric.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option.label,
                                                    color = if (option == selectedMetric) Maroon else Color.White,
                                                    fontWeight = if (option == selectedMetric) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedMetric = option
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Historical Data Section
                    SectionHeader(
                        title = "Historical Data",
                        subtitle = "Last 12 Hours"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (!vm.isLoading) {
                                LatestChartForMetric(selectedMetric, vm)
                            }
                        }
                    }

                    // Historical Statistics Cards
                    val historicalPoints = when (selectedMetric) {
                        Metric.Temperature -> vm.tempPoints
                        Metric.Humidity -> vm.humidityPoints
                        Metric.Flammable -> vm.flammablePoints
                        Metric.TVOC -> vm.tvocPoints
                        Metric.CO -> vm.coPoints
                    }

                    if (historicalPoints.isNotEmpty()) {
                        StatisticsCards(
                            points = historicalPoints,
                            metricLabel = selectedMetric.label,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }

                    // Predictive Data Section (from API)
                    SectionHeader(
                        title = "Predictive Data",
                        subtitle = " Today's Predictive Data (API Forecast)"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (!vm.isLoading) {
                                PredictiveChartForMetric(selectedMetric, vm)
                            }
                        }
                    }

                    // Predictive Statistics Cards
                    val predictivePoints = when (selectedMetric) {
                        Metric.Temperature -> vm.predictiveTempPoints
                        Metric.Humidity -> vm.predictiveHumidityPoints
                        Metric.Flammable -> vm.predictiveFlammablePoints
                        Metric.TVOC -> vm.predictiveTvocPoints
                        Metric.CO -> vm.predictiveCoPoints
                    }

                    if (predictivePoints.isNotEmpty()) {
                        StatisticsCards(
                            points = predictivePoints,
                            metricLabel = "${selectedMetric.label} (API Forecast)",
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }

                    // Prediction Controls Section
                    SectionHeader(
                        title = "Predictions",
                        subtitle = "Forecast Future Values"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Maroon.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = daysText,
                                    onValueChange = { newValue ->
                                        daysText = newValue
                                        // Validate input
                                        val floatValue = newValue.toFloatOrNull()
                                        inputError = when {
                                            floatValue == null && newValue.isNotEmpty() -> "Please enter a valid number"
                                            floatValue != null && floatValue < 0 -> "Value cannot be negative"
                                            floatValue != null && floatValue > 3 -> "Value cannot exceed 3 days"
                                            else -> null
                                        }
                                    },
                                    label = {
                                        Text(
                                            "Days to predict",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    isError = inputError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = if (inputError != null) Color(0xFFFF6B6B) else Maroon,
                                        unfocusedBorderColor = if (inputError != null) Color(0xFFFF6B6B) else Maroon.copy(alpha = 0.5f),
                                        cursorColor = Maroon,
                                        focusedLabelColor = Color.White.copy(alpha = 0.6f),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                        errorBorderColor = Color(0xFFFF6B6B),
                                        errorLabelColor = Color(0xFFFF6B6B),
                                        errorTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF2A2A2A),
                                        unfocusedContainerColor = Color(0xFF2A2A2A),
                                        errorContainerColor = Color(0xFF2A2A2A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        val floatValue = daysText.toFloatOrNull()
                                        if (floatValue != null && floatValue >= 0 && floatValue <= 3) {
                                            predictionVm.fetchAndBuild(floatValue)
                                            inputError = null
                                        }
                                    },
                                    enabled = !predictionVm.isLoading.value && inputError == null && daysText.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Maroon,
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.Gray,
                                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .shadow(4.dp, RoundedCornerShape(12.dp))
                                ) {
                                    Text(
                                        if (predictionVm.isLoading.value) "Loading…" else "Generate",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // Error message display
                            if (inputError != null) {
                                Text(
                                    text = inputError!!,
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }

                    // Prediction Chart - Current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (predictionVm.hasPreviousData.value) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Current Prediction",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(
                                        onClick = { predictionVm.clearComparison() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Maroon
                                        )
                                    ) {
                                        Text("Clear Comparison", fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                predictionVm.error.value?.let { errorMsg ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Text(
                                            "⚠️",
                                            fontSize = 32.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Text(
                                            "Error loading prediction",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            errorMsg,
                                            color = Color(0xFFFFB4A9),
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                } ?: when {
                                    predictionVm.isLoading.value -> {
                                        CircularProgressIndicator(
                                            color = Maroon,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.padding(32.dp)
                                        )
                                    }
                                    else -> {
                                        PredictionChartForMetric(selectedMetric, predictionVm)
                                    }
                                }
                            }
                        }
                    }

                    // Prediction Statistics Cards
                    val predictionPoints = when (selectedMetric) {
                        Metric.Temperature -> predictionVm.tempPoints
                        Metric.Humidity -> predictionVm.humidityPoints
                        Metric.Flammable -> predictionVm.flammablePoints
                        Metric.TVOC -> predictionVm.tvocPoints
                        Metric.CO -> predictionVm.coPoints
                    }

                    if (predictionPoints.isNotEmpty()) {
                        StatisticsCards(
                            points = predictionPoints,
                            metricLabel = "${selectedMetric.label} (Predicted)",
                            showRegressionAnalysis = false,
                            modifier = Modifier.padding(bottom = if (predictionVm.hasPreviousData.value) 16.dp else 24.dp)
                        )
                    }

                    // Previous Prediction Chart - Only shown if comparison data exists
                    if (predictionVm.hasPreviousData.value) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E).copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Previous Prediction",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                PreviousPredictionChartForMetric(selectedMetric, predictionVm)
                            }
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            color = Maroon,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun LatestChartForMetric(metric: Metric, vm: LatestChartViewModel) {
    val points = when (metric) {
        Metric.Temperature -> vm.tempPoints
        Metric.Humidity -> vm.humidityPoints
        Metric.Flammable -> vm.flammablePoints
        Metric.TVOC -> vm.tvocPoints
        Metric.CO -> vm.coPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = metric.label,
            points = points,
            color = metric.color,
            fixedYRange = metric.yRange,
            xLabels = vm.xLabels,
            allTimeLabels = vm.historicalAllTimeLabels, // Pass complete time labels for popup
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                "📊",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "No data available",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "for ${metric.label}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PredictiveChartForMetric(metric: Metric, vm: LatestChartViewModel) {
    val points = when (metric) {
        Metric.Temperature -> vm.predictiveTempPoints
        Metric.Humidity -> vm.predictiveHumidityPoints
        Metric.Flammable -> vm.predictiveFlammablePoints
        Metric.TVOC -> vm.predictiveTvocPoints
        Metric.CO -> vm.predictiveCoPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = "${metric.label} (Forecast)",
            points = points,
            color = metric.color.copy(alpha = 0.8f),
            fixedYRange = metric.yRange,
            xLabels = vm.predictiveXLabels,
            allTimeLabels = vm.predictiveAllTimeLabels, // Pass complete time labels for popup
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                "🔮",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "No predictive data available",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "for ${metric.label}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PredictionChartForMetric(metric: Metric, vmp: PredictionChartViewModel) {
    val points = when (metric) {
        Metric.Temperature -> vmp.tempPoints
        Metric.Humidity -> vmp.humidityPoints
        Metric.Flammable -> vmp.flammablePoints
        Metric.TVOC -> vmp.tvocPoints
        Metric.CO -> vmp.coPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = metric.label,
            points = points,
            color = metric.color,
            fixedYRange = metric.yRange,
            xLabels = vmp.xLabels,
            allTimeLabels = vmp.allTimeLabels, // Pass complete time labels for popup
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Text(
                "🔮",
                fontSize = 40.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "Ready to predict",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Enter days and click Generate",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PreviousPredictionChartForMetric(metric: Metric, vmp: PredictionChartViewModel) {
    val points = when (metric) {
        Metric.Temperature -> vmp.prevTempPoints
        Metric.Humidity -> vmp.prevHumidityPoints
        Metric.Flammable -> vmp.prevFlammablePoints
        Metric.TVOC -> vmp.prevTvocPoints
        Metric.CO -> vmp.prevCoPoints
    }

    if (points.isNotEmpty()) {
        MetricLineChart(
            title = "${metric.label} (Previous)",
            points = points,
            color = metric.color.copy(alpha = 0.6f),
            fixedYRange = metric.yRange,
            xLabels = vmp.prevXLabels,
            allTimeLabels = vmp.prevAllTimeLabels, // Pass complete time labels for popup
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}

@Composable
fun StatisticsCards(
    points: List<co.yml.charts.common.model.Point>,
    metricLabel: String,
    showRegressionAnalysis: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val yValues = points.map { it.y }
    val highest = yValues.maxOrNull() ?: 0f
    val lowest = yValues.minOrNull() ?: 0f
    val average = yValues.average().toFloat()

    // Determine if values are concerning based on metric type
    val isHighestConcerning = when {
        metricLabel.contains("Temperature", ignoreCase = true) -> highest > 80f
        metricLabel.contains("Humidity", ignoreCase = true) -> highest > 60f
        metricLabel.contains("TVOC", ignoreCase = true) -> highest > 500f
        metricLabel.contains("CO", ignoreCase = true) -> highest > 9f
        metricLabel.contains("Flammable", ignoreCase = true) -> highest > 1000f
        else -> false
    }

    val isLowestConcerning = when {
        metricLabel.contains("Temperature", ignoreCase = true) -> lowest < 60f
        metricLabel.contains("Humidity", ignoreCase = true) -> lowest < 20f
        else -> false
    }

    // Calculate linear regression only if needed
    val regressionResult = if (showRegressionAnalysis) {
        calculateLinearRegression(points)
    } else null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Statistics for $metricLabel",
            color = Maroon,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Highest",
                value = String.format("%.2f", highest),
                icon = if (isHighestConcerning) "⚠️" else "📈",
                color = if (isHighestConcerning) Color(0xFFFF6B6B) else Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
                isConcerning = isHighestConcerning
            )

            StatCard(
                title = "Lowest",
                value = String.format("%.2f", lowest),
                icon = if (isLowestConcerning) "⚠️" else "📉",
                color = if (isLowestConcerning) Color(0xFFFF6B6B) else Color(0xFF4CAF50),
                modifier = Modifier.weight(1f),
                isConcerning = isLowestConcerning
            )

            StatCard(
                title = "Average",
                value = String.format("%.2f", average),
                icon = "📊",
                color = Maroon,
                modifier = Modifier.weight(1f),
                isConcerning = false
            )
        }

        // Regression Analysis Card - only show if enabled
        if (showRegressionAnalysis && regressionResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Maroon.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📉 Trend Analysis",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Trend indicator
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                regressionResult.slope > 0.1 -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                regressionResult.slope < -0.1 -> Color(0xFFFF6B6B).copy(alpha = 0.2f)
                                else -> Maroon.copy(alpha = 0.2f)
                            }
                        ) {
                            Text(
                                text = when {
                                    regressionResult.slope > 0.1 -> "↗️ Rising"
                                    regressionResult.slope < -0.1 -> "↘️ Falling"
                                    else -> "→ Stable"
                                },
                                color = when {
                                    regressionResult.slope > 0.1 -> Color(0xFF4CAF50)
                                    regressionResult.slope < -0.1 -> Color(0xFFFF6B6B)
                                    else -> Maroon
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Regression details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Slope",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format("%.4f", regressionResult.slope),
                                color = Maroon,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Intercept",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format("%.2f", regressionResult.intercept),
                                color = Maroon,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "R² (Fit)",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = String.format("%.3f", regressionResult.rSquared),
                                color = when {
                                    regressionResult.rSquared > 0.7 -> Color(0xFF4CAF50)
                                    regressionResult.rSquared > 0.4 -> Maroon
                                    else -> Color(0xFFFF6B6B)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Interpretation text
                    Text(
                        text = when {
                            regressionResult.slope > 0.1 -> "Data shows an upward trend over time"
                            regressionResult.slope < -0.1 -> "Data shows a downward trend over time"
                            else -> "Data remains relatively stable over time"
                        },
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

data class RegressionResult(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double
)

fun calculateLinearRegression(points: List<co.yml.charts.common.model.Point>): RegressionResult {
    if (points.isEmpty()) return RegressionResult(0.0, 0.0, 0.0)

    val n = points.size
    val x = points.map { it.x.toDouble() }
    val y = points.map { it.y.toDouble() }

    // Calculate means
    val xMean = x.average()
    val yMean = y.average()

    // Calculate slope (m) and intercept (b) for y = mx + b
    var numerator = 0.0
    var denominator = 0.0

    for (i in points.indices) {
        numerator += (x[i] - xMean) * (y[i] - yMean)
        denominator += (x[i] - xMean) * (x[i] - xMean)
    }

    val slope = if (denominator != 0.0) numerator / denominator else 0.0
    val intercept = yMean - slope * xMean

    // Calculate R² (coefficient of determination)
    var ssRes = 0.0  // Residual sum of squares
    var ssTot = 0.0  // Total sum of squares

    for (i in points.indices) {
        val yPred = slope * x[i] + intercept
        ssRes += (y[i] - yPred) * (y[i] - yPred)
        ssTot += (y[i] - yMean) * (y[i] - yMean)
    }

    val rSquared = if (ssTot != 0.0) 1.0 - (ssRes / ssTot) else 0.0

    return RegressionResult(slope, intercept, rSquared.coerceIn(0.0, 1.0))
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    isConcerning: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isConcerning) Color(0xFF1E1E1E) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConcerning) 4.dp else 2.dp),
        border = if (isConcerning) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF6B6B)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isConcerning)
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFF6B6B).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    else Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (isConcerning) {
                Text(
                    text = "Warning",
                    color = Color(0xFFFF6B6B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CombinedLoadingOverlay(
    message: String = "Loading chart data…",
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