package com.simats.dialdish.user

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.compose.ui.window.Dialog
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.location.LocationServices
import com.simats.dialdish.BuildConfig
import com.simats.dialdish.map.OSMMapPickerActivity
import com.simats.dialdish.network.RetrofitClient
import com.simats.dialdish.network.Stall
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                UHomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UHomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userName = sharedPrefs.getString("LOGGED_IN_USER_NAME", "User") ?: "User"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 0..11 -> "Rise & Shine,"
        in 12..15 -> "Lunch O'clock,"
        in 16..19 -> "Evening Cravings,"
        else -> "Midnight Munchies,"
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Open Now") }
    val filters = listOf("Top Rated", "Nearby", "Open Now")

    var stallsList by remember { mutableStateOf<List<Stall>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showLocationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val savedDefaultAddress = sharedPrefs.getString("SAVED_ADDRESS_STRING", "") ?: ""
    var currentDisplayLocation by remember { mutableStateOf(if (savedDefaultAddress.isEmpty()) "📍 Select Delivery Address" else savedDefaultAddress) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // --- AI CHAT STATE ---
    var showAiChatDialog by remember { mutableStateOf(false) }

    val mapPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getStringExtra("LATITUDE")?.toDoubleOrNull()
            val lon = result.data?.getStringExtra("LONGITUDE")?.toDoubleOrNull()
            if (lat != null && lon != null) {
                currentDisplayLocation = "Map: %.4f, %.4f".format(lat, lon)
                sharedPrefs.edit().putString("SAVED_ADDRESS_STRING", currentDisplayLocation).apply()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentDisplayLocation = "GPS: %.4f, %.4f".format(location.latitude, location.longitude)
                    sharedPrefs.edit().putString("SAVED_ADDRESS_STRING", currentDisplayLocation).apply()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val freshlySavedAddress = sharedPrefs.getString("SAVED_ADDRESS_STRING", "") ?: ""
        if(freshlySavedAddress.isNotEmpty()) currentDisplayLocation = freshlySavedAddress

        try {
            val response = RetrofitClient.instance.fetchStalls()
            if (response.isSuccessful && response.body()?.status == "success") {
                stallsList = response.body()?.stalls ?: emptyList()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load stalls", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        bottomBar = { UserBottomNav(currentSelection = 0, context = context) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiChatDialog = true },
                containerColor = Color(0xFFF57C00),
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = "AI")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ask AI", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    ) { paddingValues ->

        if (showAiChatDialog) {
            AIChatDialog(onDismiss = { showAiChatDialog = false })
        }

        if (showLocationSheet) {
            ModalBottomSheet(onDismissRequest = { showLocationSheet = false }, sheetState = sheetState, containerColor = Color.White) {
                Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
                    Text("Select Location", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(24.dp))
                    LocationOptionRow(Icons.Filled.MyLocation, "Use Current Location", "Using OpenStreetMap GPS", Color(0xFFF57C00)) {
                        showLocationSheet = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    currentDisplayLocation = "GPS: %.4f, %.4f".format(location.latitude, location.longitude)
                                    sharedPrefs.edit().putString("SAVED_ADDRESS_STRING", currentDisplayLocation).apply()
                                }
                            }
                        } else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                    LocationOptionRow(Icons.Filled.Map, "Search on Map", "Pinpoint on OpenStreetMap", Color.Gray) { showLocationSheet = false; mapPickerLauncher.launch(Intent(context, OSMMapPickerActivity::class.java)) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                    LocationOptionRow(Icons.Filled.Bookmarks, "Saved Addresses", "Pick from your profile", Color.Gray) { showLocationSheet = false; context.startActivity(Intent(context, UAddressActivity::class.java)) }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).padding(paddingValues).padding(horizontal = 24.dp)) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(greeting, fontSize = 16.sp, color = Color.Gray)
                        Text("$userName! 👋", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showLocationSheet = true }.padding(vertical = 4.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Location", tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(currentDisplayLocation, fontSize = 14.sp, color = Color(0xFFF57C00), fontWeight = FontWeight.Medium, maxLines = 1)
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Drop", tint = Color(0xFFF57C00), modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp).shadow(4.dp, CircleShape), onClick = { context.startActivity(Intent(context, USettingsActivity::class.java)) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.DarkGray, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search for restaurants or dishes...", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) }, modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp), textStyle = TextStyle(color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent, focusedTextColor = Color.Black, unfocusedTextColor = Color.Black), singleLine = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filters.size) { index ->
                        val filter = filters[index]
                        val isSelected = selectedFilter == filter
                        Surface(shape = RoundedCornerShape(20.dp), color = if (isSelected) Color(0xFFF57C00) else Color.White, modifier = Modifier.clickable { selectedFilter = filter }.shadow(2.dp, RoundedCornerShape(20.dp))) {
                            Text(text = filter, color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Recommended for you", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading) { Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF57C00).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Text(text = "🍽️", fontSize = 32.sp) } }
            }

            val filteredStalls = stallsList.filter { stall ->
                val matchesSearch = stall.stall_name.contains(searchQuery, ignoreCase = true) || stall.tags.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (selectedFilter) {
                    "Open Now" -> stall.is_open == true
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            if (!isLoading && filteredStalls.isEmpty()) {
                item { Text("No stalls match your current filter.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp)) }
            }

            items(filteredStalls.size) { index ->
                RestaurantCard(filteredStalls[index])
                Spacer(modifier = Modifier.height(16.dp))
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- NEW COMPOSABLE: PREMIUM AI DIETARY ASSISTANT CHAT ---
@Composable
fun AIChatDialog(onDismiss: () -> Unit) {
    var userInput by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("Hi! I'm Di-Di. Ask me about menus, stall timings, or how our delivery system works!") }
    var isThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(28.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFF57C00).copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI", tint = Color(0xFFF57C00), modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Di-Di", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    color = Color(0xFFFAFAFA),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 250.dp)
                ) {
                    // Use a lazy column so long AI responses can scroll
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        item {
                            Text(
                                text = aiResponse,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    placeholder = { Text("e.g., What is Aliyas serving right now?", fontSize = 13.sp, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF57C00),
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFAFA)
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (userInput.isNotBlank()) {
                                isThinking = true
                                val userQuery = userInput
                                userInput = ""
                                aiResponse = "Thinking..."

                                coroutineScope.launch {
                                    try {
                                        val apiKey = BuildConfig.GEMINI_API_KEY

                                        // 1. Fetch the massive DB context from the backend WITH DEBUGGING
                                        val contextResponse = RetrofitClient.instance.getAiContext()
                                        val dbContext = if (contextResponse.isSuccessful) {
                                            val data = contextResponse.body()?.context_string ?: "Database returned empty."
                                            android.util.Log.d("AI_DEBUG", "Data successfully fetched: \n$data")
                                            data
                                        } else {
                                            val errorBody = contextResponse.errorBody()?.string() ?: "Unknown error"
                                            android.util.Log.e("AI_DEBUG", "Backend API Failed! Code: ${contextResponse.code()}, Error: $errorBody")
                                            "Database unavailable. Backend returned error code: ${contextResponse.code()}"
                                        }

                                        // 2. Get the exact current time to pass to the AI
                                        val currentTime = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date())

                                        // Note: Keeping gemini-1.5-flash as it's the stable default, but you can change it back to 2.5 if needed!
                                        val generativeModel = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = apiKey)

                                        // 3. THE MASTER PROMPT
                                        val prompt = """
                                            You are "Di-Di", a highly intelligent and expert AI assistant for the DialDish platform.
                                            DialDish is a large-scale food delivery platform.
                                            
                                            CRITICAL INSTRUCTION: You have access to a live database context provided below. If a user asks about a stall like 'Aliyas', search the context for 'Aliyas'. If you find it, use that data to answer. 
                                            If the context provided is empty or doesn't mention the stall, state: "I'm sorry, I'm currently having trouble reaching the live menu database. Please check the 'Menu' button on the stall card for the latest details."

                                            Here is the current exact time: $currentTime.
                                            
                                            Here is the raw database of all restaurants, their operating timings, and their exact menus:
                                            $dbContext
                                            
                                            Here are the DialDish App Rules:
                                            1. Anti-Fraud System: If a user is unreachable during delivery, the driver waits 10 minutes (600 seconds) before tapping "User Unreachable". The user receives 1 Strike. At 5 Strikes, they are banned.
                                            2. Refunds: If an order is canceled while "Out for Delivery" and the driver has been out for MORE than 30 minutes, the user gets a 100% refund due to delay. Otherwise, it is a 0% refund (food wasted). If canceled while "Preparing", it is a 50% refund.
                                            3. Driver Returns: After every delivery or cancellation, the driver MUST return to the stall. Their app uses GPS to verify they are within 50 meters of the stall before marking them "Free" for the next order.
                                            4. PIN System: The user must provide a 4-digit PIN (e.g., P-1234) to the driver to complete delivery.
                                            
                                            User Request: "$userQuery"
                                            
                                            Instructions:
                                            - Answer the user's question accurately based on the database and rules above.
                                            - If they ask about food, check the current time against the restaurant's timings to see if it is available.
                                            - Be friendly, professional, and concise. Do NOT use markdown bolding (**). Keep it clean plain text.
                                        """.trimIndent()

                                        val response = generativeModel.generateContent(prompt)
                                        withContext(Dispatchers.Main) {
                                            aiResponse = response.text ?: "I couldn't generate a response."
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            aiResponse = "Oops! Connection failed.\nReason: ${e.localizedMessage}"
                                        }
                                    } finally {
                                        isThinking = false
                                    }
                                }
                            }
                        },
                        enabled = !isThinking && userInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isThinking) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Ask AI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationOptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, iconColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) { Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.padding(8.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black); Text(subtitle, color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
fun RestaurantCard(stall: Stall) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF57C00).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🍽️", fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stall.stall_name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(stall.rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray) }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(stall.tags, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        if (!stall.contact_phone.isNullOrEmpty()) { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${stall.contact_phone}") }) } else { Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show() }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.height(36.dp)) { Icon(Icons.Filled.Call, contentDescription = "Call", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(onClick = { val intent = Intent(context, UStallMenuActivity::class.java); intent.putExtra("STALL_ID", stall.stall_id); context.startActivity(intent) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF57C00)), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), modifier = Modifier.height(36.dp)) { Icon(Icons.Filled.MenuBook, contentDescription = "Order", modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Menu", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun UHomeActivityPreview() { DialDishTheme { UHomeScreen() } }