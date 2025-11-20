package com.example.airqualitytracker

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var showReset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        email = ""
        password = ""
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo/Title Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    Text(
                        text = "RTAQT",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Real-Time Air Quality Tracker",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    )
                }

                // Sign In Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Welcome Back",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Sign in to continue monitoring",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Email,
                                    contentDescription = "Email",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Maroon,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                cursorColor = Maroon,
                                focusedContainerColor = Color(0xFF2A2A2A),
                                unfocusedContainerColor = Color(0xFF2A2A2A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Password",
                                    tint = Color.White.copy(alpha = 0.6f)
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Maroon,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                                cursorColor = Maroon,
                                focusedContainerColor = Color(0xFF2A2A2A),
                                unfocusedContainerColor = Color(0xFF2A2A2A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Sign In Button
                        Button(
                            onClick = {
                                Handler(Looper.getMainLooper()).post {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                val user = auth.currentUser
                                                if (user != null && user.isEmailVerified) {
                                                    val currentTime = LocalDateTime.now()
                                                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                                    Log.d("FirebaseAuth", "Signed in at $currentTime")

                                                    MqttManager.connect(
                                                        username = "Computer",
                                                        password = "I1mdb\$pm",
                                                        topic = "Temperature"
                                                    ) { success ->
                                                        if (success) {
                                                            Log.d("Navigation", "Navigating to HomeScreen")
                                                            Handler(Looper.getMainLooper()).post {
                                                                navController.navigate(Routes.HomeScreen)
                                                            }
                                                        } else {
                                                            Log.d("MQTT", "MQTT connection failed")
                                                            Toast.makeText(
                                                                context,
                                                                "MQTT connection failed",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Please verify your email first.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                showReset = true
                                                Toast.makeText(
                                                    context,
                                                    "Sign in failed, incorrect email or password",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Maroon,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Sign Up Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate(Routes.SignUpScreen)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Forgot Password
                        if (showReset) {
                            TextButton(
                                onClick = {
                                    if (email.isNotBlank()) {
                                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    Toast.makeText(
                                                        context,
                                                        "Reset link sent to $email",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to send reset email",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Please enter your email first",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            ) {
                                Text(
                                    "Forgot Password?",
                                    color = Maroon,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Developer Skip Button
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        MqttManager.connect(
                            username = "Computer",
                            password = "I1mdb\$pm",
                            topic = "Temperature"
                        ) { success ->
                            if (success) {
                                Log.d("Navigation", "Navigating to HomeScreen")
                            } else {
                                Log.d("MQTT", "MQTT connection failed")
                                Toast.makeText(
                                    context,
                                    "MQTT connection failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        navController.navigate(Routes.HomeScreen)
                    }
                ) {
                    Text(
                        "Developer Skip →",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }

            // Footer
            Text(
                text = "Created by RTAQT Team",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}
/*package com.example.airqualitytracker

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SignInScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var showReset by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        email = ""
        password = ""
    }

    Scaffold(
        containerColor = Maroon,
        bottomBar = {
            BottomAppBar(
                containerColor = Maroon,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Created by RTAQT Team",
                        color = Color.White
                    )
                }
            }
        }

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {

            // Centered Column for inputs and buttons
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                //Developer skip button
                Button(
                    onClick = {
                        MqttManager.connect(//automatically should connect to MQTT broker
                            username = "Computer",
                            password = "I1mdb\$pm",
                            topic = "Temperature"
                        ) { success ->
                            if (success) {
                                Log.d("Navigation", "Navigating to HomeScreen")
                            } else {
                                Log.d("MQTT", "MQTT connection failed")
                                Toast.makeText(
                                    context,
                                    "MQTT connection failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        navController.navigate(Routes.HomeScreen)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Developer Skip")
                }
                Text(
                    text = "Welcome to RTAQT, please Sign in!",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(vertical = 30.dp)
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Enter Email") },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        containerColor = Maroon,                  // Background
                        focusedIndicatorColor = Color.White,           // Underline when focused
                        unfocusedIndicatorColor = Color.White,         // Underline when not focused
                        focusedLabelColor = Color.White,               // Label color when active
                        unfocusedLabelColor = Color.White,          //label color when inactive
                        cursorColor = Color.White,                      // Cursor color

                    )
                )
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        containerColor = Maroon,                  // Background
                        focusedIndicatorColor = Color.White,           // Underline when focused
                        unfocusedIndicatorColor = Color.White,         // Underline when not focused
                        focusedLabelColor = Color.White,               // Label color when active
                        unfocusedLabelColor = Color.White,          //label color when inactive
                        cursorColor = Color.White,
                    ),                  // Cursor color
                    visualTransformation = PasswordVisualTransformation()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(100.dp)
                ) {
                    Button(
                        onClick = { /*navController.navigate(Routes.HomeScreen)*/
                            /*var currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            Log.d("MQTT","Trying to connect at $currentTime")*/ //this piece of code was redundant and could just be put into the MQTT function
                            //Log.d("SignIn","Sign-in button clicked")
                            Handler(Looper.getMainLooper()).post {
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null && user.isEmailVerified) {
                                                val currentTime = LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                                                Log.d("FirebaseAuth", "Signed in at $currentTime")

                                                MqttManager.connect(//automatically should connect to MQTT broker
                                                    username = "Computer",
                                                    password = "I1mdb\$pm",
                                                    topic = "Temperature"
                                                ) { success ->
                                                    if (success) {
                                                        Log.d(
                                                            "Navigation",
                                                            "Navigating to HomeScreen"
                                                        )
                                                        Handler(Looper.getMainLooper()).post {
                                                            navController.navigate(Routes.HomeScreen)
                                                        }
                                                    } else {
                                                        Log.d("MQTT", "MQTT connection failed")
                                                        Toast.makeText(
                                                            context,
                                                            "MQTT connection failed",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Please verify your email first.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        } else {
                                            showReset = true
                                            Toast.makeText(
                                                context,
                                                "Sign in failed, incorrect email or password",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon,         // Background
                            contentColor = Color.White       // Text color
                        )
                    ) {
                        Text("Sign In")
                    }

                    Button(
                        onClick = {
                            navController.navigate(Routes.SignUpScreen)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Maroon,         // Background
                            contentColor = Color.White       // Text color
                        )
                    ) {
                        Text(text = "Sign Up")
                    }

                }
                if (showReset) {
                    TextButton(//adds a password reset to the email
                        onClick = {
                            if (email.isNotBlank()) {//checks for email not being blanck
                                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "Reset link sent to $email",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Failed to send reset email",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please enter your email first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text("Forgot Password?", color = Color.White)
                    }
                }

            }
        }
    }
}
*/
