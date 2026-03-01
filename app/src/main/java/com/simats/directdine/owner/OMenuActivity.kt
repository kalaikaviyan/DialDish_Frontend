package com.simats.directdine.owner

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.simats.directdine.network.*
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch

data class MenuItemData(
    val id: Int,
    val name: String, val price: String, val type: String, val portion: String, val isSpecial: Boolean, var inStock: Boolean, val imageUrl: String? = null
)

class OMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                Scaffold(bottomBar = { OwnerBottomNavBar(currentSelected = "Menu") }) { innerPadding ->
                    OMenuDashboard(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OMenuDashboard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)

    val stallId = sharedPrefs.getInt("OWNER_STALL_ID", -1)
    val baseUrl = RetrofitClient.BASE_URL

    var showAddProductDialog by remember { mutableStateOf(false) }
    var showEditTimingsDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItemData?>(null) }

    var shopOpenTime by remember { mutableStateOf("Fetching...") }
    var deliveryTime by remember { mutableStateOf("Fetching...") }
    var breakfastTime by remember { mutableStateOf("Fetching...") }
    var lunchTime by remember { mutableStateOf("Fetching...") }
    var eveningTime by remember { mutableStateOf("Fetching...") }
    var dinnerTime by remember { mutableStateOf("Fetching...") }

    val menuItems = remember { mutableStateListOf<MenuItemData>() }

    fun fetchMenu() {
        coroutineScope.launch {
            try {
                val menuResponse = RetrofitClient.instance.getOwnerMenu(FetchOwnerMenuRequest(stallId))
                if (menuResponse.isSuccessful && menuResponse.body()?.status == "success") {
                    val items = menuResponse.body()?.menu ?: emptyList()
                    menuItems.clear()
                    items.forEach { item ->
                        menuItems.add(MenuItemData(item.id, item.name, item.price, item.type, item.portion, item.is_special, item.in_stock, item.image_url))
                    }
                }
            } catch (e: Exception) { }
        }
    }

    LaunchedEffect(stallId) {
        if (stallId != -1) {
            try {
                val response = RetrofitClient.instance.getTimings(GetTimingsRequest(stallId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    val data = response.body()
                    shopOpenTime = data?.shop_open_time ?: "09:00 AM - 10:00 PM"
                    deliveryTime = data?.delivery_time ?: "10:00 AM - 09:30 PM"
                    breakfastTime = data?.breakfast_time ?: "08:00 AM - 11:30 AM"
                    lunchTime = data?.lunch_time ?: "12:00 PM - 03:30 PM"
                    eveningTime = data?.evening_time ?: "04:00 PM - 06:30 PM"
                    dinnerTime = data?.dinner_time ?: "07:00 PM - 10:00 PM"
                }
            } catch (e: Exception) { }
            fetchMenu()
        }
    }

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
                    OutlinedTextField(value = breakfastTime, onValueChange = { breakfastTime = it }, label = { Text("Breakfast") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = lunchTime, onValueChange = { lunchTime = it }, label = { Text("Lunch") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = eveningTime, onValueChange = { eveningTime = it }, label = { Text("Evening") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = dinnerTime, onValueChange = { dinnerTime = it }, label = { Text("Dinner") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        try {
                            val request = UpdateTimingsRequest(stallId, shopOpenTime, deliveryTime, breakfastTime, lunchTime, eveningTime, dinnerTime)
                            val response = RetrofitClient.instance.updateTimings(request)
                            if (response.isSuccessful && response.body()?.status == "success") {
                                Toast.makeText(context, "Timings Updated!", Toast.LENGTH_SHORT).show()
                                showEditTimingsDialog = false
                            }
                        } catch (e: Exception) { }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditTimingsDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddProductDialog || itemToEdit != null) {
        var dishName by remember { mutableStateOf(itemToEdit?.name ?: "") }
        var price by remember { mutableStateOf(itemToEdit?.price ?: "") }
        var isSpecial by remember { mutableStateOf(itemToEdit?.isSpecial ?: false) }
        var portion by remember { mutableStateOf(itemToEdit?.portion ?: "Regular") }

        val selectedMealTypes = remember { mutableStateListOf<String>() }
        if (itemToEdit != null && selectedMealTypes.isEmpty()) {
            selectedMealTypes.addAll(itemToEdit!!.type.split(", "))
        }

        var photoUri by remember { mutableStateOf<Uri?>(null) }
        val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> photoUri = uri }

        fun getBase64Image(): String? {
            return try {
                photoUri?.let { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                }
            } catch (e: Exception) { null }
        }

        Dialog(onDismissRequest = { showAddProductDialog = false; itemToEdit = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (itemToEdit == null) "Add New Product" else "Edit Product", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))

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

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's Special?", fontWeight = FontWeight.Medium)
                        Switch(checked = isSpecial, onCheckedChange = { isSpecial = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Available Slots (Select Multiple)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tiffin", "Lunch", "Evening", "Dinner").forEach { type ->
                            ChoiceChip(
                                text = type, isSelected = selectedMealTypes.contains(type),
                                onClick = { if (selectedMealTypes.contains(type)) selectedMealTypes.remove(type) else selectedMealTypes.add(type) },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Portion / Combo", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Regular", "Combo").forEach { p ->
                            ChoiceChip(text = p, isSelected = portion == p, onClick = { portion = p }, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showAddProductDialog = false; itemToEdit = null }) { Text("Cancel", color = Color.Gray) }
                        Button(onClick = {
                            if (dishName.isNotBlank() && price.isNotBlank() && selectedMealTypes.isNotEmpty()) {
                                val mealTypeString = selectedMealTypes.joinToString(", ")
                                val imageBase64 = getBase64Image()

                                coroutineScope.launch {
                                    try {
                                        if (itemToEdit == null) {
                                            val req = AddMenuItemRequest(stallId, dishName, price, mealTypeString, portion, isSpecial, true, imageBase64)
                                            val response = RetrofitClient.instance.addMenuItem(req)
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "$dishName Added!", Toast.LENGTH_SHORT).show()
                                                fetchMenu()
                                            }
                                        } else {
                                            val req = EditMenuItemRequest(itemToEdit!!.id, dishName, price, mealTypeString, portion, isSpecial, itemToEdit!!.inStock, imageBase64)
                                            val response = RetrofitClient.instance.editMenuItem(req)
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "$dishName Updated!", Toast.LENGTH_SHORT).show()
                                                fetchMenu()
                                            }
                                        }
                                    } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                                    finally { showAddProductDialog = false; itemToEdit = null }
                                }
                            } else { Toast.makeText(context, "Enter name, price, and 1 slot", Toast.LENGTH_SHORT).show() }
                        }) { Text(if (itemToEdit == null) "Save Product" else "Update Changes") }
                    }
                }
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Manage Menu", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                IconButton(onClick = { showAddProductDialog = true }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Item", tint = Color.White)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Store Timings", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                IconButton(onClick = { showEditTimingsDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Timings", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TimingCard(title = "Shop Open", time = shopOpenTime, icon = Icons.Filled.AccessTime)
                TimingCard(title = "Delivery", time = deliveryTime, icon = Icons.Filled.DeliveryDining)
                TimingCard(title = "Breakfast", time = breakfastTime, icon = Icons.Filled.WbTwilight)
                TimingCard(title = "Lunch", time = lunchTime, icon = Icons.Filled.WbSunny)
                TimingCard(title = "Evening", time = eveningTime, icon = Icons.Filled.LocalCafe)
                TimingCard(title = "Dinner", time = dinnerTime, icon = Icons.Filled.NightsStay)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(text = "All-Time Menu", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }

        items(menuItems) { item ->
            MenuItemCard(
                itemData = item,
                baseUrl = baseUrl,
                onEdit = { itemToEdit = item },
                onDelete = {
                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.instance.deleteMenuItem(DeleteMenuItemRequest(item.id))
                            if (response.isSuccessful) {
                                menuItems.remove(item)
                                Toast.makeText(context, "${item.name} Deleted", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) { Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show() }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ChoiceChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
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
fun MenuItemCard(itemData: MenuItemData, baseUrl: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val coroutineScope = rememberCoroutineScope() // <--- ADDED
    var inStock by remember { mutableStateOf(itemData.inStock) }
    val statusColor = if (inStock) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusText = if (inStock) "In Stock" else "Out of Stock"

    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

        val finalImageUrl = if (itemData.imageUrl?.startsWith("uploads/") == true) {
            "$baseUrl${itemData.imageUrl}"
        } else {
            "${baseUrl}uploads/${itemData.imageUrl}"
        }

        if (!itemData.imageUrl.isNullOrEmpty() && itemData.imageUrl != "Fetching...") {
            AsyncImage(
                model = finalImageUrl.trim().replace(" ", "%20"),
                contentDescription = itemData.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                onState = { state ->
                    if (state is coil.compose.AsyncImagePainter.State.Error) {
                        android.util.Log.e("CoilError", "Owner Image failed: $finalImageUrl", state.result.throwable)
                    }
                }
            )
        } else {
            Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFFDF5)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Fastfood, contentDescription = "Food", tint = Color(0xFFD7CCC8).copy(alpha = 0.6f)) }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = itemData.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                if (itemData.isSpecial) { Spacer(modifier = Modifier.width(6.dp)); Icon(Icons.Filled.Star, contentDescription = "Special", tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp)) }
            }
            Text(text = "₹${itemData.price} • ${itemData.type} (${itemData.portion})", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            Text(text = statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
        }

        Column(horizontalAlignment = Alignment.End) {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF44336).copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) }
            }
            Switch(
                checked = inStock,
                onCheckedChange = { newState ->
                    inStock = newState
                    itemData.inStock = newState
                    // <--- ADDED: INSTANT DATABASE SYNC --->
                    coroutineScope.launch {
                        try {
                            RetrofitClient.instance.toggleStock(ToggleStockRequest(itemData.id, newState))
                        } catch (e: Exception) {
                            // Revert visually if API fails
                            inStock = !newState
                            itemData.inStock = !newState
                        }
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
            )
        }
    }
}