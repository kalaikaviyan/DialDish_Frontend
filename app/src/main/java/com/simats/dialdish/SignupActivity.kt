package com.simats.dialdish

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.network.* import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SignupScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SignupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var stallName by remember { mutableStateOf("") }
    var fssaiNumber by remember { mutableStateOf("") }
    var stallImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> stallImageUri = uri }

    var selectedRole by remember { mutableStateOf("User") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var stallNameError by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showFailDialog by remember { mutableStateOf(false) }

    fun validateBaseInputs(): Boolean {
        var isValid = true

        if (!fullName.matches("^[a-zA-Z\\s]+$".toRegex())) {
            fullNameError = "Only letters A-Z are allowed"; isValid = false
        } else fullNameError = null

        if (!email.matches("^[a-zA-Z0-9._%+-]+@(gmail\\.com|mail\\.com)$".toRegex())) {
            emailError = "Must use @gmail.com or @mail.com"; isValid = false
        } else emailError = null

        if (!phone.matches("^\\d{10}$".toRegex())) {
            phoneError = "Phone must be exactly 10 digits"; isValid = false
        } else phoneError = null

        val passRegex = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#\$%^&+=!_?-])(?=\\S+\$).{6,8}\$".toRegex()
        if (!password.matches(passRegex)) {
            passwordError = "6-8 chars: 1 Caps, 1 Num, 1 Special (@#$%), No spaces"; isValid = false
        } else passwordError = null

        if (password != confirmPassword || confirmPassword.isEmpty()) {
            confirmPasswordError = "Passwords do not match"; isValid = false
        } else confirmPasswordError = null

        return isValid
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Stall Approved ✅") },
            text = { Text("Your 14-digit FSSAI number is valid. Welcome to DialDish Partner network!") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false

                    coroutineScope.launch {
                        try {
                            val base64Image = stallImageUri?.let { uriToBase64(context, it) }
                            val request = OwnerStallRequest(
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                passwordHash = password,
                                role = "Owner",
                                stallName = stallName,
                                fssaiNumber = fssaiNumber,
                                stallImageBase64 = base64Image
                            )
                            val response = RetrofitClient.instance.registerStall(request)

                            if (response.isSuccessful && response.body()?.status == "success") {
                                Toast.makeText(context, "Owner Account Created!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(context, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                                context.startActivity(intent, options)
                            } else {
                                Toast.makeText(context, "Server Error: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("DialDishNetwork", "Signup failed: ", e)
                            Toast.makeText(context, "Network Error! Is the PHP server running?", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Continue", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showFailDialog) {
        AlertDialog(
            onDismissRequest = { showFailDialog = false },
            title = { Text("Verification Failed ❌") },
            text = { Text("Invalid FSSAI number. It must be exactly 14 digits. Owner login denied.") },
            confirmButton = {
                TextButton(onClick = { showFailDialog = false }) { Text("Try Again", color = Color.Red) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Create Account", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Join DialDish to get started", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

        CustomTextField("FULL NAME", fullName, "Enter your Full name", Icons.Filled.Person, fullNameError) { fullName = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField("EMAIL", email, "Enter your Mail", Icons.Filled.Email, emailError, KeyboardType.Email) { email = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField("PHONE NUMBER", phone, "Enter your Phone Number", Icons.Filled.Phone, phoneError, KeyboardType.Phone) { phone = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomPasswordField("PASSWORD", password, passwordError, passwordVisible, { passwordVisible = it }) { password = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomPasswordField("CONFIRM PASSWORD", confirmPassword, confirmPasswordError, confirmPasswordVisible, { confirmPasswordVisible = it }) { confirmPassword = it }
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "SIGN UP AS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RoleSelectorButton("User", selectedRole == "User", Modifier.weight(1f)) { selectedRole = "User" }
            RoleSelectorButton("Owner", selectedRole == "Owner", Modifier.weight(1f)) { selectedRole = "Owner" }
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (selectedRole == "Owner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF7ED))
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = "STALL DETAILS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField("STALL NAME", stallName, "e.g., Royal Biryani Hub", Icons.Filled.Store, stallNameError) { stallName = it }
                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField("FSSAI NUMBER (14 Digits)", fssaiNumber, "Enter 14-digit FSSAI", Icons.Filled.VerifiedUser, null, KeyboardType.Number) { fssaiNumber = it }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "STALL PHOTO", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (stallImageUri != null) Color(0xFF4CAF50) else Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "Upload", tint = Color.White)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(text = if (stallImageUri != null) "Photo Selected ✅" else "Upload Stall Photo")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Button(
            onClick = {
                if (validateBaseInputs()) {
                    if (selectedRole == "Owner") {
                        if (stallName.isBlank()) {
                            stallNameError = "Stall Name is required"
                            return@Button
                        }
                        stallNameError = null

                        if (fssaiNumber.matches("^\\d{14}$".toRegex())) {
                            showSuccessDialog = true
                        } else {
                            showFailDialog = true
                        }
                    } else {
                        coroutineScope.launch {
                            try {
                                val request = UserSignupRequest(fullName, email, phone, password, "User", null)
                                val response = RetrofitClient.instance.registerUser(request)

                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "User Account Created!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(context, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                                    context.startActivity(intent, options)
                                } else {
                                    Toast.makeText(context, "Server Error: ${response.body()?.message}", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("DialDishNetwork", "Signup failed: ", e)
                                Toast.makeText(context, "Network Error! Is the PHP server running?", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Already have an account? ", color = Color.Gray, fontSize = 14.sp)
            Text(
                text = "Login", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CustomTextField(label: String, value: String, placeholder: String, icon: ImageVector, errorMessage: String?, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = Color.LightGray) },
            leadingIcon = { Icon(icon, contentDescription = label, tint = Color.Gray) },
            isError = errorMessage != null, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, errorContainerColor = Color(0xFFFFF0F0), focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFFE0E0E0)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        if (errorMessage != null) { Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) }
    }
}

@Composable
fun CustomPasswordField(label: String, value: String, errorMessage: String?, isVisible: Boolean, onVisibilityChange: (Boolean) -> Unit, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text("••••••••", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = label, tint = Color.Gray) },
            trailingIcon = { val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { onVisibilityChange(!isVisible) }) { Icon(imageVector = image, contentDescription = "Toggle visibility", tint = Color.Gray) } },
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = errorMessage != null, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, errorContainerColor = Color(0xFFFFF0F0), focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color(0xFFE0E0E0)),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        if (errorMessage != null) { Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) }
    }
}

@Composable
fun RoleSelectorButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.height(50.dp).clip(RoundedCornerShape(12.dp)).background(backgroundColor).border(1.dp, borderColor, RoundedCornerShape(12.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text = text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}
// HELPER FUNCTION: Converts the selected Image into a Base64 String to send to Python
fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() { DialDishTheme { SignupScreen() } }