package com.simats.dialdish.user

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.network.ChangePasswordRequest
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.network.VerifyPasswordRequest
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class UChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                UChangePasswordScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UChangePasswordScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val userId = userIdStr.toIntOrNull() ?: -1

    var isCurrentPasswordVerified by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }

    val hasLength = newPassword.length in 6..8
    val hasUpper = newPassword.any { it.isUpperCase() }
    val hasNumber = newPassword.any { it.isDigit() }
    val hasSpecial = newPassword.any { !it.isLetterOrDigit() }
    val passwordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()

    val isFormValid = hasLength && hasUpper && hasNumber && hasSpecial && passwordsMatch

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if(isCurrentPasswordVerified) Icons.Filled.LockOpen else Icons.Filled.LockPerson, contentDescription = "Security", tint = Color(0xFFF57C00), modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Create New Password", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Text("Your new password must be unique.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = currentPassword, onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                    }
                },
                modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                enabled = !isCurrentPasswordVerified,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFCC80),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledBorderColor = Color(0xFF4CAF50),
                    disabledTextColor = Color.Gray
                )
            )

            AnimatedVisibility(visible = !isCurrentPasswordVerified) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        enabled = currentPassword.isNotEmpty() && !isVerifying,
                        onClick = {
                            isVerifying = true
                            coroutineScope.launch {
                                try {
                                    val res = RetrofitClient.instance.verifyCurrentPassword(VerifyPasswordRequest(userId, currentPassword))
                                    if (res.isSuccessful && res.body()?.status == "success") {
                                        isCurrentPasswordVerified = true
                                    } else {
                                        Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isVerifying = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isVerifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Text("Verify Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            AnimatedVisibility(
                visible = isCurrentPasswordVerified,
                enter = fadeIn() + expandVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = Color.Gray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFCC80), unfocusedBorderColor = Color(0xFFE0E0E0), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                IndicatorText("6-8 Characters", hasLength)
                                IndicatorText("1 Uppercase", hasUpper)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                IndicatorText("1 Number", hasNumber)
                                IndicatorText("1 Special Char", hasSpecial)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp),
                        isError = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !passwordsMatch,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFCC80), unfocusedBorderColor = Color(0xFFE0E0E0), errorBorderColor = Color.Red, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && !passwordsMatch) {
                        Text("Passwords do not match", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).align(Alignment.Start))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        enabled = isFormValid && !isUpdating,
                        onClick = {
                            isUpdating = true
                            coroutineScope.launch {
                                try {
                                    val res = RetrofitClient.instance.changePassword(ChangePasswordRequest(userId, currentPassword, newPassword))
                                    if (res.isSuccessful && res.body()?.status == "success") {
                                        Toast.makeText(context, "Password Changed Successfully!", Toast.LENGTH_LONG).show()
                                        onBackClick()
                                    } else {
                                        Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isUpdating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF57C00),
                            disabledContainerColor = Color(0xFFFFCC80) // Light orange when disabled
                        ),
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(
                            elevation = if (isFormValid && !isUpdating) 8.dp else 0.dp, // Dynamic shadow
                            shape = RoundedCornerShape(16.dp)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Update Password", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorText(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isValid) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isValid) Color(0xFF4CAF50) else Color.LightGray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = if (isValid) Color(0xFF4CAF50) else Color.Gray, fontWeight = FontWeight.Medium)
    }
}