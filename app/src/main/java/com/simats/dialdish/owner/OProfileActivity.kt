package com.simats.dialdish.owner

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

class OProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    bottomBar = { OwnerBottomNavBar(currentSelected = "Profile") }
                ) { innerPadding ->
                    OProfileDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun OProfileDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // --- EDITABLE STATE VARIABLES ---
    var ownerName by remember { mutableStateOf("Suriya Kumar") }
    var ownerPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var stallPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // --- LOCKED MOCK DATA ---
    val fssai = "12421000000123"
    val stallName = "Royal Biryani Hub"
    val location = "Food Court A, SIMATS"
    val stallId = "RB" // The first two letters lock
    val phone = "9876543210"
    val email = "suriya.rbh@gmail.com"

    // --- IMAGE PICKERS ---
    val ownerImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> ownerPhotoUri = uri }
    val stallImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> stallPhotoUri = uri }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER
        Text(text = "My Profile", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))

        // --- SECTION 1: EDITABLE PHOTOS ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Owner Photo
            PhotoUploadAvatar(
                title = "Owner Photo",
                icon = Icons.Filled.Person,
                hasImage = ownerPhotoUri != null,
                onClick = { ownerImagePicker.launch("image/*") }
            )
            // Stall Photo
            PhotoUploadAvatar(
                title = "Stall Photo",
                icon = Icons.Filled.Store,
                hasImage = stallPhotoUri != null,
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
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Edit Name", tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- SECTION 3: LOCKED BUSINESS DETAILS ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "BUSINESS DETAILS", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface) // Uses the surface color for dark/light mode
                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                LockedDataRow(label = "Stall Name", value = stallName)
                LockedDataRow(label = "Stall ID Prefix", value = stallId)
                LockedDataRow(label = "FSSAI Number", value = fssai)
                LockedDataRow(label = "Location", value = location)
                LockedDataRow(label = "Email Address", value = email)
                LockedDataRow(label = "Phone Number", value = phone, isLast = true)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // --- SECTION 4: ACTIONS ---
        OutlinedButton(
            onClick = {
                val intent = Intent(context, OChangePasswordActivity::class.java)
                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                context.startActivity(intent, options)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Change Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = "Save Changes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(32.dp)) // Extra padding for bottom nav
    }
}

// ==========================================
// REUSABLE CUSTOM UI COMPONENTS
// ==========================================

@Composable
fun PhotoUploadAvatar(title: String, icon: ImageVector, hasImage: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(if (hasImage) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                .border(2.dp, if (hasImage) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // If image is selected, show checkmark. Otherwise, show default icon
            Icon(
                imageVector = if (hasImage) Icons.Filled.CameraAlt else icon,
                contentDescription = title,
                tint = if (hasImage) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(40.dp)
            )

            // Little edit badge icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun LockedDataRow(label: String, value: String, isLast: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)

        if (!isLast) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.Gray.copy(alpha = 0.1f))
        }
    }
}

// Preview Block for Android Studio
@Preview(showBackground = true)
@Composable
fun OProfileDashboardPreview() {
    DialDishTheme {
        OProfileDashboard()
    }
}