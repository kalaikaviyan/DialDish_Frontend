package com.simats.directdine.delivery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.simats.directdine.R
import com.simats.directdine.network.*
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { directdineTheme { DHomeScreen() } }
    }
}

@Composable
fun DHomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)
    val staffId = sharedPrefs.getInt("DELIVERY_STAFF_ID", -1)

    var isOnline by remember { mutableStateOf(false) }
    var otpValue by remember { mutableStateOf("") }
    var activeTask by remember { mutableStateOf<DeliveryTaskResponse?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var driverMarker by remember { mutableStateOf<Marker?>(null) }
    var homeMarker by remember { mutableStateOf<Marker?>(null) }

    var isPinVerified by remember { mutableStateOf(false) }
    var localPaymentStatus by remember { mutableStateOf("") }
    var isDelivered by remember { mutableStateOf(false) }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeliveryDialog by remember { mutableStateOf(false) }
    var showUnreachableDialog by remember { mutableStateOf(false) }

    // --- PHASE 4: ANTI-FRAUD STATE ---
// --- PHASE 4: ANTI-FRAUD STATE ---
    var isArrived by remember { mutableStateOf(false) }
    var waitSeconds by remember { mutableIntStateOf(600) } // 10 Minutes
    var isReturning by remember { mutableStateOf(false) }
    var returningStallName by remember { mutableStateOf("") }
    var returningReason by remember { mutableStateOf("") }

    // Reset state on new task or returning state

    // Reset state on new task or returning state
    LaunchedEffect(activeTask?.order_id, isReturning) {
        if (activeTask != null) {
            isPinVerified = false
            isDelivered = false
            otpValue = ""
            localPaymentStatus = activeTask?.payment_status ?: "Unpaid"
            isArrived = false
            waitSeconds = 600
        } else {
            mapView?.let { map ->
                if (homeMarker != null && map.overlays.contains(homeMarker)) {
                    map.overlays.remove(homeMarker)
                    map.invalidate()
                }
            }
        }
    }

    // Timer Countdown Logic
    LaunchedEffect(isArrived) {
        if (isArrived) {
            while (waitSeconds > 0 && activeTask != null) {
                delay(1000)
                waitSeconds--
            }
        }
    }

    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasLocationPermission = isGranted
        if (!isGranted) isOnline = false
    }

    LaunchedEffect(isOnline, hasLocationPermission) {
        if (isOnline && hasLocationPermission && staffId != -1) {
            while (true) {
                try {
                    locationClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            coroutineScope.launch {
                                try { RetrofitClient.instance.updateLocation(LocationUpdateRequest(staffId, loc.latitude, loc.longitude)) } catch (e: Exception) {}
                            }

                            mapView?.let { map ->
                                val driverPt = GeoPoint(loc.latitude, loc.longitude)
                                driverMarker?.position = driverPt
                                if (driverMarker != null && !map.overlays.contains(driverMarker)) map.overlays.add(driverMarker)
                                map.controller.animateTo(driverPt)
                                map.invalidate()
                            }

                            // --- AUTO-FREE LOGIC WHEN RETURNING ---
                            if (isReturning && activeTask?.stall_lat != null && activeTask?.stall_lng != null) {
                                val driverLoc = android.location.Location("").apply {
                                    latitude = loc.latitude
                                    longitude = loc.longitude
                                }
                                val stallLoc = android.location.Location("").apply {
                                    latitude = activeTask!!.stall_lat!!.toDouble()
                                    longitude = activeTask!!.stall_lng!!.toDouble()
                                }
                                // If driver is within 50 meters of the stall
                                if (driverLoc.distanceTo(stallLoc) < 50f) {
                                    coroutineScope.launch {
                                        try {
                                            val resp = RetrofitClient.instance.confirmReturnToStall(ConfirmReturnRequest(staffId))
                                            if(resp.isSuccessful) {
                                                isReturning = false
                                                activeTask = null
                                            }
                                        } catch(e:Exception){}
                                    }
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {}

                // Polling for Task OR Returning Status

                // Polling for Task OR Returning Status
                try {
                    val response = RetrofitClient.instance.getDeliveryTask(GetTaskRequest(staffId))
                    if (response.isSuccessful) {
                        val resBody = response.body()
                        if (resBody?.status == "success") {
                            activeTask = resBody
                            isReturning = false
                        } else if (resBody?.status == "returning") {
                            isReturning = true
                            returningStallName = resBody.stall_name ?: "Restaurant"
                            returningReason = resBody.return_reason ?: "Canceled"
                            activeTask = null
                        } else {
                            activeTask = null
                            isReturning = false
                        }
                    }
                } catch (e: Exception) {}

                // Polling for Map Tracker Data (Only if we have a task)
                if (activeTask != null) {
                    try {
                        val trackRes = RetrofitClient.instance.getTrackingData(TrackingRequest(activeTask!!.order_id!!))
                        if (trackRes.isSuccessful && trackRes.body()?.status == "success") {
                            val data = trackRes.body()!!
                            if (data.user_lat != null && data.user_lng != null) {
                                mapView?.let { map ->
                                    val homePt = GeoPoint(data.user_lat.toDouble(), data.user_lng.toDouble())
                                    homeMarker?.position = homePt
                                    if (homeMarker != null && !map.overlays.contains(homeMarker)) map.overlays.add(homeMarker)
                                    map.invalidate()
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }

                delay(5000)
            }
        }
    }

    Scaffold(bottomBar = { DeliveryBottomNav(currentSelection = 0, context = context) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF5F5F5))) {

            Box(modifier = Modifier.weight(if (activeTask != null || isReturning) 1f else 1.3f).fillMaxWidth()) {

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                        Configuration.getInstance().userAgentValue = ctx.packageName

                        val newMap = MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(18.0)
                        }

                        val dMarker = Marker(newMap).apply {
                            title = "Me"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.isometric_bike_3d)
                        }

                        val hMarker = Marker(newMap).apply {
                            title = "Delivery Destination"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = ContextCompat.getDrawable(context, R.drawable.isometric_home_3d)
                        }

                        driverMarker = dMarker
                        homeMarker = hMarker
                        mapView = newMap

                        newMap
                    }
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(30.dp), color = Color.White, modifier = Modifier.shadow(6.dp, RoundedCornerShape(30.dp))) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isOnline) Color(0xFF4CAF50) else Color.Red))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isOnline) "ONLINE" else "OFFLINE", fontWeight = FontWeight.ExtraBold, color = if (isOnline) Color(0xFF4CAF50) else Color.Red, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isOnline,
                                onCheckedChange = {
                                    if (it && !hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    else isOnline = it
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp).shadow(6.dp, CircleShape),
                        onClick = { context.startActivity(Intent(context, DSettingsActivity::class.java)) }
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.DarkGray, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // --- UI 1: RETURNING TO STALL MODE ---
            // --- UI 1: RETURNING TO STALL MODE ---
            if (isOnline && isReturning) {
                val isSuccess = returningReason == "Delivered"
                val bgColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                val iconColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF9800)
                val mainTextColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100)
                val buttonColor = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFE65100)
                val iconImage = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber
                val titleText = if (isSuccess) "Delivery Complete!" else "Order Canceled!"
                val subText = if (isSuccess) "Please return your delivery bag to:" else "Please return the food to:"

                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = bgColor
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(iconImage, contentDescription = "Status", tint = iconColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(titleText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = mainTextColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(subText, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(returningStallName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isVerifying = true
                                coroutineScope.launch {
                                    try {
                                        val response = RetrofitClient.instance.confirmReturnToStall(ConfirmReturnRequest(staffId))
                                        if (response.isSuccessful) {
                                            isReturning = false
                                            Toast.makeText(context, "You are now Free for new orders!", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {} finally { isVerifying = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isVerifying
                        ) {
                            if (isVerifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Confirm Returned to Stall", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
            // --- UI 2: ACTIVE ORDER MODE ---
            else if (isOnline && activeTask != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = Color.White
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(if (activeTask!!.order_status == "Preparing") "GO TO RESTAURANT:" else "DELIVER TO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(if (activeTask!!.order_status == "Preparing") activeTask!!.stall_name!! else activeTask!!.delivery_address!!, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(activeTask!!.customer_name ?: "User", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Order #${activeTask!!.order_id}", color = Color.Gray, fontSize = 14.sp)
                            }
                            Surface(shape = CircleShape, color = Color(0xFFFFF3E0), modifier = Modifier.size(48.dp), onClick = { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${activeTask!!.customer_phone}") }) }) {
                                Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Color(0xFFFF8C00), modifier = Modifier.padding(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- ARRIVAL & PIN LOGIC ---
                        // --- ARRIVAL & PIN LOGIC ---
                        if (activeTask!!.order_status == "Out_for_Delivery" && !isArrived) {
                            Button(
                                onClick = { isArrived = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("I Have Arrived At Location", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                        else if (activeTask!!.order_status == "Out_for_Delivery" && isArrived) {

                            // TOP ROW: P and D INDICATORS
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                val pColor by animateColorAsState(
                                    targetValue = if (!isPinVerified) Color.LightGray else if (localPaymentStatus == "Paid") Color(0xFF4CAF50) else Color.Red
                                )
                                Box(modifier = Modifier.size(48.dp).background(pColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("P", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))

                                val dColor by animateColorAsState(targetValue = if (isDelivered) Color(0xFF4CAF50) else Color.LightGray)
                                Box(modifier = Modifier.size(48.dp).background(dColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                    Text("D", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // BOTTOM ROW: ACTIONS based on state
                            if (!isPinVerified) {
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF5F5), border = BorderStroke(1.dp, Color(0xFFFFEBEE)), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ENTER 4-DIGIT PIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            BasicTextField(
                                                value = otpValue, onValueChange = { if (it.length <= 4) otpValue = it },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                decorationBox = {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        repeat(4) { index ->
                                                            Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                                                Text(text = if (index < otpValue.length) otpValue[index].toString() else "", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                isVerifying = true
                                                coroutineScope.launch {
                                                    try {
                                                        val response = RetrofitClient.instance.checkPin(VerifyCodeRequest(activeTask!!.order_id!!, "P-$otpValue"))
                                                        if (response.isSuccessful && response.body()?.status == "success") {
                                                            isPinVerified = true
                                                            localPaymentStatus = response.body()?.payment_status ?: "Unpaid"
                                                        } else { Toast.makeText(context, "Invalid PIN!", Toast.LENGTH_SHORT).show() }
                                                    } catch (e: Exception) {} finally { isVerifying = false }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (otpValue.length == 4) Color(0xFFFF8C00) else Color.LightGray),
                                            shape = RoundedCornerShape(12.dp), enabled = otpValue.length == 4 && !isVerifying
                                        ) { Text("Verify", color = Color.White) }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { showUnreachableDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), enabled = waitSeconds <= 0
                                ) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = if (waitSeconds <= 0) Color.Red else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (waitSeconds > 0) "User Unreachable (Wait ${waitSeconds / 60}:${String.format("%02d", waitSeconds % 60)})" else "User Unreachable - Cancel", color = if (waitSeconds <= 0) Color.Red else Color.Gray)
                                }
                            } else {
                                // PIN IS VERIFIED. Show Payment OR Delivery based on payment status.
                                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF9F9F9), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(if (localPaymentStatus == "Paid") "ONLINE PAID" else "COLLECT HANDCASH:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (localPaymentStatus == "Paid") Color(0xFF4CAF50) else Color.Red)
                                            Text("₹${activeTask!!.total_amount}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (localPaymentStatus == "Paid") Color(0xFF4CAF50) else Color.Red)
                                        }
                                        if (localPaymentStatus == "Unpaid") {
                                            Button(onClick = { showPaymentDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(12.dp)) { Text("Mark Paid", color = Color.White) }
                                        } else {
                                            Button(onClick = { showDeliveryDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), shape = RoundedCornerShape(12.dp)) { Text("Finish Delivery", color = Color.White) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // DIALOGS
            if (showPaymentDialog) {
                AlertDialog(
                    onDismissRequest = { showPaymentDialog = false },
                    title = { Text("Confirm Payment", fontWeight = FontWeight.Bold) },
                    text = { Text("Did you collect ₹${activeTask?.total_amount} via Cash/UPI from the user?") },
                    confirmButton = { TextButton(onClick = { localPaymentStatus = "Paid"; showPaymentDialog = false }) { Text("Yes, Collected", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel", color = Color.Gray) } }
                )
            }

            if (showDeliveryDialog) {
                AlertDialog(
                    onDismissRequest = { showDeliveryDialog = false },
                    title = { Text("Complete Delivery", fontWeight = FontWeight.Bold) },
                    text = { Text("Hand over the food. Confirm order is delivered?") },
                    confirmButton = {
                        Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), onClick = {
                            showDeliveryDialog = false
                            coroutineScope.launch {
                                try {
                                    val response = RetrofitClient.instance.completeDelivery(ConfirmDeliveryRequest(activeTask!!.order_id!!))
                                    if (response.isSuccessful) {
                                        isDelivered = true; Toast.makeText(context, "Delivery Complete!", Toast.LENGTH_SHORT).show()
                                        delay(1500); activeTask = null
                                    }
                                } catch (e: Exception) {}
                            }
                        }) { Text("Confirm", color = Color.White) }
                    },
                    dismissButton = { TextButton(onClick = { showDeliveryDialog = false }) { Text("Cancel", color = Color.Gray) } }
                )
            }

            if (showUnreachableDialog) {
                AlertDialog(
                    onDismissRequest = { showUnreachableDialog = false },
                    title = { Text("User Unreachable?", fontWeight = FontWeight.Bold, color = Color.Red) },
                    text = { Text("The user will be issued a fraud strike, and you will be directed back to the stall to return the food.") },
                    confirmButton = {
                        Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red), onClick = {
                            showUnreachableDialog = false
                            coroutineScope.launch {
                                try {
                                    val response = RetrofitClient.instance.unreachableCancel(ConfirmDeliveryRequest(activeTask!!.order_id!!))
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Order Canceled. Strike Issued.", Toast.LENGTH_LONG).show()
                                        activeTask = null // This triggers the UI to check for "Returning" mode
                                    }
                                } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                            }
                        }) { Text("Yes, Cancel Order", color = Color.White) }
                    },
                    dismissButton = { TextButton(onClick = { showUnreachableDialog = false }) { Text("Cancel", color = Color.Gray) } }
                )
            }
        }
    }
}