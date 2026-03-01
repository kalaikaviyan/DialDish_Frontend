package com.simats.directdine.user

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

class UInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val infoType = intent.getStringExtra("INFO_TYPE") ?: "HOW_TO"

        enableEdgeToEdge()
        setContent {
            directdineTheme {
                UInfoScreen(infoType = infoType, onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UInfoScreen(infoType: String, onBackClick: () -> Unit) {
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
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            when(infoType) {
                "HOW_TO" -> {
                    Text("Getting Started as a User", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("1. Address Mapping", "Save multiple addresses (Hostel, Dept) using the interactive map to ensure perfect delivery accuracy.")
                    InfoSection("2. Voice Ordering", "Tap the Mic icon to use our Gemini AI. Say 'Order Biryani' or 'Call Royal Stall' and the AI will execute it instantly!")
                    InfoSection("3. Payment Verification", "Provide the unique 4-digit PIN to the delivery partner upon arrival to securely complete the transaction.")
                }
                "FEATURES" -> {
                    Text("The directdine Ecosystem", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("📱 For Campus Users", "• Gemini Voice AI Ordering\n• Multi-Address Mapping\n• Live Order Tracking")
                    InfoSection("🛵 For Delivery Partners", "• Live OpenStreetMap Tracking\n• Handcash Ledger Management\n• Secure PIN Verification")
                    InfoSection("👨‍🍳 For Stall Owners", "• Live Kitchen Dashboard\n• Dynamic Staff Settlement\n• Automatic Menu Locking")
                }
                "LEGAL" -> {
                    Text("Privacy Policy, Cancellation & Legal", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    InfoSection("1. Cancellation Policy", "Late cancellations (after food preparation begins) will result in a 50% penalty to compensate the stall owner.")
                    InfoSection("2. The 10-Minute Wait Rule", "If you are unreachable for 10 minutes when the driver arrives, the order is canceled and you receive an Anti-Fraud Strike.")
                    InfoSection("3. Data Privacy", "Your location is strictly used for delivery purposes and is never shared outside of the active order lifecycle.")
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