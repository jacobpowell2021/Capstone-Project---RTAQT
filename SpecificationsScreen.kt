package com.example.airqualitytracker

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SpecificationScreen(
    navController: NavController,
) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Maroon,
            darkIcons = true
        )
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
                        text = "System Specifications",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RTAQT Technical Documentation",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // About Card
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
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = "ℹ️",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "About RTAQT",
                                color = Maroon,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "The Real-Time Air Quality Tracker (RTAQT) is an advanced environmental monitoring system designed to provide continuous, accurate measurements of indoor air quality parameters. This device helps ensure safe and healthy living environments through real-time monitoring and intelligent alerting.",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "Developed by: Xavier Carbone-Larson, Isaiah Pili, Jacob Powell, and Angel Soto",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                // Home Screen Features Section
                Text(
                    text = "📱 Home Screen Features",
                    color = Maroon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                FeatureCard(
                    icon = "🔄",
                    title = "Real-Time Monitoring",
                    description = "The Home screen displays live sensor readings with automatic refresh capabilities. Data updates occur at user-configurable intervals (default: 30 seconds), ensuring you always have access to the most current environmental conditions."
                )

                FeatureCard(
                    icon = "⚠️",
                    title = "Intelligent Alerting System",
                    description = "Automatic notifications are triggered when sensor readings exceed safe thresholds. The system monitors all parameters continuously and sends immediate alerts for dangerous conditions, even when the app is in the background."
                )

                FeatureCard(
                    icon = "🎨",
                    title = "Visual Warning Indicators",
                    description = "Sensor cards display color-coded warnings (red borders and gradients) when values reach concerning levels. Warning icons (⚠️) appear alongside critical readings, making it easy to identify issues at a glance."
                )

                FeatureCard(
                    icon = "🔋",
                    title = "Device Status Monitoring",
                    description = "Track the battery level of your RTAQT device directly from the app. Receive low battery warnings when power drops below 20% to ensure uninterrupted monitoring."
                )

                Spacer(Modifier.height(24.dp))

                // Charts Screen Features Section
                Text(
                    text = "📊 Charts Screen Features",
                    color = Maroon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                FeatureCard(
                    icon = "📈",
                    title = "Historical Data Visualization",
                    description = "View the last 12 hours of sensor data through interactive line charts. Select different metrics using the dropdown menu to analyze trends and patterns in temperature, humidity, gas concentrations, and more."
                )

                FeatureCard(
                    icon = "🔮",
                    title = "API-Based Predictive Forecasting",
                    description = "Access 24-hour forecasts generated by the RTAQT backend API. These predictions use advanced algorithms to forecast future environmental conditions based on historical patterns and current trends."
                )

                FeatureCard(
                    icon = "🤖",
                    title = "Custom Prediction Generation",
                    description = "Generate on-demand predictions for 1-3 days into the future using linear regression analysis. Compare multiple prediction runs side-by-side to evaluate forecast accuracy and model performance."
                )

                FeatureCard(
                    icon = "📉",
                    title = "Statistical Analysis",
                    description = "Comprehensive statistics including highest, lowest, and average values are calculated for each metric. Regression analysis provides trend information (slope, intercept, R² values) to understand data patterns over time."
                )

                FeatureCard(
                    icon = "🔍",
                    title = "Interactive Chart Tooltips",
                    description = "Tap any data point on a chart to view precise values and timestamps. Charts support smooth scrolling and zooming for detailed examination of specific time periods."
                )

                Spacer(Modifier.height(24.dp))

                // Sensor Specifications Section
                Text(
                    text = "🔬 Sensor Specifications",
                    color = Maroon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                SensorCard(
                    icon = "🌡️",
                    name = "Temperature Sensor",
                    unit = "Degrees Fahrenheit (°F)",
                    description = "Monitors ambient temperature with high precision. Ideal range: 60-78°F.",
                    alertThresholds = listOf(
                        "High Alert: Above 80°F - Risk of discomfort and increased energy costs",
                        "Low Alert: Below 33°F - Risk of freezing and pipe damage"
                    ),
                    technicalDetails = "Digital temperature sensor provides accurate readings with ±0.5°F precision. Updates occur in real-time with minimal latency."
                )

                SensorCard(
                    icon = "💧",
                    name = "Humidity Sensor",
                    unit = "Relative Humidity (%)",
                    description = "Measures moisture content in the air. Optimal range: 30-60% RH.",
                    alertThresholds = listOf(
                        "High Alert: Above 60% - Risk of mold growth and dust mite proliferation",
                        "Low Alert: Below 30% - Risk of dry skin, respiratory irritation, and static electricity"
                    ),
                    technicalDetails = "Capacitive humidity sensor with ±3% RH accuracy. Essential for maintaining comfortable and healthy indoor environments."
                )

                SensorCard(
                    icon = "🔥",
                    name = "Flammable Gas Sensor",
                    unit = "Parts Per Million (ppm)",
                    description = "Detects combustible gases including methane, propane, and natural gas.",
                    alertThresholds = listOf(
                        "Critical Alert: Above 1,000 ppm - Immediate danger, potential fire/explosion risk",
                        "Safe Range: Below 1,000 ppm"
                    ),
                    technicalDetails = "Metal oxide semiconductor sensor responsive to a wide range of flammable gases. Provides early warning of gas leaks before dangerous concentrations are reached."
                )

                SensorCard(
                    icon = "🌫️",
                    name = "Total Volatile Organic Compounds (TVOC)",
                    unit = "Parts Per Billion (ppb)",
                    description = "Measures airborne chemicals from paints, cleaners, building materials, and other sources.",
                    alertThresholds = listOf(
                        "High Alert: Above 500 ppb - Poor air quality, potential health effects",
                        "Moderate: 200-500 ppb - Acceptable but monitor closely",
                        "Good: Below 200 ppb - Healthy air quality"
                    ),
                    technicalDetails = "Multi-pixel gas sensor technology detects a broad spectrum of VOCs. Long-term exposure to elevated TVOC levels may cause headaches, dizziness, and respiratory issues."
                )

                SensorCard(
                    icon = "☠️",
                    name = "Carbon Monoxide (CO) Sensor",
                    unit = "Parts Per Million (ppm)",
                    description = "Detects this colorless, odorless, deadly gas produced by incomplete combustion.",
                    alertThresholds = listOf(
                        "Critical Alert: Above 9 ppm - Dangerous exposure level",
                        "Prolonged exposure effects: 10-35 ppm can cause symptoms in 6-8 hours",
                        "Safe Range: Below 9 ppm"
                    ),
                    technicalDetails = "Electrochemical CO sensor with high sensitivity and selectivity. Carbon monoxide poisoning can be fatal - this sensor provides crucial early warning to prevent tragedy."
                )

                Spacer(Modifier.height(24.dp))

                // System Features Section
                Text(
                    text = "⚙️ System Features",
                    color = Maroon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                FeatureCard(
                    icon = "🔋",
                    title = "Battery-Powered Operation",
                    description = "The RTAQT device operates on battery power, allowing for flexible placement anywhere in your home without requiring proximity to electrical outlets. Battery level is monitored and displayed in the app."
                )

                FeatureCard(
                    icon = "📡",
                    title = "MQTT Connectivity",
                    description = "Utilizes MQTT protocol for reliable, low-latency data transmission. This lightweight messaging protocol ensures your device can communicate efficiently even with limited network bandwidth."
                )

                FeatureCard(
                    icon = "🌙",
                    title = "Background Monitoring",
                    description = "The app continues monitoring air quality even when closed or in the background. WorkManager ensures periodic data fetching and notification delivery without draining your phone's battery."
                )

                FeatureCard(
                    icon = "🎯",
                    title = "Customizable Refresh Intervals",
                    description = "Adjust auto-refresh rates from 1 to 9,999 seconds based on your needs. Lower intervals provide more frequent updates but may consume more battery power on both the device and your phone."
                )

                Spacer(Modifier.height(24.dp))

                // Understanding the Data Section
                Text(
                    text = "📖 Understanding Your Data",
                    color = Maroon,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Chart Interpretation Guide",
                            color = Maroon,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        DataPointText("• Historical Charts: Show actual recorded data from the past 12 hours")
                        DataPointText("• Predictive Charts (API): Display forecasts generated by the backend server for the next 24 hours")
                        DataPointText("• Prediction Charts: Custom forecasts you generate based on historical patterns")
                        DataPointText("• Trend Analysis: Slope values indicate the rate of change (positive = increasing, negative = decreasing)")
                        DataPointText("• R² Values: Measure prediction accuracy (closer to 1.0 = better fit, above 0.7 = strong correlation)")
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Warning Color Codes",
                            color = Maroon,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFFFF6B6B), RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Red Border/Gradient: Critical or concerning values detected",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = "Warning Icon: Threshold exceeded, attention required",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Green: Normal, safe operating range",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = icon,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 16.dp, top = 4.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Maroon,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SensorCard(
    icon: String,
    name: String,
    unit: String,
    description: String,
    alertThresholds: List<String>,
    technicalDetails: String
) {
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
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = name,
                        color = Maroon,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = unit,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = description,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Alert Thresholds
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Maroon.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Alert Thresholds:",
                        color = Maroon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    alertThresholds.forEach { threshold ->
                        Row(
                            modifier = Modifier.padding(bottom = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = threshold,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = technicalDetails,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DataPointText(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}