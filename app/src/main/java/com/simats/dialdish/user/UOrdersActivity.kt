package com.simats.dialdish.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.simats.dialdish.map.OMapTrackingActivity
import com.simats.dialdish.network.*
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch
import java.util.Calendar

class UOrdersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                UOrdersScreen()
            }
        }
    }
}

fun showDatePicker(context: Context, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UOrdersScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val userId = userIdStr.toIntOrNull() ?: -1

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Active Orders", "Past Orders")

    var isLoading by remember { mutableStateOf(true) }
    var myOrders by remember { mutableStateOf<List<UserOrder>>(emptyList()) }
    var filterDate by remember { mutableStateOf<String?>(null) }

    // Phase 4: Cancellation States
    var orderToCancel by remember { mutableStateOf<UserOrder?>(null) }
    var isCanceling by remember { mutableStateOf(false) }

    fun fetchOrders() {
        if (userId != -1) {
            coroutineScope.launch {
                try {
                    val response = RetrofitClient.instance.getUserOrders(FetchUserOrdersRequest(userId))
                    if (response.isSuccessful && response.body()?.status == "success") {
                        myOrders = response.body()?.orders ?: emptyList()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    LaunchedEffect(userId) { fetchOrders() }

    val activeOrders = myOrders.filter { it.status in listOf("Pending", "Preparing", "Out_for_Delivery") }
    val pastOrders = myOrders.filter { it.status in listOf("Delivered", "Canceled") }.filter {
        // Robust check: ensure the date string starts with or contains the exact YYYY-MM-DD
        if (filterDate != null) {
            it.created_at.startsWith(filterDate!!) || it.created_at.contains(filterDate!!)
        } else {
            true
        }
    }

    // --- PHASE 4: CANCEL WARNING DIALOG ---
    if (orderToCancel != null) {
        val isPreparing = orderToCancel!!.status == "Preparing"
        val isPaid = orderToCancel!!.payment_method == "Online" || orderToCancel!!.payment_status == "Paid"

        AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel Order?", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (isPreparing) {
                        Text("The Stall Owner has already started preparing your food!", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isPaid) {
                            Text("A 50% cancellation penalty will be applied to compensate for wasted ingredients. You will only receive a partial refund.", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            Text("Since this is a Handcash order, please respect the stall owner's effort. Frequent cancellations will result in account strikes.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        Text("Are you sure you want to cancel this order? It has not been accepted by the kitchen yet.", fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isCanceling,
                    onClick = {
                        isCanceling = true
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.instance.cancelOrder(CancelOrderRequest(orderToCancel!!.order_id, userId))
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, res.body()?.message ?: "Order Canceled", Toast.LENGTH_LONG).show()
                                    orderToCancel = null
                                    fetchOrders() // Refresh the list!
                                } else {
                                    Toast.makeText(context, "Error: ${res.body()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isCanceling = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (isCanceling) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text("Confirm Cancel")
                }
            },
            dismissButton = { TextButton(onClick = { orderToCancel = null }) { Text("Keep Order", color = Color.Gray) } }
        )
    }

    Scaffold(
        bottomBar = { UserBottomNav(currentSelection = 2, context = context) },
        topBar = {
            TopAppBar(
                title = { Text("My Orders", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = MaterialTheme.colorScheme.primary, height = 3.dp)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(text = title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    if (selectedTabIndex == 0) {
                        if (activeOrders.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text("No active orders", color = Color.Gray) } }
                        } else {
                            items(activeOrders, key = { it.order_id }) { order ->
                                ActiveOrderCard(
                                    order = order,
                                    context = context,
                                    onCancelClick = { orderToCancel = order }
                                )
                            }
                        }
                    } else {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (filterDate == null) "All Past Orders" else "Filtered: $filterDate", fontWeight = FontWeight.Bold, color = Color.Gray)
                                IconButton(onClick = { showDatePicker(context) { selected -> filterDate = selected } }) {
                                    Icon(Icons.Filled.DateRange, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (filterDate != null) {
                                TextButton(onClick = { filterDate = null }) { Text("Clear Filter", color = Color.Red) }
                            }
                        }

                        if (pastOrders.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { Text("No past orders found", color = Color.Gray) } }
                        } else {
                            items(pastOrders, key = { it.order_id }) { order ->
                                PastOrderCard(
                                    order = order,
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            try {
                                                val response = RetrofitClient.instance.hideUserOrder(HideUserOrderRequest(order.order_id))
                                                if (response.isSuccessful) {
                                                    myOrders = myOrders.filter { it.order_id != order.order_id }
                                                    Toast.makeText(context, "Order removed from history", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ActiveOrderCard(order: UserOrder, context: Context, onCancelClick: () -> Unit) {
    val baseUrl = RetrofitClient.BASE_URL

    val statusText = when (order.status) {
        "Pending" -> "Requested"
        "Preparing" -> "Preparing"
        "Out_for_Delivery" -> "Out for Delivery"
        else -> order.status
    }

    val badgeColor = when (order.status) {
        "Pending" -> Color.Gray
        "Preparing" -> Color(0xFFF57C00)
        "Out_for_Delivery" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order #${order.order_id}", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(order.created_at, color = Color.Gray, fontSize = 10.sp)
                }
                Text("₹${order.total_amount}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(order.stall_name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text(order.items_summary, color = Color.DarkGray, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // DYNAMIC STATUS BADGE
            Surface(color = badgeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (order.status) {
                        "Pending" -> Icons.Filled.AccessTime
                        "Preparing" -> Icons.Filled.SoupKitchen
                        "Out_for_Delivery" -> Icons.Filled.Moped
                        else -> Icons.Filled.Info
                    }
                    Icon(icon, contentDescription = "Status", tint = badgeColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(statusText, color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // VISUAL TIMELINE
            val step = when(order.status) { "Pending" -> 1; "Preparing" -> 2; "Out_for_Delivery" -> 3; else -> 1 }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TimelineStep(text = "Pending", isActive = step >= 1)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (step >= 2) Color(0xFFF57C00) else Color(0xFFEEEEEE), thickness = 2.dp)
                TimelineStep(text = "Preparing", isActive = step >= 2)
                Divider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (step >= 3) Color(0xFFF57C00) else Color(0xFFEEEEEE), thickness = 2.dp)
                TimelineStep(text = "On the Way", isActive = step >= 3)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (step >= 2 && !order.verification_code.isNullOrEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF3E0)).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Share this code with delivery partner", color = Color(0xFFE65100), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.verification_code, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 4.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (step >= 2 && !order.delivery_man_name.isNullOrEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val safePhotoUrl = order.delivery_man_photo?.replace("\\", "/") ?: ""
                        val photoUrl = if (safePhotoUrl.startsWith("uploads/")) "$baseUrl$safePhotoUrl" else "${baseUrl}uploads/$safePhotoUrl"

                        if (safePhotoUrl.isNotEmpty()) {
                            AsyncImage(model = photoUrl.trim().replace(" ", "%20"), contentDescription = "Delivery Partner", contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray))
                        } else {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Person, contentDescription = "User", tint = Color.Gray) }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.delivery_man_name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Moped, contentDescription = "Bike", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delivery Partner", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        if (!order.delivery_man_phone.isNullOrEmpty()) {
                            IconButton(
                                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${order.delivery_man_phone}") }) },
                                modifier = Modifier.background(Color(0xFFE8F5E9), CircleShape)
                            ) { Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Color(0xFF2E7D32)) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- PHASE 4: CANCEL BUTTON & MAP TRACKING ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Cancel button only shows if it hasn't left the kitchen
                if (order.status == "Pending" || order.status == "Preparing") {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Cancel Order", fontWeight = FontWeight.Bold) }
                }

                if (order.status == "Out_for_Delivery") {
                    Button(
                        onClick = {
                            val intent = Intent(context, OMapTrackingActivity::class.java)
                            intent.putExtra("TRACKING_ORDER_ID", order.order_id)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Track", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Track Live", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStep(text: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (isActive) Color(0xFFF57C00) else Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
            if (isActive) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, fontSize = 10.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) Color(0xFFF57C00) else Color.Gray)
    }
}

@Composable
fun PastOrderCard(order: UserOrder, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order #${order.order_id}", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    Text(order.created_at, fontSize = 10.sp, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (order.status == "Canceled") Color.Red else Color(0xFF4CAF50)
                    Text(order.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.stall_name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("₹${order.total_amount}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(order.items_summary, fontSize = 13.sp, color = Color.DarkGray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UOrdersPreview() { DialDishTheme { UOrdersScreen() } }