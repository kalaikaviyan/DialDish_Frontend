package com.simats.dialdish.delivery

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
import com.simats.dialdish.ui.theme.DialDishTheme

class DInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val infoType = intent.getStringExtra("INFO_TYPE") ?: "HOW_TO"

        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                DInfoScreen(infoType = infoType, onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DInfoScreen(infoType: String, onBackClick: () -> Unit) {

    val title = when(infoType) {
        "HOW_TO" -> "How to Use DialDish"
        "FEATURES" -> "App Features"
        "LEGAL" -> "Privacy Policy & Legal"
        else -> "Information"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
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
                "HOW_TO" -> HowToUseContent()
                "FEATURES" -> FeaturesContent()
                "LEGAL" -> LegalContent()
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun HowToUseContent() {
    Text("Getting Started as a Delivery Partner", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))

    InfoSection("1. Going Online", "Open the Home Map screen and toggle the switch to 'ONLINE'. Ensure your GPS permissions are granted. You are now visible to your Stall Owner and ready to receive food orders on campus.")
    InfoSection("2. Accepting & Navigating", "When a user places an order, it automatically routes to your screen. The map will show two markers: your bike and the destination. Head to the stall to pick up the food.")
    InfoSection("3. The P-D Handshake", "Once you arrive at the delivery location, ask the user for their 4-digit PIN. Enter it into the app and click 'Verify PIN'. This unlocks the 'P' (Payment/Processed) step.")
    InfoSection("4. Cash Collection", "If the order is Prepaid, the system will clear it automatically. If it is Handcash, you must collect the exact amount shown on your screen in cash or via your personal UPI before pressing 'Payment Done'.")
    InfoSection("5. Delivery Completion", "After the payment is cleared, hand over the parcel and press 'Mark Delivered' to complete the 'D' step. You are now free for the next order!")
}

@Composable
fun FeaturesContent() {
    Text("The DialDish Ecosystem", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))

    InfoSection("🛵 For Delivery Partners", "• Live OpenStreetMap Tracking\n• Real-time Handcash Ledger Management\n• Secure PIN Verification Handshake\n• Instant Settlement Calculators\n• System-wide Dark Mode Integration")
    InfoSection("👨‍🍳 For Stall Owners", "• Live Kitchen Management Dashboard\n• Dynamic Staff Settlement Hub\n• Automatic Menu Locking based on schedules\n• Permanent Digital Ledgers\n• Fraud & Refund Lock Systems")
    InfoSection("📱 For Campus Users", "• Gemini Voice AI Ordering (Call, Text, Search)\n• Multi-Address Mapping (Hostel/Department)\n• Live Order Tracking\n• Strict Verification Codes")
}

@Composable
fun LegalContent() {
    Text("Privacy Policy, Cancellation & Legal", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(16.dp))

    InfoSection("1. Handcash Trust Protocol", "As a Delivery Partner, any Handcash collected from users technically belongs to the Stall Owner. DialDish maintains a permanent, unalterable digital ledger of your active balance. You must surrender this amount, minus negotiated expenses like petrol, at the end of your shift.")
    InfoSection("2. User Cancellation Penalties", "Users cannot casually cancel orders once the kitchen begins preparation. Late cancellations will result in financial penalties for the user, ensuring your time and the stall's food are not wasted.")
    InfoSection("3. The 10-Minute Wait Rule", "If you arrive at the destination and the user is completely unreachable, a 10-minute timer initiates. If the timer expires, you may cancel the order. The Owner keeps the money, and the User receives an 'Anti-Fraud Strike'. 5 strikes result in a permanent ban.")
    InfoSection("4. Location Tracking Privacy", "DialDish strictly respects your privacy. We only pull and broadcast your GPS coordinates when you are actively toggled 'ONLINE'. Once offline, tracking instantly and permanently ceases.")
}

@Composable
fun InfoSection(title: String, description: String) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    Spacer(modifier = Modifier.height(6.dp))
    Text(description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
    Spacer(modifier = Modifier.height(24.dp))
}