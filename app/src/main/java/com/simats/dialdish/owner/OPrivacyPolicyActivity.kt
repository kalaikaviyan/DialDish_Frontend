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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

class OPrivacyPolicyActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Privacy Policy & Legal", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    PrivacyPolicyScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Column {
                Text("DialDish Owner Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                Text("1. Data Collection", fontWeight = FontWeight.Bold)
                Text("We collect your FSSAI number, phone number, and stall details strictly for platform verification and user trust.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))

                Text("2. GPS Tracking", fontWeight = FontWeight.Bold)
                Text("Delivery staff locations are actively tracked and shared with the User and Owner only while an order is in the 'Assigned' state.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))

                Text("3. Data Deletion Rights", fontWeight = FontWeight.Bold)
                Text("You maintain the right to permanently delete your stall account from the Settings menu. This will wipe all historical order data and staff records from our active servers.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp, top = 4.dp))
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun PrivacyPolicyScreenPreview() {
    DialDishTheme { PrivacyPolicyScreen() }
}