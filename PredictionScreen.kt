package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
import com.example.airqualitytracker.PredictionChartViewModel
import kotlinx.coroutines.delay

@Composable
fun PredictionScreen(
        navController: NavController,
        vm: PredictionChartViewModel = viewModel()
){
    LaunchedEffect(Unit) {
        delay(1000L)
        vm.fetchAndBuild(1f)
    }
    data class BottomItem(
        val label: String,
        val route: String,
        val icon: @Composable () -> Unit
    )
    //Navigation bar
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

    var daysText by remember { mutableStateOf("0.5") }

    Scaffold(
        containerColor = Maroon,
        bottomBar ={
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
                Text("Fetch Prediction (Azure Function)", color = Color.White)

                PredictionChartsSection()
                /*
                Button(
                    onClick = {
                        vm.fetchPrediction(daysText.toFloat())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Maroon, contentColor = Color.White)
                ) {
                    Text("Fetch Prediction")
                }


                Text("Temperature (first 5): ${vm.temperature.value.take(5).joinToString()}", color = Color.White)
                Text("Flammable (first 5): ${vm.flammable.value.take(5).joinToString()}", color = Color.White)
                Text("Humidity (first 5): ${vm.humidity.value.take(5).joinToString()}", color = Color.White)
                Text("TVOC (first 5): ${vm.tvoc.value.take(5).joinToString()}", color = Color.White)
                Text("CO (first 5): ${vm.co.value.take(5).joinToString()}", color = Color.White)*/

            }
        }
    }
}
