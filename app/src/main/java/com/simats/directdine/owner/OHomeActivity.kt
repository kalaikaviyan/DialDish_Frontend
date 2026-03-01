package com.simats.directdine.owner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.simats.directdine.network.DashboardStatsRequest
import com.simats.directdine.network.RetrofitClient
import com.simats.directdine.network.StoreStatusRequest
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch
import java.util.Calendar

class OHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
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
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)

    val ownerName = sharedPrefs.getString("LOGGED_IN_USER_NAME", "Partner") ?: "Partner"
    val stallId = sharedPrefs.getInt("OWNER_STALL_ID", -1)

    val initialOpenState = sharedPrefs.getBoolean("IS_STORE_OPEN", false)
    var isAcceptingOrders by remember { mutableStateOf(initialOpenState) }

    // LIVE STATS STATE
    var approvedCount by remember { mutableIntStateOf(0) }
    var rejectedCount by remember { mutableIntStateOf(0) }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning,"
        in 12..15 -> "Good Afternoon,"
        in 16..19 -> "Good Evening,"
        else -> "Good Night,"
    }

    // AUTO-REFRESH LOGIC: Triggers every time the screen comes into view (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (stallId != -1) {
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.instance.getDashboardStats(DashboardStatsRequest(stallId))
                            if (response.isSuccessful && response.body()?.status == "success") {
                                approvedCount = response.body()?.approved_today ?: 0
                                rejectedCount = response.body()?.rejected_today ?: 0
                            }
                        } catch (e: Exception) {
                            // Silently ignore network failures on auto-refresh
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun toggleStoreStatus(isOpen: Boolean) {
        if (stallId == -1) {
            Toast.makeText(context, "Error: Stall ID not found. Please log out and log in again.", Toast.LENGTH_LONG).show()
            return
        }

        isAcceptingOrders = isOpen
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.updateStoreStatus(StoreStatusRequest(stallId, isOpen))
                if (response.isSuccessful && response.body()?.status == "success") {
                    sharedPrefs.edit().putBoolean("IS_STORE_OPEN", isOpen).apply()
                    Toast.makeText(context, if (isOpen) "Store is now LIVE!" else "Store Closed.", Toast.LENGTH_SHORT).show()
                } else {
                    isAcceptingOrders = !isOpen
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isAcceptingOrders = !isOpen
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = greeting, fontSize = 16.sp, color = Color.Gray)
                    Text(text = "$ownerName! 👋", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = {
                    val intent = Intent(context, OSettingsActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Accepting Orders", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = if (isAcceptingOrders) "Store is Open 🟢" else "Store is Closed 🔴",
                        fontSize = 14.sp,
                        color = if (isAcceptingOrders) Color(0xFF4CAF50) else Color.Red
                    )
                }
                Switch(
                    checked = isAcceptingOrders,
                    onCheckedChange = { toggleStoreStatus(it) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF4CAF50).copy(alpha = 0.1f)).padding(16.dp)) {
                    Column {
                        Text("Approved Today", fontSize = 14.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        // LIVE DATA INSERTED HERE
                        Text("$approvedCount", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF44336).copy(alpha = 0.1f)).padding(16.dp)) {
                    Column {
                        Text("Rejected Today", fontSize = 14.sp, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                        // LIVE DATA INSERTED HERE
                        Text("$rejectedCount", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Button(
                onClick = { context.startActivity(Intent(context, OAddDeliveryManActivity::class.java)) },
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

        item {
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, OManageStaffActivity::class.java)) },
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
    }
}

@Preview(showBackground = true)
@Composable
fun OHomeDashboardPreview() { directdineTheme { OHomeDashboard() } }