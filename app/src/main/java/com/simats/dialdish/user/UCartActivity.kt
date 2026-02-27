package com.simats.dialdish.user

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.simats.dialdish.map.OSMMapPickerActivity
import com.simats.dialdish.network.PlaceOrderRequest
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch
import java.util.Calendar

// --- GLOBAL CART STATE ---
object CartData {
    var stallId: Int = -1
    var stallName: String = ""
    var stallPhone: String = ""
    var cartItemsText: String = ""
    var cartTotal: Double = 0.0
}

class UCartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                UCartScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UCartScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userId = sharedPrefs.getString("LOGGED_IN_USER_ID", "1") ?: "1"
    val userName = sharedPrefs.getString("LOGGED_IN_USER_NAME", "User") ?: "User"

    val stallId = CartData.stallId
    val stallName = CartData.stallName.ifBlank { "Selected Stall" }
    val stallPhone = CartData.stallPhone
    val cartItems = CartData.cartItemsText.ifBlank { "No items selected" }
    val cartTotal = CartData.cartTotal

    var cookingInstructions by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // EXACT LOCATION STATE
    var deliveryLat by remember { mutableStateOf<String?>(null) }
    var deliveryLng by remember { mutableStateOf<String?>(null) }
    var displayAddress by remember { mutableStateOf("No location selected yet") }

    // --- NEW: LIVE LOCATION FETCHING CLIENT ---
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val mapPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            deliveryLat = result.data?.getStringExtra("LATITUDE")
            deliveryLng = result.data?.getStringExtra("LONGITUDE")
            if (deliveryLat != null && deliveryLng != null) {
                displayAddress = "Location Pinned on Map!"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            try {
                locationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        deliveryLat = loc.latitude.toString()
                        deliveryLng = loc.longitude.toString()
                        displayAddress = "Current GPS Location Captured!"
                        Toast.makeText(context, "Location Found!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Please turn on device GPS", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {}
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        deliveryLat = loc.latitude.toString()
                        deliveryLng = loc.longitude.toString()
                        displayAddress = "Current GPS Location Captured!"
                    } else {
                        Toast.makeText(context, "Please turn on device GPS", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {}
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..15 -> "Good Afternoon"
        in 16..19 -> "Good Evening"
        else -> "Hello"
    }

    fun placeOrder(orderType: String) {
        if (isProcessing) return
        if (cartTotal <= 0) {
            Toast.makeText(context, "Your cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryLat == null || deliveryLng == null) {
            Toast.makeText(context, "Please provide a delivery location!", Toast.LENGTH_LONG).show()
            return
        }

        isProcessing = true

        coroutineScope.launch {
            try {
                val request = PlaceOrderRequest(
                    user_id = userId,
                    stall_id = stallId,
                    items_summary = cartItems,
                    total_amount = cartTotal,
                    delivery_address = displayAddress,
                    delivery_lat = deliveryLat,
                    delivery_lng = deliveryLng,
                    special_request = cookingInstructions,
                    order_type = orderType
                )

                val response = RetrofitClient.instance.placeOrder(request)

                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(context, "Order sent to Stall Dashboard!", Toast.LENGTH_SHORT).show()

                    CartData.cartItemsText = ""
                    CartData.cartTotal = 0.0

                    val ordersIntent = Intent(context, UOrdersActivity::class.java)
                    context.startActivity(ordersIntent)

                    if (orderType == "Call") {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$stallPhone") }
                            context.startActivity(intent)
                        } catch (e: Exception) { Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show() }
                    } else if (orderType == "WhatsApp") {
                        try {
                            val message = """
                                *$greeting, $stallName!* 👋
                                I would like to place an order via DialDish.
                                
                                *Order Details:*
                                $cartItems
                                *Total:* ₹$cartTotal
                                
                                *Instructions:* ${cookingInstructions.ifBlank { "None" }}
                                *Location Pinned on Map!*
                                (Lat: $deliveryLat, Lng: $deliveryLng)
                                
                                Please confirm the order and share payment QR/Details. Thank you!
                            """.trimIndent()

                            val phone = "+91$stallPhone"
                            val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                            (context as ComponentActivity).finish()
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp is not installed on your phone!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to place order.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.background(Color.White).padding(24.dp).shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
                Text("How do you want to place this order?", fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { placeOrder("Call") }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Place Order via Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Call restaurant to confirm and pay", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Icon(Icons.Filled.Phone, contentDescription = "Call")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = { placeOrder("WhatsApp") }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Send Order via WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Send items and address automatically", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "WA")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.15f)).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Your Cart", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("Ordering from $stallName", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- UPDATED LOCATION UI WITH TWO OPTIONS ---
            Text("DELIVERY LOCATION", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Button 1: Live GPS Location
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3)),
                    modifier = Modifier.weight(1f).clickable { fetchCurrentLocation() }.height(48.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "GPS", tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Current", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                    }
                }

                // Button 2: Manual Map Picker
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF8C00)),
                    modifier = Modifier.weight(1f).clickable { mapPickerLauncher.launch(Intent(context, OSMMapPickerActivity::class.java)) }.height(48.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.Map, contentDescription = "Map", tint = Color(0xFFFF8C00), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick on Map", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                    }
                }
            }

            // Location Status Box
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (deliveryLat == null) Color.Transparent else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (deliveryLat == null) Color.Transparent else Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (deliveryLat != null) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Ready", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayAddress, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("CART SUMMARY", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(cartItems, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBackClick, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFFF57C00))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add more items", color = Color(0xFFF57C00), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = cookingInstructions,
                onValueChange = { cookingInstructions = it },
                label = { Text("Cooking Instructions") },
                placeholder = { Text("e.g. Make it extra spicy...", color = Color.LightGray) },
                leadingIcon = { Icon(Icons.Filled.EditNote, contentDescription = "Edit", tint = Color(0xFFF57C00)) },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF57C00), unfocusedBorderColor = Color.LightGray, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("BILL DETAILS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Item Total", color = Color.DarkGray); Text("₹$cartTotal", color = Color.Black) }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Taxes & Charges", color = Color.DarkGray); Text("₹0 (Direct Order)", color = Color(0xFF2E7D32)) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Grand Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black); Text("₹$cartTotal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black) }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}