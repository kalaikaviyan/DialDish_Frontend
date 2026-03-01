package com.simats.directdine.owner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.ui.theme.directdineTheme

class OInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val infoType = intent.getStringExtra("INFO_TYPE") ?: "HOW_TO"

        enableEdgeToEdge()
        setContent {
            directdineTheme {
                OInfoScreen(infoType = infoType, onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OInfoScreen(infoType: String, onBackClick: () -> Unit) {
    val title = when(infoType) {
        "HOW_TO" -> "How to Use directdine"
        "FEATURES" -> "App Features"
        "LEGAL" -> "Privacy Policy & Legal"
        else -> "Information"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            when(infoType) {
                "HOW_TO" -> {
                    Text("Getting Started as a Stall Owner", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("1. Live Kitchen Dashboard", "Monitor incoming orders in real-time. Approve them to immediately notify the user that food preparation has started.")
                    InfoSection("2. Dispatching & Staff", "Assign prepared orders to your registered delivery partners. You can track their live location while they deliver.")
                    InfoSection("3. Managing Handcash", "Use the 'Profile > Manage Staff' section to view live handcash balances held by your delivery staff and process End-of-Day settlements.")
                    InfoSection("4. Automated Menus", "Your menu automatically locks and unlocks based on your configured breakfast, lunch, and dinner schedules. You can also manually toggle your store 'Offline' in settings.")
                }
                "FEATURES" -> {
                    Text("The directdine Ecosystem", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("👨‍🍳 For Stall Owners", "• Live Kitchen Dashboard\n• Dynamic Staff Settlement\n• Automatic Menu Locking")
                    InfoSection("📱 For Campus Users", "• Gemini Voice AI Ordering\n• Multi-Address Mapping\n• Live Order Tracking")
                    InfoSection("🛵 For Delivery Partners", "• Live OpenStreetMap Tracking\n• Handcash Ledger Management\n• Secure PIN Verification")
                }
                "LEGAL" -> {
                    Text("Privacy Policy, Cancellation & Legal", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("1. Refund Lock Protocol", "If a user cancels an order early or you reject a paid order, a refund is owed. Failure to process this refund will lock your dashboard from receiving new orders.")
                    InfoSection("2. Handcash Accountability", "The directdine system maintains an immutable ledger of handcash collected by delivery partners. You are responsible for settling this balance with them directly.")
                    InfoSection("3. Order Data Privacy", "User location and contact details are masked and only available to you and your delivery staff during an active delivery lifecycle.")
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun InfoSection(title: String, description: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(6.dp))
    Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
    Spacer(modifier = Modifier.height(24.dp))
}