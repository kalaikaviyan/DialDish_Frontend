package com.simats.dialdish.owner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

class OHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    bottomBar = { OwnerBottomNavBar(currentSelected = "Home") }
                ) { innerPadding ->
                    OHomeDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun OHomeDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP HEADER WITH SETTINGS ICON
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Dashboard", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)

                // Settings Icon Button
// Settings Icon Button
                IconButton(onClick = {
                    val intent = android.content.Intent(context, OSettingsActivity::class.java)
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // FEATURE 1: Today's Approved & Rejected Orders (Will fetch CURDATE() from PHP)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF4CAF50).copy(alpha = 0.1f)).padding(16.dp)) {
                    Column {
                        Text("Approved Today", fontSize = 14.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        Text("24", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF44336).copy(alpha = 0.1f)).padding(16.dp)) {
                    Column {
                        Text("Rejected Today", fontSize = 14.sp, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                        Text("3", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // FEATURE 2: Add Delivery Man Button
        item {
            Button(
                onClick = {
                    val intent = Intent(context, OAddDeliveryManActivity::class.java)
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add New Delivery Personnel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // NEW FEATURE: Manage Staff & EOD Settlements Button
        item {
            OutlinedButton(
                onClick = {
                    val intent = Intent(context, OManageStaffActivity::class.java)
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Group, contentDescription = "Manage")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Manage Staff & Settlements", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // FEATURE 3: List of Free and Working Delivery Personnel
        item {
            Text(text = "Live Delivery Staff Status", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Delivery Personnel List
        item { DeliveryStaffCard(name = "Ramesh Kumar", status = "Free", id = "AD3214") }
        item { DeliveryStaffCard(name = "Suresh Singh", status = "Working", id = "AD3215") }
    }
}

@Composable
fun DeliveryStaffCard(name: String, status: String, id: String) {
    val isFree = status == "Free"
    val statusColor = if (isFree) Color(0xFF4CAF50) else Color(0xFFFF9800)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = "ID: $id", fontSize = 12.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(statusColor.copy(alpha = 0.2f)).padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OHomeDashboardPreview() {
    DialDishTheme {
        OHomeDashboard()
    }
}