package com.simats.dialdish.owner

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.simats.dialdish.ui.theme.DialDishTheme

// Temporary Data Model for UI Testing
data class MenuItemData(
    val name: String, val price: String, val type: String, val portion: String, val isSpecial: Boolean, var inStock: Boolean
)

class OMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    bottomBar = { OwnerBottomNavBar(currentSelected = "Menu") }
                ) { innerPadding ->
                    OMenuDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun OMenuDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showEditTimingsDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItemData?>(null) } // NEW: State to track which item is being edited

    // Timings States
    var shopOpenTime by remember { mutableStateOf("09:00 AM - 10:00 PM") }
    var deliveryTime by remember { mutableStateOf("10:00 AM - 09:30 PM") }
    var breakfastTime by remember { mutableStateOf("08:00 AM - 11:30 AM") }
    var lunchTime by remember { mutableStateOf("12:00 PM - 03:30 PM") }
    var dinnerTime by remember { mutableStateOf("07:00 PM - 10:00 PM") }

    // Menu Items State (Dynamic List)
    val menuItems = remember {
        mutableStateListOf(
            MenuItemData("Chicken Dum Biryani", "220", "Lunch", "Regular", true, true),
            MenuItemData("Idli Sambhar", "60", "Tiffin", "Regular", false, false)
        )
    }

    // ==========================================
    // DIALOG 1: EDIT TIMINGS
    // ==========================================
    if (showEditTimingsDialog) {
        AlertDialog(
            onDismissRequest = { showEditTimingsDialog = false },
            title = { Text("Edit Store Timings", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = shopOpenTime, onValueChange = { shopOpenTime = it }, label = { Text("Shop Open") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = deliveryTime, onValueChange = { deliveryTime = it }, label = { Text("Delivery") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = breakfastTime, onValueChange = { breakfastTime = it }, label = { Text("Breakfast (Tiffin)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = lunchTime, onValueChange = { lunchTime = it }, label = { Text("Lunch") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = dinnerTime, onValueChange = { dinnerTime = it }, label = { Text("Dinner") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showEditTimingsDialog = false; Toast.makeText(context, "Timings Updated", Toast.LENGTH_SHORT).show() }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTimingsDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // ==========================================
    // DIALOG 2: ADD NEW PRODUCT
    // ==========================================
    if (showAddProductDialog) {
        var dishName by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var isSpecial by remember { mutableStateOf(false) }
        var mealType by remember { mutableStateOf("Tiffin") }
        var portion by remember { mutableStateOf("Regular") }
        var photoUri by remember { mutableStateOf<Uri?>(null) }
        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> photoUri = uri }

        Dialog(onDismissRequest = { showAddProductDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add New Product", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Upload
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (photoUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Upload", tint = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoUri != null) "Photo Added ✅" else "Upload Dish Image", color = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = dishName, onValueChange = { dishName = it }, label = { Text("Dish Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    // Special Category
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's Special?", fontWeight = FontWeight.Medium)
                        Switch(checked = isSpecial, onCheckedChange = { isSpecial = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Meal Type Chips
                    Text("Meal Type", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tiffin", "Lunch", "Dinner").forEach { type ->
                            ChoiceChip(text = type, isSelected = mealType == type, onClick = { mealType = type }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Portion Chips
                    Text("Portion / Combo", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Regular", "Combo").forEach { p ->
                            ChoiceChip(text = p, isSelected = portion == p, onClick = { portion = p }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel", color = Color.Gray) }
                        Button(onClick = {
                            if (dishName.isNotBlank() && price.isNotBlank()) {
                                menuItems.add(MenuItemData(dishName, price, mealType, portion, isSpecial, true))
                                showAddProductDialog = false
                                Toast.makeText(context, "$dishName Added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter name and price", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Save Product") }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 3: EDIT EXISTING PRODUCT
    // ==========================================
    if (itemToEdit != null) {
        // Pre-fill the states with the selected item's data
        var dishName by remember(itemToEdit) { mutableStateOf(itemToEdit!!.name) }
        var price by remember(itemToEdit) { mutableStateOf(itemToEdit!!.price) }
        var isSpecial by remember(itemToEdit) { mutableStateOf(itemToEdit!!.isSpecial) }
        var mealType by remember(itemToEdit) { mutableStateOf(itemToEdit!!.type) }
        var portion by remember(itemToEdit) { mutableStateOf(itemToEdit!!.portion) }
        var photoUri by remember { mutableStateOf<Uri?>(null) }
        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> photoUri = uri }

        Dialog(onDismissRequest = { itemToEdit = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Edit Product", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Image Upload
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = if (photoUri != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Upload", tint = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoUri != null) "New Photo Added ✅" else "Change Dish Image", color = if (photoUri != null) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(value = dishName, onValueChange = { dishName = it }, label = { Text("Dish Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    // Special Category
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's Special?", fontWeight = FontWeight.Medium)
                        Switch(checked = isSpecial, onCheckedChange = { isSpecial = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Meal Type Chips
                    Text("Meal Type", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tiffin", "Lunch", "Dinner").forEach { type ->
                            ChoiceChip(text = type, isSelected = mealType == type, onClick = { mealType = type }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Portion Chips
                    Text("Portion / Combo", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Regular", "Combo").forEach { p ->
                            ChoiceChip(text = p, isSelected = portion == p, onClick = { portion = p }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { itemToEdit = null }) { Text("Cancel", color = Color.Gray) }
                        Button(onClick = {
                            if (dishName.isNotBlank() && price.isNotBlank()) {
                                // Find the item in the list and replace it with updated data
                                val index = menuItems.indexOf(itemToEdit)
                                if (index != -1) {
                                    menuItems[index] = MenuItemData(dishName, price, mealType, portion, isSpecial, itemToEdit!!.inStock)
                                }
                                itemToEdit = null
                                Toast.makeText(context, "$dishName Updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter name and price", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Save Changes") }
                    }
                }
            }
        }
    }

    // ==========================================
    // MAIN LAYOUT
    // ==========================================
    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {

        // TOP HEADER
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Manage Menu", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(
                    onClick = { showAddProductDialog = true },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Item", tint = Color.White)
                }
            }
        }

        // SECTION 1: TIMINGS CONFIGURATION
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Store Timings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                IconButton(onClick = { showEditTimingsDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Timings", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Horizontal scroll for timings to keep UI clean
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimingCard(title = "Shop Open", time = shopOpenTime, icon = Icons.Filled.AccessTime)
                TimingCard(title = "Delivery", time = deliveryTime, icon = Icons.Filled.DeliveryDining)
                TimingCard(title = "Breakfast", time = breakfastTime, icon = Icons.Filled.WbTwilight)
                TimingCard(title = "Lunch", time = lunchTime, icon = Icons.Filled.WbSunny)
                TimingCard(title = "Dinner", time = dinnerTime, icon = Icons.Filled.NightsStay)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // SECTION 2: ALL-TIME MENU LIST
        item {
            Text(text = "All-Time Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }

        items(menuItems) { item ->
            MenuItemCard(
                itemData = item,
                onEdit = { itemToEdit = item }, // NEW: Actually opens the edit dialog instead of a Toast!
                onDelete = {
                    menuItems.remove(item)
                    Toast.makeText(context, "${item.name} Deleted", Toast.LENGTH_SHORT).show()
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ==========================================
// REUSABLE CUSTOM UI COMPONENTS
// ==========================================
@Composable
fun ChoiceChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TimingCard(title: String, time: String, icon: ImageVector) {
    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = time, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun MenuItemCard(itemData: MenuItemData, onEdit: () -> Unit, onDelete: () -> Unit) {
    var inStock by remember { mutableStateOf(itemData.inStock) }
    val statusColor = if (inStock) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusText = if (inStock) "In Stock" else "Out of Stock"

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        // Food Icon/Placeholder
        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Fastfood, contentDescription = "Food", tint = Color.Gray)
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Item Details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = itemData.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                if (itemData.isSpecial) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Star, contentDescription = "Special", tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                }
            }
            Text(text = "₹${itemData.price} • ${itemData.type} (${itemData.portion})", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            Text(text = statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }

        // Actions: Edit, Delete, and Stock Toggle
        Column(horizontalAlignment = Alignment.End) {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF44336).copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
            Switch(checked = inStock, onCheckedChange = { inStock = it; itemData.inStock = it }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OMenuDashboardPreview() {
    DialDishTheme {
        OMenuDashboard()
    }
}