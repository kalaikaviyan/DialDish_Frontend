package com.simats.directdine.delivery

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.network.GetActiveCashRequest
import com.simats.directdine.network.OrderLogItem
import com.simats.directdine.network.RetrofitClient
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch

class DHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                DHistoryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DHistoryScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)
    val staffId = sharedPrefs.getInt("DELIVERY_STAFF_ID", -1)

    // Dynamic State Variables
    var activeCash by remember { mutableDoubleStateOf(0.0) }
    var totalDeliveries by remember { mutableIntStateOf(0) }
    var orderLog by remember { mutableStateOf<List<OrderLogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchLedger() {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getActiveCash(GetActiveCashRequest(staffId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    activeCash = response.body()?.active_cash ?: 0.0
                    totalDeliveries = response.body()?.total_deliveries ?: 0
                    orderLog = response.body()?.log ?: emptyList()
                } else {
                    Toast.makeText(context, "Failed to load ledger", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(staffId) {
        if (staffId != -1) fetchLedger()
    }

    Scaffold(
        bottomBar = { DeliveryBottomNav(currentSelection = 1, context = context) },
        topBar = {
            TopAppBar(
                title = { Text("Settlement Ledger", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F5), titleContentColor = Color.Black),
                actions = {
                    IconButton(onClick = { fetchLedger() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
                }
            )
        }
    ) { paddingValues ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF8C00))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF5F5F5)).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. FINANCIAL SUMMARY SECTION ---
                item {
                    Text("Today's Summary", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = "Deliveries", tint = Color(0xFF2196F3))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Deliveries", fontSize = 12.sp, color = Color.Gray)
                                Text("$totalDeliveries", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Payable", tint = Color(0xFFFF8C00))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Active Handcash", fontSize = 12.sp, color = Color.Gray)
                                Text("₹$activeCash", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                            }
                        }
                    }
                }

                // --- 2. ETHICAL NOTIFICATION ---
                item {
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Expense deductions (like Petrol) will be negotiated and logged by the Stall Owner during final handover.", fontSize = 12.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }

                // --- 3. CHRONOLOGICAL ORDER LOG ---
                item {
                    Text("Order Log (Recent Deliveries)", fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                }

                if (orderLog.isEmpty()) {
                    item { Text("No completed deliveries yet.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(orderLog) { order ->
                        val isCash = order.method == "Handcash"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = if (isCash) Color(0xFFFFF3E0) else Color(0xFFE3F2FD), modifier = Modifier.size(40.dp)) {
                                        Icon(imageVector = if (isCash) Icons.Filled.Payments else Icons.Filled.CreditCard, contentDescription = "Type", tint = if (isCash) Color(0xFFFF9800) else Color(0xFF2196F3), modifier = Modifier.padding(8.dp))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Order #${order.order_id}", fontWeight = FontWeight.Bold)
                                            if (order.is_settled) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) { Text("SETTLED", color = Color(0xFF4CAF50), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
                                            }
                                        }
                                        Text(order.date, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (isCash) {
                                        Text(if (order.is_settled) "₹${order.amount}" else "+ ₹${order.amount}", fontWeight = FontWeight.Bold, color = if (order.is_settled) Color.Gray else Color(0xFF4CAF50))
                                        Text("Hand Cash", fontSize = 10.sp, color = Color.Gray)
                                    } else {
                                        Text("Prepaid", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                        Text("Online", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}