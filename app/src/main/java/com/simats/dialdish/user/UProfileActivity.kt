package com.simats.dialdish.user

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.dialdish.network.*
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class UProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                UProfileScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UProfileScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val userId = userIdStr.toIntOrNull() ?: -1
    val baseUrl = RetrofitClient.BASE_URL

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBase64 by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Helper to convert Image URI to Base64 String
    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Compress to 70% quality
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageBase64 = uriToBase64(uri)
        }
    }

    LaunchedEffect(userId) {
        if (userId != -1) {
            try {
                val response = RetrofitClient.instance.getProfile(GetProfileRequest(userId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!
                    fullName = data.full_name ?: ""
                    phone = data.phone ?: ""
                    email = data.email ?: ""
                    profileImageUrl = data.profile_image_url
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        bottomBar = { UserBottomNav(currentSelection = 3, context = context) },
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA))
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFF57C00))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFAFA))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier.size(110.dp).clip(CircleShape).background(Color(0xFFFFCC80)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            // Show newly selected image before saving
                            AsyncImage(model = selectedImageUri, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (!profileImageUrl.isNullOrEmpty()) {
                            // Show existing DB image
                            val fullImgUrl = if (profileImageUrl!!.startsWith("uploads/")) "$baseUrl$profileImageUrl" else "${baseUrl}uploads/$profileImageUrl"
                            AsyncImage(model = fullImgUrl.trim().replace(" ", "%20"), contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            // Default icon
                            Icon(Icons.Filled.Person, contentDescription = "Photo", modifier = Modifier.size(60.dp), tint = Color.White)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF57C00),
                        modifier = Modifier.size(36.dp).shadow(4.dp, CircleShape),
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Photo", tint = Color.White, modifier = Modifier.padding(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(context, UAddressActivity::class.java))
                    }.shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFFFF3E0), modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Location", tint = Color(0xFFF57C00), modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Manage Addresses", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Add Map Locations & Hostels", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("PERSONAL DETAILS", modifier = Modifier.fillMaxWidth(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE0E0E0), focusedBorderColor = Color(0xFFF57C00), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE0E0E0), focusedBorderColor = Color(0xFFF57C00), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it }, label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE0E0E0), focusedBorderColor = Color(0xFFF57C00), focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text("SECURITY", modifier = Modifier.fillMaxWidth(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().clickable {
                        context.startActivity(Intent(context, UChangePasswordActivity::class.java))
                    }.shadow(1.dp, RoundedCornerShape(12.dp))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Change Password", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    enabled = !isSaving,
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val req = UpdateProfileRequest(userId, fullName, phone, email, imageBase64)
                                val res = RetrofitClient.instance.updateProfile(req)
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                                    sharedPrefs.edit().putString("LOGGED_IN_USER_NAME", fullName).apply()
                                } else {
                                    Toast.makeText(context, "Update Failed", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}