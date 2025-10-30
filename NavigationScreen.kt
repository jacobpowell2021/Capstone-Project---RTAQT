package com.example.airqualitytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationScreen(navController:NavController) {//is going to be the screen for the navigation of the app
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ){
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Button(onClick = {
                navController.navigate(Routes.HomeScreen)
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White       // Text color
                )) {
                Text(text = "Home")
            }
            Button(onClick = {
                navController.navigate(Routes.DataScreen)
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White       // Text color
                )) {
                Text(
                    text = "Data/Trends"
                )
            }
            Button(onClick = {
                navController.navigate(Routes.SpecScreen)
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White       // Text color
                )) {
                Text(text = "Systems Specs")
            }
            /*Button(onClick = {
                navController.navigate(Routes.DataCreationScreen)
            }) {
                Text(text = "Data Creation")
            }*/
            Button(onClick = {
                FirebaseAuth.getInstance().signOut()//for SigningOut the user
                MqttManager.disconnect()
                navController.navigate(Routes.SignInScreen){
                    popUpTo(0)//for no backstack
                }
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White       // Text color
                )) {
                Text(text = "Sign Out")
            }


        }
    }
}