package com.example.airqualitytracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var autoRefresh by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            vm.fetch()
            delay(500L)
            predictionVm.fetchAndBuild(1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data", e)
        }
    }
    /*
    // Continuous update effect
    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            try {
                vm.fetch()
                delay(5000L) // Update every 5 seconds
            } catch (e: Exception) {
                Log.e(TAG, "Auto-refresh error", e)
            }
        }
    }
    */
    var expanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(Metric.Temperature) }
    var daysText by remember { mutableStateOf("1.0") }

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
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
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
                                            tint = Color.White
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Maroon,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
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
                            .padding(bottom = 32.dp),
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
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = daysText,
                                onValueChange = { daysText = it },
                                label = {
                                    Text(
                                        "Days to predict",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Maroon,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Maroon,
                                    focusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    focusedContainerColor = Color(0xFF2A2A2A),
                                    unfocusedContainerColor = Color(0xFF2A2A2A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    daysText.toFloatOrNull()?.let(predictionVm::fetchAndBuild)
                                },
                                enabled = !predictionVm.isLoading.value,
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
                    }

                    // Prediction Chart
                    // Replace the Prediction Chart section with this:

// Prediction Chart - Current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (predictionVm.hasPreviousData.value) 16.dp else 24.dp),
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
            color = Color.White,
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
            color = metric.color.copy(alpha = 0.6f), // Slightly faded to distinguish from current
            fixedYRange = metric.yRange,
            xLabels = vmp.prevXLabels,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
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

/*package com.example.airqualitytracker

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    LaunchedEffect(Unit) {
        try {
            vm.fetch()
            delay(500L)
            predictionVm.fetchAndBuild(1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading data", e)
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf(Metric.Temperature) }
    var daysText by remember { mutableStateOf("1.0") }

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
            BottomItem(
                "Home",
                Routes.HomeScreen,
                { Icon(Icons.Filled.Home, contentDescription = "Home") }
            ),
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
                { Icon(Icons.Filled.Info, contentDescription = "Alerts") }
            ),
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
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
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
                                            tint = Color.White
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Maroon,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
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
                            .padding(bottom = 32.dp),
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
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = daysText,
                                onValueChange = { daysText = it },
                                label = {
                                    Text(
                                        "Days to predict",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Maroon,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    cursorColor = Maroon,
                                    focusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                    focusedContainerColor = Color(0xFF2A2A2A),
                                    unfocusedContainerColor = Color(0xFF2A2A2A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    daysText.toFloatOrNull()?.let(predictionVm::fetchAndBuild)
                                },
                                enabled = !predictionVm.isLoading.value,
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
                    }

                    // Prediction Chart
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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
            color = Color.White,
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
/*package com.example.airqualitytracker

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
    var selectedMetric by remember { mutableStateOf(Metric.Temperature) }
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
                            Metric.entries.forEach { option ->
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
fun LatestChartForMetric(metric: Metric, vm: LatestChartViewModel = viewModel()) {
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
}*/