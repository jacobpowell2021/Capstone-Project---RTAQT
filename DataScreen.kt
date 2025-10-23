package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
import com.example.airqualitytracker.LatestChartViewModel
import kotlinx.coroutines.delay

@Composable
fun DataScreen(
    navController: NavController,
    vm: LatestChartViewModel = viewModel()) {

    LaunchedEffect(Unit) {
        delay(1000L)
        vm.fetch()
    }
    if (vm.isLoading) {
        LoadingOverlay()
    } else {

        // ---------------- Bottom bar setup ----------------
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

        // ---------------- Scaffold with bottom bar ----------------
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
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ) // gives spacing for a "floating" look
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

            // ---------- Your original plot UI, padded above the bar ----------
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .background(Color.Black)
                    .padding(innerPadding)
            ) {
                Text("Last 24 Hours")
                LatestChartsSection()
            }
        }
    }
}
@Composable
fun LoadingOverlay(
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

//working code
/*package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*

@Composable
fun DataScreen(navController: NavController) {
    // ---------------- Preset data (edit these) ----------------
    val temperatureY = remember { listOf(24f, 25f, 26.5f, 25.8f, 27.2f, 26.9f) }
    val humidityY    = remember { listOf(45f, 47f, 46f, 48.5f, 49f, 50f) }
    val particleY    = remember { listOf(10f, 12f, 15f, 13f, 11f, 14f) }

    fun toPoints(values: List<Float>) =
        values.mapIndexed { i, y -> Point(i.toFloat(), y) }

    val temperaturePoints = remember { toPoints(temperatureY) }
    val humidityPoints    = remember { toPoints(humidityY) }
    val particlePoints    = remember { toPoints(particleY) }

    // Shared X labels — one per index (t0, t1, …). Replace with timestamps if needed.
    val count   = maxOf(temperatureY.size, humidityY.size, particleY.size)
    val xLabels = List(count) { i -> "t$i" }

    // ---------------- Layout & axes ----------------
    val steps = 5                 // Y ticks 0..100 by 20 (adjust if your range differs)
    val chartHeight = 300.dp
    val yAxisWidth = 48.dp
    val xAxisGutter = 28.dp

    val stepDp = 80.dp
    val xSteps = (xLabels.size - 1).coerceAtLeast(1)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth: Dp = maxOf(screenWidth, (xSteps * stepDp.value).dp)

    // X axis (visible labels)
    val xAxisData = AxisData.Builder()
        .axisStepSize(stepDp)
        .backgroundColor(Color.Black)
        .steps(xSteps)
        .labelData { i -> xLabels.getOrNull(i) ?: "" }
        .labelAndAxisLinePadding(8.dp)
        .axisLabelColor(Color.White)
        .build()

    // Hide built-in Y labels inside the chart; we’ll draw our own on the left
    val yAxisHidden = AxisData.Builder()
        .steps(steps)
        .backgroundColor(Color.Black)
        .labelAndAxisLinePadding(0.dp)
        .labelData { _ -> "" }
        .axisLabelColor(Color.Transparent)
        .build()

    // Anchor line to lock Y to [0,100] (make transparent)
    val anchorLine = Line(
        dataPoints = listOf(Point(0f, 0f), Point(0f, 100f)),
        lineStyle = LineStyle(color = Color.Transparent),
        intersectionPoint = IntersectionPoint(color = Color.Transparent),
        selectionHighlightPoint = SelectionHighlightPoint(color = Color.Transparent),
        shadowUnderLine = ShadowUnderLine(color = Color.Transparent),
        selectionHighlightPopUp = SelectionHighlightPopUp(backgroundColor = Color.Transparent)
    )

    // Build visible lines
    val lines = buildList {
        add(anchorLine)
        if (temperaturePoints.isNotEmpty()) add(
            Line(
                dataPoints = temperaturePoints,
                lineStyle = LineStyle(color = Color(0xFFFF5252)), // red
                intersectionPoint = IntersectionPoint(color = Color(0xFFFF5252)),
                selectionHighlightPoint = SelectionHighlightPoint(color = Color(0xFFFF5252)),
                shadowUnderLine = ShadowUnderLine(),
                selectionHighlightPopUp = SelectionHighlightPopUp()
            )
        )
        if (particlePoints.isNotEmpty()) add(
            Line(
                dataPoints = particlePoints,
                lineStyle = LineStyle(color = Color(0xFF00E5FF)), // cyan
                intersectionPoint = IntersectionPoint(color = Color(0xFF00E5FF)),
                selectionHighlightPoint = SelectionHighlightPoint(color = Color(0xFF00E5FF)),
                shadowUnderLine = ShadowUnderLine(),
                selectionHighlightPopUp = SelectionHighlightPopUp()
            )
        )
        if (humidityPoints.isNotEmpty()) add(
            Line(
                dataPoints = humidityPoints,
                lineStyle = LineStyle(color = Color(0xFF00FF00)), // green
                intersectionPoint = IntersectionPoint(color = Color(0xFF00FF00)),
                selectionHighlightPoint = SelectionHighlightPoint(color = Color(0xFF00FF00)),
                shadowUnderLine = ShadowUnderLine(),
                selectionHighlightPopUp = SelectionHighlightPopUp()
            )
        )
    }

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(lines = lines),
        xAxisData = xAxisData,
        yAxisData = yAxisHidden,
        gridLines = GridLines(),
        backgroundColor = Color.Black
    )

    // Horizontal scroll (auto-scroll to end if labels grow)
    val hScroll = rememberScrollState()
    LaunchedEffect(xLabels.size) { hScroll.animateScrollTo(hScroll.maxValue) }

    // ---------------- UI ----------------
    Box(modifier = Modifier
        .fillMaxSize()              // fill the entire screen
        .background(Color.Black)){    // black background everywhere
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(12.dp)
        ) {
            // LEFT: fixed Y labels aligned to plot area
            Box(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(chartHeight)
                    .padding(bottom = xAxisGutter)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    // Top → bottom: 100, 80, 60, 40, 20, 0
                    for (i in steps downTo 0) {
                        val value = i * (100 / steps)
                        Text(value.toString(), color = Color.White)
                    }
                }
            }

            // RIGHT: scrollable plot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                Column {
                    LineChart(
                        modifier = Modifier
                            .width(contentWidth)
                            .height(chartHeight),
                        lineChartData = lineChartData
                    )
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}
*/