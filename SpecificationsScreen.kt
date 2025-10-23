package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
            color = Maroon,   // your custom color
            darkIcons = true // false = white icons (good for dark bars)
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
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ----- Device Specs -----
                Text("System Specs", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Power Supply: ", color = Color.White)
                Text("Temperature Sensor: ", color = Color.White)
                Text("Humidity Sensor: ", color = Color.White)
                Text("Particle Sensor: ", color = Color.White)

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

            }
        }
    }
}


/*package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon

//This page will display all the specification of our device
@Composable
fun SpecificationScreen (navController: NavController,
                         vm: PredictionViewModel = viewModel()){
    // ----- Bottom Navigation Items -----
    data class BottomItem(
        val label: String,
        val route: String,
        val icon: @Composable () -> Unit
    )
    // Replace routes below with your actual routes where needed.
    val items = listOf(
        BottomItem("Home", Routes.HomeScreen, { Icon(Icons.Filled.Home, contentDescription = "Home") }),
        BottomItem("Charts", Routes.DataScreen, { Icon(Icons.Filled.List, contentDescription = "Charts") }),
        BottomItem("Specifications", Routes.SpecScreen , { Icon(Icons.Filled.Info, contentDescription = "Alerts") }),

        )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Maroon,
        bottomBar = {
            NavigationBar(
                containerColor = Maroon,
                contentColor = Color.White,
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
    ) {innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)

        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Power Supply: ",
                    color = Color.White
                )
                Text(
                    text = "Temperature Sensor: ",
                    color = Color.White
                )
                Text(
                    text = "Humidity Sensor: ",
                    color = Color.White
                )
                Text(
                    text = "Particle Sensor: ",
                    color = Color.White
                )
                Text(text = "System Specs")

            }
        }
    }
}*/