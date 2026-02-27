package com.simats.dialdish.owner

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.network.AddStaffRequest
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class OAddDeliveryManActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Add Delivery Personnel", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                }) {
                                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    AddDeliveryManScreen(modifier = Modifier.padding(innerPadding), onFinish = { finish() })
                }
            }
        }
    }
}

@Composable
fun AddDeliveryManScreen(modifier: Modifier = Modifier, onFinish: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var aadhar by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // State for API Calls
    var isLoading by remember { mutableStateOf(false) }
    var generatedStaffId by remember { mutableStateOf<String?>(null) }

    // MOCK STATE: In a real app, fetch this from SharedPreferences after Login
    // ---> REPLACE THE HARDCODED val ownerId = 1 WITH THIS: <---
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)
    // Grab the saved ID. If it's somehow missing, default to -1 to prevent app crashes.
    val ownerIdString = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val ownerId = ownerIdString.toIntOrNull() ?: -1
    var hasPrefix by remember { mutableStateOf(false) }
    var currentPrefix by remember { mutableStateOf("") }

    var showPrefixDialog by remember { mutableStateOf(false) }
    var newPrefix by remember { mutableStateOf("") }
    var prefixError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> photoUri = uri }
    LaunchedEffect(ownerId) {
        if (ownerId != -1) {
            try {
                val request = com.simats.dialdish.network.CheckPrefixRequest(ownerId)
                val response = RetrofitClient.instance.checkPrefix(request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    // STRICT CHECK: Ensure it's true AND the prefix actually contains text
                    val returnedPrefix = response.body()?.prefix
                    if (response.body()?.has_prefix == true && !returnedPrefix.isNullOrBlank()) {
                        hasPrefix = true
                        currentPrefix = returnedPrefix
                    } else {
                        hasPrefix = false
                    }
                }
            } catch (e: Exception) {
                // If network fails silently on load, it will just ask them manually. No crash!
            }
        }
    }
    if (generatedStaffId != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Staff Added Successfully!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Please write down this ID and give it to your delivery personnel. They will use this as their password to log in.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).padding(16.dp)) {
                        Text(text = generatedStaffId!!, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    generatedStaffId = null
                    onFinish()
                }) { Text("Done") }
            }
        )
    }

    if (showPrefixDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Set Stall ID Prefix", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This is your first time adding staff. Enter a unique 2-letter code for your stall (e.g., 'RB'). This cannot be changed later.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPrefix,
                        // Forces the string to always be UPPERCASE
                        onValueChange = { input ->
                            if (input.length <= 2) {
                                newPrefix = input.uppercase()
                            }
                        },
                        label = { Text("2-Letter Code") },
                        isError = prefixError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (prefixError != null) {
                        Text(text = prefixError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPrefix.length != 2 || !newPrefix.matches("^[A-Z]{2}$".toRegex())) {
                        prefixError = "Must be exactly 2 letters"
                    } else {
                        prefixError = null
                        showPrefixDialog = false
                        isLoading = true

                        coroutineScope.launch {
                            try {
                                // FIXED: Convert URI to Base64 Image String
                                val base64Image = photoUri?.let { uriToBase64(context, it) }
                                val request = AddStaffRequest(ownerId, name, phone, email, aadhar, newPrefix, base64Image)

                                val response = RetrofitClient.instance.addDeliveryMan(request)
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    hasPrefix = true
                                    currentPrefix = response.body()?.prefix_used ?: newPrefix
                                    generatedStaffId = response.body()?.generated_id
                                } else {
                                    Toast.makeText(context, response.body()?.message ?: "Error", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }) { Text("Confirm & Generate ID") }
            },
            dismissButton = {
                TextButton(onClick = { showPrefixDialog = false }) { Text("Cancel", color = Color.Gray) }
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
        Text(text = "Enter Staff Details", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))

        CustomStaffTextField("FULL NAME", name, "Enter person name", Icons.Filled.Person) { name = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomStaffTextField("PHONE NUMBER", phone, "Enter 10-digit number", Icons.Filled.Phone, KeyboardType.Phone) { phone = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomStaffTextField("EMAIL ADDRESS", email, "Enter email ID", Icons.Filled.Email, KeyboardType.Email) { email = it }
        Spacer(modifier = Modifier.height(16.dp))
        CustomStaffTextField("AADHAR NUMBER", aadhar, "Enter 12-digit Aadhar", Icons.Filled.Badge, KeyboardType.Number) { aadhar = it }
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "PASSPORT SIZE PHOTO", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = if (photoUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Upload", tint = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (photoUri != null) "Photo Uploaded ✅" else "Upload Photo", color = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (name.isBlank() || phone.isBlank() || email.isBlank() || aadhar.isBlank() || photoUri == null) {
                    Toast.makeText(context, "Please fill all fields and upload photo", Toast.LENGTH_SHORT).show()
                } else {
                    if (!hasPrefix) {
                        showPrefixDialog = true
                    } else {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                // FIXED: Convert URI to Base64 Image String
                                val base64Image = photoUri?.let { uriToBase64(context, it) }
                                val request = AddStaffRequest(ownerId, name, phone, email, aadhar, currentPrefix, base64Image)

                                val response = RetrofitClient.instance.addDeliveryMan(request)
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    generatedStaffId = response.body()?.generated_id
                                } else {
                                    Toast.makeText(context, response.body()?.message ?: "Error", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "Add Delivery Personnel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CustomStaffTextField(label: String, value: String, placeholder: String, icon: ImageVector, keyboardType: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, placeholder = { Text(placeholder, color = Color.LightGray) },
            leadingIcon = { Icon(icon, contentDescription = label, tint = Color.Gray) },
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

// HELPER FUNCTION: Converts the selected Image into a Base64 String to send to Python
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun AddDeliveryManPreview() { DialDishTheme { AddDeliveryManScreen() } }