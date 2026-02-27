package com.simats.dialdish.delivery

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.dialdish.network.DeliveryProfileRequest
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class DProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                DProfileScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DProfileScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val staffId = sharedPrefs.getInt("DELIVERY_STAFF_ID", -1)
    val baseUrl = RetrofitClient.BASE_URL

    // State Variables
    var fullName by remember { mutableStateOf("Loading...") }
    var staffIdCode by remember { mutableStateOf("...") }
    var phone by remember { mutableStateOf("...") }
    var aadhar by remember { mutableStateOf("...") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(staffId) {
        if (staffId != -1) {
            try {
                val response = RetrofitClient.instance.getDeliveryProfile(DeliveryProfileRequest(staffId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()!!
                    fullName = data.full_name ?: "Unknown"
                    staffIdCode = data.staff_id_code ?: "N/A"
                    phone = data.phone ?: "N/A"
                    aadhar = data.aadhar_number ?: "N/A"
                    photoUrl = data.profile_image_url
                } else {
                    Toast.makeText(context, "Failed to load identity card", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = {
            // INDEX 2 = PROFILE (ID CARD)
            DeliveryBottomNav(currentSelection = 2, context = context)
        },
        topBar = {
            TopAppBar(
                title = { Text("Staff Identity Card", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color.Black
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF5F5F5))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your details are managed by your Stall Owner. Contact them for updates.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // --- THE DIGITAL ID BADGE ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)) // Dark professional badge
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DIALDISH DELIVERY", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        // Dynamic Profile Photo Loading
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                                .border(4.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!photoUrl.isNullOrEmpty() && photoUrl != "null") {
                                // Fixes the Windows backslash issue causing double 'uploads/uploads/'
                                val cleanUrl = photoUrl!!.replace("\\", "/")
                                val fullUrl = if (cleanUrl.startsWith("uploads/")) "$baseUrl$cleanUrl" else "${baseUrl}uploads/$cleanUrl"
                                AsyncImage(
                                    model = fullUrl.trim().replace(" ", "%20"),
                                    contentDescription = "Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = "Photo", modifier = Modifier.size(60.dp), tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(fullName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("STAFF ID: $staffIdCode", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)

                        Spacer(modifier = Modifier.height(32.dp))

                        // Locked Details Info Box
                        Surface(
                            color = Color(0xFF2C2C2C),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                LockedInfoRow(icon = Icons.Filled.Phone, label = "Phone", value = phone)
                                LockedInfoRow(icon = Icons.Filled.Badge, label = "Aadhar", value = aadhar)
                                LockedInfoRow(icon = Icons.Filled.Lock, label = "Role", value = "Delivery Personnel")
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Replaced QR with Verified Badge
                        Surface(color = Color(0xFF4CAF50).copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verified Active Partner", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockedInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = Color.Gray, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, color = Color.Gray, fontSize = 14.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.DarkGray, modifier = Modifier.size(14.dp)) // Lock indicator
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DProfileActivityPreview() {
    DialDishTheme {
        DProfileScreen()
    }
}