package com.simats.dialdish.owner

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.network.*
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class OManageStaffActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Staff & Settlement", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
                        )
                    }
                ) { innerPadding ->
                    ManageStaffScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ManageStaffScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val ownerUserIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val ownerUserId = ownerUserIdStr.toIntOrNull() ?: -1

    var staffList by remember { mutableStateOf<List<StaffBalanceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog States
    var staffToSettle by remember { mutableStateOf<StaffBalanceItem?>(null) }
    var staffToEdit by remember { mutableStateOf<StaffBalanceItem?>(null) }
    var staffToDelete by remember { mutableStateOf<StaffBalanceItem?>(null) }

    fun fetchStaffData() {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getAllStaffBalances(FetchAllStaffBalancesRequest(ownerUserId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    staffList = response.body()?.staff ?: emptyList()
                } else {
                    Toast.makeText(context, "Failed to load staff", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(ownerUserId) {
        if (ownerUserId != -1) fetchStaffData()
    }

    // ==========================================
    // 1. DELETE STAFF DIALOG (DYNAMIC)
    // ==========================================
    if (staffToDelete != null) {
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if(!isDeleting) staffToDelete = null },
            title = { Text("Delete Delivery Partner?", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently remove ${staffToDelete!!.name}? This will instantly revoke their login access to the Delivery App.") },
            confirmButton = {
                Button(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.instance.deleteStaff(DeleteStaffRequest(staffToDelete!!.id))
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, "Staff Deleted Successfully", Toast.LENGTH_SHORT).show()
                                    staffToDelete = null
                                    fetchStaffData() // Refresh UI list
                                } else {
                                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                            finally { isDeleting = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text(if(isDeleting) "Deleting..." else "Yes, Delete") }
            },
            dismissButton = { TextButton(onClick = { if(!isDeleting) staffToDelete = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color.White
        )
    }

    // ==========================================
    // 2. EDIT STAFF DIALOG (DYNAMIC)
    // ==========================================
    if (staffToEdit != null) {
        var editName by remember { mutableStateOf(staffToEdit!!.name) }
        var editPhone by remember { mutableStateOf(staffToEdit!!.phone) }
        var editEmail by remember { mutableStateOf(staffToEdit!!.email) }
        var base64Image by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        // Image Picker Launcher
        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    base64Image = bytes?.let { b -> android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { if(!isSaving) staffToEdit = null },
            title = { Text("Edit ${staffToEdit!!.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Update contact details and photo below:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone, onValueChange = { editPhone = it },
                        label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editEmail, onValueChange = { editEmail = it },
                        label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (base64Image != null) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (base64Image != null) Icons.Filled.CheckCircle else Icons.Filled.CameraAlt, contentDescription = "Photo", tint = if (base64Image != null) Color(0xFF2E7D32) else Color.DarkGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (base64Image != null) "Photo Selected" else "Update Photo", color = if (base64Image != null) Color(0xFF2E7D32) else Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val res = RetrofitClient.instance.editStaff(EditStaffRequest(staffToEdit!!.id, editName, editPhone, editEmail, base64Image))
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                    staffToEdit = null
                                    fetchStaffData() // Refresh list with new data
                                } else {
                                    Toast.makeText(context, res.body()?.message ?: "Failed to update", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                            finally { isSaving = false }
                        }
                    }
                ) { Text(if(isSaving) "Saving..." else "Save Changes", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { if(!isSaving) staffToEdit = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color.White
        )
    }

    // ==========================================
    // 3. EOD SETTLEMENT DIALOG
    // ==========================================
    if (staffToSettle != null) {
        var expenseReason by remember { mutableStateOf("") }
        var expenseAmountStr by remember { mutableStateOf("") }
        var isSettling by remember { mutableStateOf(false) }

        val expenseAmount = expenseAmountStr.toDoubleOrNull() ?: 0.0
        val finalAmount = staffToSettle!!.active_cash - expenseAmount

        AlertDialog(
            onDismissRequest = { if(!isSettling) staffToSettle = null },
            title = { Text("End of Day Settlement", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Calculating settlement for ${staffToSettle!!.name}.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Unsettled Handcash:", fontWeight = FontWeight.Medium)
                        Text("₹${staffToSettle!!.active_cash}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = expenseReason, onValueChange = { expenseReason = it },
                        label = { Text("Expense Reason (e.g. Petrol)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = expenseAmountStr, onValueChange = { expenseAmountStr = it },
                        label = { Text("Deduction Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net to Collect:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹$finalAmount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSettling,
                    onClick = {
                        isSettling = true
                        coroutineScope.launch {
                            try {
                                val req = SettleCashRequest(ownerUserId, staffToSettle!!.id, expenseReason, expenseAmount)
                                val res = RetrofitClient.instance.settleCash(req)
                                if (res.isSuccessful && res.body()?.status == "success") {
                                    Toast.makeText(context, "Settlement Saved & Ledger Locked!", Toast.LENGTH_LONG).show()
                                    staffToSettle = null
                                    fetchStaffData()
                                } else {
                                    Toast.makeText(context, res.body()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                            finally { isSettling = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text(if(isSettling) "Processing..." else "Confirm & Collect ₹$finalAmount") }
            },
            dismissButton = { TextButton(onClick = { if(!isSettling) staffToSettle = null }) { Text("Cancel", color = Color.Gray) } },
            containerColor = Color.White
        )
    }

    // ==========================================
    // MAIN LIST LAYOUT
    // ==========================================
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Active Handcash Ledgers", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            IconButton(onClick = { fetchStaffData() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        } else if (staffList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No staff found. Please add Delivery Partners.", color = Color.Gray) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(staffList) { staff ->
                    StaffManagementCard(
                        staff = staff,
                        onEdit = { staffToEdit = staff },
                        onDelete = { staffToDelete = staff },
                        onSettle = {
                            if (staff.active_cash > 0) staffToSettle = staff
                            else Toast.makeText(context, "No active cash to settle for ${staff.name}.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StaffManagementCard(staff: StaffBalanceItem, onEdit: () -> Unit, onDelete: () -> Unit, onSettle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = staff.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(text = "ID: ${staff.staff_id_code}  •  ${staff.phone}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                // Quick Balance Display
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${staff.active_cash}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if(staff.active_cash > 0) Color(0xFFE65100) else Color(0xFF4CAF50))
                    Surface(color = if(staff.active_cash > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                        Text(if(staff.active_cash > 0) "PENDING" else "SETTLED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if(staff.active_cash > 0) Color(0xFFE65100) else Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF5F5F5), modifier = Modifier.clickable { onEdit() }) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFFEBEE), modifier = Modifier.clickable { onDelete() }) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }
                }

                Button(
                    onClick = onSettle,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if(staff.active_cash > 0) MaterialTheme.colorScheme.primary else Color.LightGray)
                ) {
                    Icon(Icons.Filled.Calculate, contentDescription = "Calculate", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settle Cash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}