package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.airqualitytracker.ui.theme.Black
import com.example.airqualitytracker.ui.theme.Maroon
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
            }
        }
    }
}
