package com.example.airqualitytracker

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Email
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.airqualitytracker.ui.theme.Maroon
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController,auth: FirebaseAuth, context: Context){
    var email by remember { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Please Enter New User Email and Password",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(vertical = 30.dp)
            )
            TextField(//adds a rectangular box for username
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
                    cursorColor = Maroon,)                      // Cursor color
            )
            TextField(//adds rectangular box for password
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
                    cursorColor = Maroon,),                     // Cursor color
                visualTransformation = PasswordVisualTransformation()//this makes the password non visible
            )
            Button(onClick = {
                if (email.isNotBlank() && password.length >= 6) {
                    createAccountWithFirebase(auth, context, email, password)
                    navController.navigate(Routes.SignInScreen)
                } else {
                    Toast.makeText(context, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Maroon,         // Background
                    contentColor = Color.White)       // Text color
            ) {
                Text(text = "Create Account")
            }
            Button(onClick = {
                navController.navigate(Routes.SignInScreen) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Maroon,         // Background
                        contentColor = Color.White)       // Text color
                ) {
                    Text(text = "Already Have an account?")
                }
        }
    }
}

fun createAccountWithFirebase(auth: FirebaseAuth, context: Context, email: String, password: String) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            Toast.makeText(context, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to send verification email: ${verifyTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
}

