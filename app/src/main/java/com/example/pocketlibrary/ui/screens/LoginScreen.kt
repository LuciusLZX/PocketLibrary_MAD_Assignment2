package com.example.pocketlibrary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pocketlibrary.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A screen that allows users to log in or sign up for a Firebase account.
 *
 * @param authRepository The repository to handle authentication logic.
 * @param onLoginSuccess A callback triggered after a successful login or sign-up.
 */
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("PocketLibrary", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Show error message if one exists
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        // Show a loading indicator when an operation is in progress
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Login Button
                Button(
                    onClick = {
                        isLoading = true
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = authRepository.login(email, password)
                            result.onSuccess {
                                isLoading = false
                                if (it.isNotEmpty()) onLoginSuccess()
                            }
                            result.onFailure {
                                isLoading = false
                                error = it.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Login")
                }

                // Sign Up Button
                Button(
                    onClick = {
                        isLoading = true
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = authRepository.signUp(email, password)
                            result.onSuccess {
                                isLoading = false
                                if (it.isNotEmpty()) onLoginSuccess()
                            }
                            result.onFailure {
                                isLoading = false
                                error = it.message
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign Up")
                }
            }
        }
    }
}

// FOR TESTING ONLY, PLEASE DELETE AFTERWARDS!!!!!!
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            authRepository = AuthRepository(),
            onLoginSuccess = {}      // Pass an empty lambda
        )
    }
}
