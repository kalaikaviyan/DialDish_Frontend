package com.simats.dialdish.owner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonOutline
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
import com.simats.dialdish.network.FetchStaffRequest
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.network.StaffMember
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class OOrdersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    bottomBar = { OwnerBottomNavBar(currentSelected = "Orders") }
                ) { innerPadding ->
                    OOrdersDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun OOrdersDashboard(modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live Orders", "Special Requests", "History")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Orders & Dispatch",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> LiveOrdersList()
            1 -> SpecialRequestsList()
            2 -> HistoryAnalyticsList()
        }
    }
}

// ==========================================
// 1. LIVE ORDERS & TRACKING TAB
// ==========================================
@Composable
fun LiveOrdersList() {
    val context = LocalContext.current
    var showAssignDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            AdvancedLiveOrderCard(
                orderId = "#1024",
                customerName = "Arjun T",
                location = "CS Department, Block B",
                items = "2x Chicken Biryani, 1x Coke",
                total = "₹350",
                onAssignClick = { showAssignDialog = true },
                onTrackClick = {
                    val intent = android.content.Intent(context, com.simats.dialdish.map.OMapTrackingActivity::class.java)
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                }
            )
        }
    }

    if (showAssignDialog) {
        AssignStaffDialog(
            onDismiss = { showAssignDialog = false },
            onAssign = { staffId ->
                showAssignDialog = false
                // TODO: Next Phase - Send API call to assign order to staffId
                Toast.makeText(context, "Assigned to $staffId! Code P-4829 generated.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun AdvancedLiveOrderCard(
    orderId: String, customerName: String, location: String, items: String, total: String,
    onAssignClick: () -> Unit, onTrackClick: () -> Unit
) {
    // STATE: 0 = Pending, 1 = Approved (Needs Assignment), 2 = Assigned (Out for Delivery)
    var orderState by remember { mutableIntStateOf(0) }
    var isPaid by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Order $orderId", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(text = total, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Location", tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = location, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = customerName, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = items, fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            // DYNAMIC UI BASED ON STATE
            when (orderState) {
                0 -> { // PENDING
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { /* Reject */ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f)), modifier = Modifier.weight(1f)) {
                            Text("Reject", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { orderState = 1 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.weight(1f)) {
                            Text("Approve", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                1 -> { // APPROVED, NEEDS STAFF
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                            Text(text = if (isPaid) "Marked Paid" else "Unpaid (COD)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isPaid) Color(0xFF4CAF50) else Color.Gray)
                        }
                        Button(onClick = {
                            onAssignClick()
                            orderState = 2 // Mock moving to assigned state
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text("Assign Staff", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                2 -> { // ASSIGNED, OUT FOR DELIVERY
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PersonOutline, contentDescription = "Staff", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delivery Assigned", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Text(text = if (isPaid) "Status: P-4829" else "Collect Cash: $total", fontSize = 12.sp, color = if (isPaid) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        }
                        Button(onClick = onTrackClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Map", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Track Map", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UPDATED API LOGIC: Assign Staff Dialog
// ==========================================
@Composable
fun AssignStaffDialog(onDismiss: () -> Unit, onAssign: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var freeStaffList by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Fetch the list of free staff when the dialog opens!
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Mock Owner ID = 1. Update this to pull from SharedPreferences later
                val request = FetchStaffRequest(owner_id = 1)
                val response = RetrofitClient.instance.getFreeStaff(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success" && body.staff != null) {
                        freeStaffList = body.staff
                    } else {
                        errorMessage = body?.message ?: "No free staff found."
                    }
                } else {
                    errorMessage = "Server Error: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Network Error. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Free Staff", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select an available delivery person to dispatch this order.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (errorMessage != null) {
                    Text(text = errorMessage!!, color = Color.Red, fontWeight = FontWeight.Medium)
                } else {
                    // Display the REAL data from XAMPP
                    freeStaffList.forEach { staff ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                                .clickable { onAssign(staff.staff_id) }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Free", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "${staff.name} (${staff.staff_id})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}

// ==========================================
// 2. SPECIAL REQUESTS TAB
// ==========================================
@Composable
fun SpecialRequestsList() {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Event Committe", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "10:42 AM", fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                        Text(text = "Hi, can you prepare 20 plates of Biryani for an event at 1 PM today? Will pay upfront.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Reply", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reply", color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f)) {
                            Text("Convert to Order", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. HISTORY & ANALYTICS TAB
// ==========================================
@Composable
fun HistoryAnalyticsList() {
    val filters = listOf("Today", "This Week", "Canceled", "Ramesh (AD3214)", "Suresh (AD3215)")
    var selectedFilter by remember { mutableStateOf("Today") }

    Column(modifier = Modifier.fillMaxSize()) {
        // SMART FILTERS
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (selectedFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface).clickable { selectedFilter = filter }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = filter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedFilter == filter) Color.White else Color.Gray)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                HistoryCard(orderId = "#1023", customerName = "Vikram S", staffName = "Ramesh (AD3214)", status = "Delivered", finalCode = "PD-4821", amount = "₹200", isCash = true)
            }
        }
    }
}

@Composable
fun HistoryCard(orderId: String, customerName: String, staffName: String, status: String, finalCode: String, amount: String, isCash: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, if (status == "Canceled") Color(0xFFF44336).copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Order $orderId", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = customerName, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonOutline, contentDescription = "Staff", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = staffName, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = amount, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (isCash) Color(0xFFFF9800) else Color(0xFF4CAF50))
                Text(text = if (isCash) "Cash to Collect" else "Paid Online", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = status, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (status == "Canceled") Color(0xFFF44336) else Color(0xFF4CAF50))
                if (status != "Canceled") Text(text = "Code: $finalCode", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OOrdersDashboardPreview() { DialDishTheme { OOrdersDashboard() } }