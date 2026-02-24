package com.simats.dialdish.owner

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

class OAppFeaturesActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("App Features", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    AppFeaturesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppFeaturesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        FeatureCategory("👨‍🍳 Owner Features", listOf(
            "Live GPS Tracking via OpenStreetMap",
            "Smart 'In Stock' Toggle System",
            "1-Click Staff Assignment",
            "End-of-Day Cash Settlement Tracker"
        ))
        Spacer(modifier = Modifier.height(16.dp))
        FeatureCategory("📱 User Features", listOf(
            "Secure 4-Digit Delivery PIN Verification",
            "Real-time Food Tracking",
            "FSSAI Verified Stalls Only",
            "Direct In-App Custom Order Requests"
        ))
        Spacer(modifier = Modifier.height(16.dp))
        FeatureCategory("🛵 Delivery Staff Features", listOf(
            "Secure, Auto-Generated Login IDs",
            "Live 'Free / Working' State Management",
            "Instant Cash Collection Verification",
            "Direct Integration with Owner Dashboard"
        ))
    }
}

@Composable
fun FeatureCategory(title: String, features: List<String>) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Column {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(12.dp))
            features.forEach { feature ->
                Text(text = "• $feature", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun AppFeaturesScreenPreview() {
    DialDishTheme { AppFeaturesScreen() }
}