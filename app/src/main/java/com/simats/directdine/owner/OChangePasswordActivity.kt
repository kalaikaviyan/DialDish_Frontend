package com.simats.directdine.owner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OChangePasswordActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    ChangePasswordScreen(modifier = Modifier.padding(innerPadding)) { finish() }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(modifier: Modifier = Modifier, onFinish: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Step 1: Verification State
    var currentPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var verifyError by remember { mutableStateOf<String?>(null) }

    // Step 2: New Password State
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation Logic
    val isLengthValid = newPassword.length >= 8
    val hasUppercase = newPassword.any { it.isUpperCase() }
    val hasNumber = newPassword.any { it.isDigit() }
    val passwordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()
    val isNewPasswordValid = isLengthValid && hasUppercase && hasNumber && passwordsMatch

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- STEP 1: VERIFY CURRENT PASSWORD ---
        Text(
            text = "For your security, please verify your current password before making changes.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; verifyError = null },
            label = { Text("Current Password") },
            enabled = !isVerified, // Lock field after verification
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color.Gray) },
            trailingIcon = {
                val image = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Visibility")
                }
            },
            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = verifyError != null,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
        if (verifyError != null) {
            Text(text = verifyError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verify Button (Disappears after successful verification)
        if (!isVerified) {
            Button(
                onClick = {
                    if (currentPassword.isEmpty()) {
                        verifyError = "Please enter your current password"
                        return@Button
                    }
                    isVerifying = true
                    // MOCK API CALL: Pretend we check the database
                    coroutineScope.launch {
                        delay(1000) // Simulate network delay
                        // TODO: Connect to PHP API to check password matching hash
                        if (currentPassword == "admin123") { // Mock success condition
                            isVerified = true
                            verifyError = null
                        } else {
                            verifyError = "Incorrect current password."
                        }
                        isVerifying = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isVerifying
            ) {
                if (isVerifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Verify Password", fontWeight = FontWeight.Bold)
            }
        }

        // --- STEP 2: ENTER NEW PASSWORD (Revealed after verification) ---
        AnimatedVisibility(
            visible = isVerified,
            enter = fadeIn() + expandVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Enter New Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) { Icon(imageVector = image, contentDescription = "Toggle Visibility") }
                    },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Rules Checklist
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                    RuleText("At least 8 characters", isLengthValid)
                    RuleText("At least 1 uppercase letter", hasUppercase)
                    RuleText("At least 1 number", hasNumber)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(imageVector = image, contentDescription = "Toggle Visibility") }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                    Text(text = "Passwords do not match", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        // TODO: Send to PHP API to UPDATE users SET password_hash = ...
                        Toast.makeText(context, "Password Updated Successfully!", Toast.LENGTH_LONG).show()
                        onFinish() // Go back to profile
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = isNewPasswordValid,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Update New Password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RuleText(text: String, isValid: Boolean) {
    val color = if (isValid) Color(0xFF4CAF50) else Color.Gray
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(
            imageVector = if (isValid) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    directdineTheme { ChangePasswordScreen() }
}