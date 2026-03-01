package com.simats.directdine.owner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.ui.theme.directdineTheme

class OHowToUseActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("How to use the App", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    HowToUseScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HowToUseScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        InfoCard("1. Managing Your Menu", "Go to the Menu tab. You do not need to delete items when they run out. Simply toggle the 'In Stock' switch off, and it will instantly hide from users.")
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("2. Adding Delivery Staff", "Click 'Add New Delivery Personnel' on the Home Dashboard. Ensure you upload their photo and Aadhar. A unique login code (e.g., AD3214) will be generated for them to use as a password.")
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("3. Dispatching Orders", "When an order arrives, click 'Approve'. Then, click 'Assign Staff' to open a list of your Free delivery workers. Selecting one will instantly send the order to their app.")
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("4. EOD Cash Settlement", "Use the History tab to track 'Cash to Collect'. Verify the total amount your delivery staff has gathered before confirming their end-of-shift status.")
    }
}

@Composable
fun InfoCard(title: String, description: String) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, lineHeight = 22.sp)
        }
    }
}
@Preview(showBackground = true)
@Composable
fun HowToUseScreenPreview() {
    directdineTheme { HowToUseScreen() }
}
