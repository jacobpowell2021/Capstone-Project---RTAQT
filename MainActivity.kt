package com.example.airqualitytracker

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = Firebase.auth
        setContent {
            val navController = rememberNavController()//initialize the navigation controller
            NavHost(navController= navController, startDestination = Routes.SignInScreen, builder = {
                composable(Routes.HomeScreen){
                    HomeScreen(navController)
                }
                composable(Routes.DataScreen){
                    DataScreen(navController)
                }
                composable(Routes.SignInScreen){
                    SignInScreen(navController)
                }
                composable(Routes.NavigationScreen){
                    NavigationScreen(navController)
                }
                composable(Routes.SpecScreen){
                    SpecificationScreen(navController)
                }
                composable(Routes.SignUpScreen) {
                    SignUpScreen(
                        navController = navController,
                        auth = auth,
                        context = this@MainActivity
                    )
                }
                composable(Routes.PredictionScreen) {
                    PredictionScreen(navController)
                }
                composable(Routes.ChartsScreen) {
                    ChartsScreen(navController)
                }

                /*composable(Routes.DataCreationScreen){
                    DataCreationScreen(navController)
                }*/

            } )

        }
        TESTING.runTest()
        // Observe MQTT messages
        MqttManager.mqttMessage.observe(this) { pair ->
            pair?.let { (topic, message) ->
                Log.d("MQTT", "Received Topic: $topic: $message")
            }
        }
    }
}

