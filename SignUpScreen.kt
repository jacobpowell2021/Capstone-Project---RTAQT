package com.example.airqualitytracker

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController, auth: FirebaseAuth, context: Context) {
    var email by remember { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Function to validate email
    fun validateEmail(email: String): Boolean {
        return if (email.isBlank()) {
            emailError = "Email cannot be empty"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            false
        } else {
            emailError = null
            true
        }
    }

    // Function to validate password
    fun validatePassword(password: String): Boolean {
        return if (password.isBlank()) {
            passwordError = "Password cannot be empty"
            false
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            false
        } else {
            passwordError = null
            true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Create Account",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Join RTAQT to start monitoring",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Sign Up Card
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
                        text = "Enter your details",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(8.dp))

                    // Email Field with Error Handling
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            // Clear error when user starts typing
                            if (emailError != null) {
                                emailError = null
                            }
                        },
                        label = { Text("Email Address") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = "Email",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        isError = emailError != null,
                        supportingText = {
                            emailError?.let {
                                Text(
                                    text = it,
                                    color = Color.Red
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (emailError != null) Color.Red else Maroon,
                            unfocusedBorderColor = if (emailError != null) Color.Red else Color.White.copy(alpha = 0.3f),
                            errorBorderColor = Color.Red,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            errorLabelColor = Color.Red,
                            cursorColor = Maroon,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            errorContainerColor = Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Password Field with Error Handling
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            // Clear error when user starts typing
                            if (passwordError != null) {
                                passwordError = null
                            }
                        },
                        label = { Text("Password (min 6 characters)") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Password",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        isError = passwordError != null,
                        supportingText = {
                            passwordError?.let {
                                Text(
                                    text = it,
                                    color = Color.Red
                                )
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (passwordError != null) Color.Red else Maroon,
                            unfocusedBorderColor = if (passwordError != null) Color.Red else Color.White.copy(alpha = 0.3f),
                            errorBorderColor = Color.Red,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            errorLabelColor = Color.Red,
                            cursorColor = Maroon,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            errorContainerColor = Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // Create Account Button
                    Button(
                        onClick = {
                            // Validate inputs before attempting account creation
                            val isEmailValid = validateEmail(email)
                            val isPasswordValid = validatePassword(password)

                            if (isEmailValid && isPasswordValid) {
                                createAccountWithFirebase(
                                    auth = auth,
                                    context = context,
                                    email = email,
                                    password = password,
                                    navController = navController,
                                    onEmailError = { error -> emailError = error },
                                    onPasswordError = { error -> passwordError = error }
                                )
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
                            "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Back to Sign In Button
                    OutlinedButton(
                        onClick = {
                            navController.navigate(Routes.SignInScreen)
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
                            "Already Have an Account?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
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

fun createAccountWithFirebase(
    auth: FirebaseAuth,
    context: Context,
    email: String,
    password: String,
    navController: NavController,
    onEmailError: (String) -> Unit = {},
    onPasswordError: (String) -> Unit = {}
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Verification email sent. Please check your inbox.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Only navigate after successful account creation
                            navController.navigate(Routes.SignInScreen)
                        } else {
                            Toast.makeText(
                                context,
                                "Account created but failed to send verification email: ${verifyTask.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            // Still navigate since account was created
                            navController.navigate(Routes.SignInScreen)
                        }
                    }
            } else {
                // Handle specific Firebase errors
                val errorMessage = task.exception?.message
                when {
                    errorMessage?.contains("email address is already in use", ignoreCase = true) == true -> {
                        onEmailError("This email is already registered")
                        Toast.makeText(
                            context,
                            "Email already registered. Please sign in instead.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    errorMessage?.contains("badly formatted", ignoreCase = true) == true -> {
                        onEmailError("Invalid email format")
                        Toast.makeText(
                            context,
                            "Invalid email format",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    errorMessage?.contains("password", ignoreCase = true) == true -> {
                        onPasswordError("Password does not meet requirements")
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            "Signup failed: ${errorMessage ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
}