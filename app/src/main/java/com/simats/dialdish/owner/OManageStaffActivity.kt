package com.simats.dialdish.owner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

// Temporary Mock Data Class to run the UI
data class Staff(val id: String, var name: String, var phone: String, var email: String, val totalCashToday: Int)

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
                            title = { Text("Manage Staff & Settlement", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    finish()
                                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
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

    // Mock Database List (Using mutableStateListOf so the UI updates when we delete someone)
    val staffList = remember {
        mutableStateListOf(
            Staff("AD3214", "Ramesh Kumar", "9876543210", "ramesh@email.com", 1550),
            Staff("AD3215", "Suresh Singh", "8765432109", "suresh@email.com", 840)
        )
    }

    // States for the Dialogs
    var staffToEdit by remember { mutableStateOf<Staff?>(null) }
    var staffToDelete by remember { mutableStateOf<Staff?>(null) }
    var staffToSettle by remember { mutableStateOf<Staff?>(null) }

    // ==========================================
    // 1. DELETE CAUTION DIALOG
    // ==========================================
    if (staffToDelete != null) {
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = { Text("Delete Staff Record?", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) },
            text = { Text("CAUTION: This will permanently delete ${staffToDelete!!.name} (${staffToDelete!!.id}) and their entire delivery history from the database. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        staffList.remove(staffToDelete) // UI removal (Later, trigger PHP API here)
                        Toast.makeText(context, "${staffToDelete!!.name} Deleted", Toast.LENGTH_SHORT).show()
                        staffToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Confirm Delete") }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // ==========================================
    // 2. EDIT STAFF DIALOG
    // ==========================================
    if (staffToEdit != null) {
        var editPhone by remember { mutableStateOf(staffToEdit!!.phone) }
        var editEmail by remember { mutableStateOf(staffToEdit!!.email) }

        AlertDialog(
            onDismissRequest = { staffToEdit = null },
            title = { Text("Edit ${staffToEdit!!.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Update contact details below:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = editPhone, onValueChange = { editPhone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = editEmail, onValueChange = { editEmail = it }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { Toast.makeText(context, "Select New Photo (Mock)", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Photo", tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Photo", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    Toast.makeText(context, "Details Updated in Database!", Toast.LENGTH_SHORT).show()
                    staffToEdit = null
                }) { Text("Save Changes") }
            },
            dismissButton = {
                TextButton(onClick = { staffToEdit = null }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // ==========================================
    // 3. EOD SETTLEMENT DIALOG (The Calculator)
    // ==========================================
    if (staffToSettle != null) {
        var petrolDeduction by remember { mutableStateOf("") }
        val petrolInt = petrolDeduction.toIntOrNull() ?: 0
        val finalAmount = staffToSettle!!.totalCashToday - petrolInt

        AlertDialog(
            onDismissRequest = { staffToSettle = null },
            title = { Text("End of Day Settlement", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Calculating settlement for ${staffToSettle!!.name} for today's deliveries.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total COD Collected:", fontWeight = FontWeight.Medium)
                        Text("₹${staffToSettle!!.totalCashToday}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = petrolDeduction,
                        onValueChange = { petrolDeduction = it },
                        label = { Text("Petrol/Expense Deduction (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.LocalGasStation, contentDescription = "Gas") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Amount to Handover:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹$finalAmount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    Toast.makeText(context, "Ledger Locked. Settlement Saved!", Toast.LENGTH_LONG).show()
                    staffToSettle = null
                }) { Text("Confirm Settlement") }
            },
            dismissButton = {
                TextButton(onClick = { staffToSettle = null }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // ==========================================
    // MAIN LIST LAYOUT
    // ==========================================
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        // Simple Top Filter for Date
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Showing data for:", fontSize = 14.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { Toast.makeText(context, "Opening Date Picker...", Toast.LENGTH_SHORT).show() }) {
                Text(text = "Today (Current)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(staffList) { staff ->
                StaffManagementCard(
                    staff = staff,
                    onEdit = { staffToEdit = staff },
                    onDelete = { staffToDelete = staff },
                    onSettle = { staffToSettle = staff }
                )
            }
        }
    }
}

@Composable
fun StaffManagementCard(staff: Staff, onEdit: () -> Unit, onDelete: () -> Unit, onSettle: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Column {
            // Header: Photo, Name, ID
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = staff.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(text = "ID: ${staff.id}  •  ${staff.phone}", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Actions Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).size(40.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.background(Color(0xFFF44336).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).size(40.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF44336), modifier = Modifier.size(20.dp))
                    }
                }

                Button(onClick = onSettle, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Icon(Icons.Filled.Calculate, contentDescription = "Calculate", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settle Cash", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManageStaffScreenPreview() {
    DialDishTheme { ManageStaffScreen() }
}