package com.simats.dialdish.owner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.map.OMapTrackingActivity
import com.simats.dialdish.network.*
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch
import java.util.Calendar

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

fun showOwnerDatePicker(context: Context, onDateSelected: (String) -> Unit) {
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

@Composable
fun OOrdersDashboard(modifier: Modifier = Modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("New Orders", "Preparing", "History")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)
    val stallId = sharedPrefs.getInt("OWNER_STALL_ID", -1)
    val ownerUserIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val ownerUserId = ownerUserIdStr.toIntOrNull() ?: -1

    var isLoading by remember { mutableStateOf(true) }
    var liveOrders by remember { mutableStateOf<List<OwnerOrder>>(emptyList()) }

    var isAccountLocked by remember { mutableStateOf(false) }
    var refundOwed by remember { mutableStateOf(0.0) }

    // UTR Dialog State
    var showRefundDialog by remember { mutableStateOf(false) }

    fun fetchLiveOrders() {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getLiveOrders(FetchOwnerOrdersRequest(stallId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    liveOrders = response.body()?.orders ?: emptyList()
                    isAccountLocked = response.body()?.is_account_locked ?: false
                    refundOwed = response.body()?.refund_owed ?: 0.0
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(stallId) {
        if (stallId != -1) fetchLiveOrders()
    }

    // --- PHASE 4: UTR PROOF DIALOG ---
    if (showRefundDialog) {
        var utrNumber by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRefundDialog = false },
            title = { Text("Clear Refund Lock", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Column {
                    Text("Check your History tab for the canceled order. Send ₹$refundOwed to the user's phone number via GPay/PhonePe.", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = { utrNumber = it },
                        label = { Text("Enter UPI Transaction ID (UTR)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = utrNumber.length >= 8 && !isSubmitting,
                    onClick = {
                        isSubmitting = true
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.instance.clearRefund(ClearRefundRequest(stallId, utrNumber))
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, "Account Unlocked!", Toast.LENGTH_LONG).show()
                                    showRefundDialog = false
                                    fetchLiveOrders() // Refresh dashboard instantly
                                } else {
                                    Toast.makeText(context, res.body()?.message ?: "Error clearing refund", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text("Submit Proof", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Text("Orders & Dispatch", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(16.dp))

        if (isAccountLocked) {
            Surface(
                color = Color(0xFFD32F2F),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚠️ ACCOUNT LOCKED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text("You owe a refund of ₹$refundOwed to a user for a canceled order.", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { showRefundDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Clear Lock", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = MaterialTheme.colorScheme.primary)
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index, onClick = { selectedTabIndex = index },
                    text = { Text(text = title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.Gray) }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else {
            when (selectedTabIndex) {
                0 -> OrderListTab(liveOrders.filter { it.status == "Pending" }, "No new orders.", ownerUserId, isAccountLocked, { fetchLiveOrders() })
                1 -> OrderListTab(liveOrders.filter { it.status == "Preparing" }, "No orders preparing.", ownerUserId, isAccountLocked, { fetchLiveOrders() })
                2 -> HistoryAnalyticsList(stallId = stallId)
            }
        }
    }
}

@Composable
fun OrderListTab(orders: List<OwnerOrder>, emptyMessage: String, ownerUserId: Int, isAccountLocked: Boolean, refreshFeed: () -> Unit) {
    val context = LocalContext.current
    var showApproveDialog by remember { mutableStateOf(false) }
    var showManualAssignDialog by remember { mutableStateOf(false) }
    var selectedOrderIdForAction by remember { mutableStateOf<Int?>(null) }
    var localOrders by remember(orders) { mutableStateOf(orders) }

    if (localOrders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(emptyMessage, color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(localOrders, key = { it.order_id }) { order ->
                AdvancedLiveOrderCard(
                    order = order,
                    isAccountLocked = isAccountLocked,
                    onApproveClick = {
                        if (isAccountLocked) Toast.makeText(context, "Clear your refund due to accept new orders!", Toast.LENGTH_LONG).show()
                        else { selectedOrderIdForAction = order.order_id; showApproveDialog = true }
                    },
                    onManualAssignClick = { selectedOrderIdForAction = order.order_id; showManualAssignDialog = true },
                    onRemoveFromUI = { localOrders = localOrders.filter { it.order_id != order.order_id }; refreshFeed() }
                )
            }
        }
    }

    if (showApproveDialog && selectedOrderIdForAction != null) {
        AutoApproveDialog(orderId = selectedOrderIdForAction!!, onDismiss = { showApproveDialog = false }, onSuccess = { msg ->
            showApproveDialog = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show(); refreshFeed()
        })
    }

    if (showManualAssignDialog && selectedOrderIdForAction != null) {
        ManualAssignDialog(ownerId = ownerUserId, orderId = selectedOrderIdForAction!!, onDismiss = { showManualAssignDialog = false }, onSuccess = { msg ->
            showManualAssignDialog = false; Toast.makeText(context, msg, Toast.LENGTH_LONG).show(); refreshFeed()
        })
    }
}

@Composable
fun AdvancedLiveOrderCard(order: OwnerOrder, isAccountLocked: Boolean, onApproveClick: () -> Unit, onManualAssignClick: () -> Unit, onRemoveFromUI: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun handleReject() {
        isProcessing = true
        coroutineScope.launch {
            try {
                if (RetrofitClient.instance.rejectOrder(RejectOrderRequest(order.order_id)).isSuccessful) { Toast.makeText(context, "Order Rejected", Toast.LENGTH_SHORT).show(); onRemoveFromUI() }
            } catch (e: Exception) {} finally { isProcessing = false }
        }
    }

    fun handleDispatch() {
        isProcessing = true
        coroutineScope.launch {
            try {
                if (RetrofitClient.instance.dispatchOrder(DispatchOrderRequest(order.order_id)).isSuccessful) { Toast.makeText(context, "Order Dispatched!", Toast.LENGTH_SHORT).show(); onRemoveFromUI() }
            } catch (e: Exception) {} finally { isProcessing = false }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).shadow(6.dp, RoundedCornerShape(16.dp)).clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFFFCC80))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order #${order.order_id}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(order.customer_name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${order.total_amount}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) "Hide Details" else "View Details", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "Expand", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFFFF3E0), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Location", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(order.delivery_address, fontSize = 13.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (order.order_type == "WhatsApp") Icons.Filled.ChatBubbleOutline else Icons.Filled.PersonOutline, contentDescription = "User", tint = if (order.order_type == "WhatsApp") Color(0xFF25D366) else Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phone: ${order.customer_phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(order.items_summary, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                if (!order.special_request.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Instruction", tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chef Note: ${order.special_request}", color = Color(0xFFE65100), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // --- DYNAMIC BUTTONS ---
                if (order.status == "Pending") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { handleReject() }, enabled = !isProcessing, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f)), modifier = Modifier.weight(1f)) { Text("Reject", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { onApproveClick() },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isAccountLocked) Color.LightGray else Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isAccountLocked) "Locked" else "Approve", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                } else if (order.status == "Preparing") {
                    val isStaffMissing = order.staff_name.isNullOrEmpty() || order.staff_name.equals("Not Assigned", ignoreCase = true) || order.staff_name == "null"

                    if (isStaffMissing) {
                        Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("⚠️ No Staff Assigned!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                    Text("Auto-dispatch was busy. Please assign manually.", fontSize = 12.sp, color = Color(0xFFD32F2F).copy(alpha = 0.8f))
                                }
                            }
                        }
                        Button(onClick = { onManualAssignClick() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("Assign Staff First", color = Color.White, fontWeight = FontWeight.Bold) }
                    } else {
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DeliveryDining, contentDescription = "Staff", tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Assigned: ${order.staff_name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text("Code: ${order.verification_code} | ${if (order.payment_status == "Paid") "Paid Online" else "Collect Cash"}", fontSize = 12.sp, color = Color(0xFF2E7D32).copy(alpha = 0.8f))
                                }
                            }
                        }
                        Button(onClick = { handleDispatch() }, enabled = !isProcessing, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Product Ready - Dispatch Order", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun AutoApproveDialog(orderId: Int, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isApproving by remember { mutableStateOf(false) }
    var isPaid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Approve Order", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Auto-dispatch will find the best available delivery partner.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isPaid, onCheckedChange = { isPaid = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50)))
                        Column { Text("Customer Paid Online", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground); Text("Uncheck to collect Handcash.", fontSize = 10.sp, color = Color.Gray) }
                    }
                }
                if (isApproving) { Spacer(modifier = Modifier.height(16.dp)); CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                isApproving = true
                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.instance.approveOrder(ApproveOrderRequest(orderId, "", isPaid))
                        if (response.isSuccessful) onSuccess(response.body()?.message ?: "Success")
                    } catch (e: Exception) {} finally { isApproving = false }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Approve & Auto-Assign", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }, containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ManualAssignDialog(ownerId: Int, orderId: Int, onDismiss: () -> Unit, onSuccess: (String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isApproving by remember { mutableStateOf(false) }
    var freeStaffList by remember { mutableStateOf<List<StaffMember>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getFreeStaff(FetchStaffRequest(owner_id = ownerId))
                if (response.isSuccessful && response.body()?.status == "success") freeStaffList = response.body()?.staff ?: emptyList()
                else errorMessage = response.body()?.message ?: "No free staff found."
            } catch (e: Exception) { errorMessage = "Network Error" } finally { isLoading = false }
        }
    }

    fun pickStaff(staffId: String) {
        isApproving = true
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.approveOrder(ApproveOrderRequest(orderId, staffId, false))
                if (response.isSuccessful) onSuccess(response.body()?.message ?: "Success")
            } catch (e: Exception) {} finally { isApproving = false }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Manual Staff Assignment", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Select an available delivery person:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading || isApproving) { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) } }
                else if (errorMessage != null) { Text(text = errorMessage!!, color = Color.Red, fontWeight = FontWeight.Medium) }
                else {
                    freeStaffList.forEach { staff ->
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF4CAF50).copy(alpha = 0.1f)).clickable { pickStaff(staff.staff_id) }.padding(12.dp)) {
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
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }, containerColor = MaterialTheme.colorScheme.surface
    )
}

// ==========================================
// 3. HISTORY & ANALYTICS TAB
// ==========================================
@Composable
fun HistoryAnalyticsList(stallId: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var filterDate by remember { mutableStateOf<String?>(null) }
    var historyOrders by remember { mutableStateOf<List<OwnerOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchHistory() {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getHistoryOrders(FetchHistoryRequest(stallId, filterDate))
                if (response.isSuccessful && response.body()?.status == "success") {
                    historyOrders = response.body()?.orders ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load history", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(stallId, filterDate) {
        if (stallId != -1) fetchHistory()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (filterDate == null) "All Past History" else "Filtered: $filterDate", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (filterDate != null) {
                    TextButton(onClick = { filterDate = null }) { Text("Clear", color = Color.Red, fontSize = 12.sp) }
                }
                IconButton(onClick = { showOwnerDatePicker(context) { selected -> filterDate = selected } }) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else if (historyOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) { Text("No orders found for this selection.", color = Color.Gray) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(historyOrders, key = { it.order_id }) { order ->
                    HistoryAccordionCard(order = order)
                }
            }
        }
    }
}

@Composable
fun HistoryAccordionCard(order: OwnerOrder) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (order.status) {
        "Canceled" -> Color.Red
        "Out_for_Delivery" -> Color(0xFFFF9800)
        "Delivered" -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).shadow(2.dp, RoundedCornerShape(12.dp)).clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order #${order.order_id}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(order.customer_name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(order.created_at ?: "", fontSize = 10.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(order.status.replace("_", " "), color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) "Less" else "View", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "Expand", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.items_summary, fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.weight(1f))
                    Text("₹${order.total_amount}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonOutline, contentDescription = "Staff", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Staff: ${order.staff_name ?: "Not Assigned"}", fontSize = 12.sp, color = Color.Gray)
                }

                // Show User Phone so the owner can actually send the UPI money!
                if(order.status == "Canceled" && !order.customer_phone.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("User Phone for UPI Refund: ${order.customer_phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OOrdersDashboardPreview() { DialDishTheme { OOrdersDashboard() } }