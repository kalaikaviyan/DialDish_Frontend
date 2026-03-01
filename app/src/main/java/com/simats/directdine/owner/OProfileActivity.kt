package com.simats.directdine.owner

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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.directdine.network.OwnerIdRequest
import com.simats.directdine.network.RetrofitClient
import com.simats.directdine.network.UpdateOwnerProfileRequest
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class OProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                Scaffold(
                    bottomBar = { OwnerBottomNavBar(currentSelected = "Profile") },
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("My Profile", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA))
                        )
                    }
                ) { innerPadding ->
                    OProfileDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OProfileDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)

    val ownerIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val ownerId = ownerIdStr.toIntOrNull() ?: -1
    val baseUrl = RetrofitClient.BASE_URL

    // --- DYNAMIC STATE VARIABLES ---
    var ownerName by remember { mutableStateOf("") }

    // DB Image URLs
    var dbOwnerPhotoUrl by remember { mutableStateOf<String?>(null) }
    var dbStallPhotoUrl by remember { mutableStateOf<String?>(null) }

    // Selected Local URIs (for Preview before saving)
    var selectedOwnerUri by remember { mutableStateOf<Uri?>(null) }
    var selectedStallUri by remember { mutableStateOf<Uri?>(null) }

    // Base64 Strings (to send to server)
    var ownerBase64 by remember { mutableStateOf<String?>(null) }
    var stallBase64 by remember { mutableStateOf<String?>(null) }

    // --- LOCKED DATA STATES ---
    var fssai by remember { mutableStateOf("") }
    var stallName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var stallPrefix by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Helper to convert Image URI to Base64 String
    fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    // --- IMAGE PICKERS ---
    val ownerImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedOwnerUri = uri
            ownerBase64 = uriToBase64(uri)
        }
    }

    val stallImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedStallUri = uri
            stallBase64 = uriToBase64(uri)
        }
    }

    // Fetch Data on Load
    LaunchedEffect(ownerId) {
        if (ownerId != -1) {
            try {
                val response = RetrofitClient.instance.getOwnerProfile(OwnerIdRequest(ownerId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!
                    ownerName = data.full_name ?: ""
                    dbOwnerPhotoUrl = data.owner_image_url
                    dbStallPhotoUrl = data.stall_image_url

                    stallName = data.stall_name ?: ""
                    stallPrefix = data.stall_prefix ?: ""
                    fssai = data.fssai ?: ""
                    location = data.location ?: ""
                    phone = data.phone ?: ""
                    email = data.email ?: ""
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFF57C00))
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTION 1: EDITABLE PHOTOS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // Owner Photo
                PhotoUploadAvatar(
                    title = "Owner Photo",
                    icon = Icons.Filled.Person,
                    selectedUri = selectedOwnerUri,
                    dbUrl = dbOwnerPhotoUrl,
                    baseUrl = baseUrl,
                    onClick = { ownerImagePicker.launch("image/*") }
                )
                // Stall Photo
                PhotoUploadAvatar(
                    title = "Stall Photo",
                    icon = Icons.Filled.Store,
                    selectedUri = selectedStallUri,
                    dbUrl = dbStallPhotoUrl,
                    baseUrl = baseUrl,
                    onClick = { stallImagePicker.launch("image/*") }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // --- SECTION 2: EDITABLE DETAILS ---
            Text(text = "PERSONAL DETAILS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Owner Full Name") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Edit Name", tint = Color(0xFFF57C00)) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFF57C00),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- SECTION 3: LOCKED BUSINESS DETAILS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "BUSINESS DETAILS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LockedDataRow(label = "Stall Name", value = stallName)
                    LockedDataRow(label = "Stall ID Prefix", value = stallPrefix)
                    LockedDataRow(label = "FSSAI Number", value = fssai)
                    LockedDataRow(label = "Location", value = location)
                    LockedDataRow(label = "Email Address", value = email)
                    LockedDataRow(label = "Phone Number", value = phone, isLast = true)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // --- SECTION 4: ACTIONS ---
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                color = Color.White,
                modifier = Modifier.fillMaxWidth().clickable {
                    val intent = Intent(context, com.simats.directdine.user.UChangePasswordActivity::class.java)
                    context.startActivity(intent)
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = !isSaving,
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val req = UpdateOwnerProfileRequest(ownerId, ownerName, ownerBase64, stallBase64)
                            val res = RetrofitClient.instance.updateOwnerProfile(req)
                            if(res.isSuccessful && res.body()?.status == "success") {
                                Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                                sharedPrefs.edit().putString("LOGGED_IN_USER_NAME", ownerName).apply()
                            } else {
                                Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(text = "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// REUSABLE CUSTOM UI COMPONENTS
// ==========================================

@Composable
fun PhotoUploadAvatar(title: String, icon: ImageVector, selectedUri: Uri?, dbUrl: String?, baseUrl: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFF3E0)) // Creamy Orange Default Background
                .border(2.dp, Color(0xFFF57C00), CircleShape) // Primary Orange Border
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri != null) {
                AsyncImage(model = selectedUri, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (!dbUrl.isNullOrEmpty() && dbUrl != "null") {
                val fullUrl = if (dbUrl.startsWith("uploads/")) "$baseUrl$dbUrl" else "${baseUrl}uploads/$dbUrl"
                AsyncImage(model = fullUrl.trim().replace(" ", "%20"), contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                // Default Icon if no image exists
                Icon(imageVector = icon, contentDescription = title, tint = Color(0xFFF57C00), modifier = Modifier.size(50.dp))
            }

            // Edit badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF57C00)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun LockedDataRow(label: String, value: String, isLast: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)

        if (!isLast) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color(0xFFEEEEEE))
        }
    }
}