package com.simats.dialdish.user

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.simats.dialdish.map.OSMMapPickerActivity
import com.simats.dialdish.network.*
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.launch

class UAddressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DialDishTheme {
                UAddressScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UAddressScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)

    val userIdStr = sharedPrefs.getString("LOGGED_IN_USER_ID", "-1") ?: "-1"
    val userId = userIdStr.toIntOrNull() ?: -1

    var addressList by remember { mutableStateOf<List<AddressItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    fun fetchAddresses() {
        isLoading = true
        coroutineScope.launch {
            try {
                val res = RetrofitClient.instance.getAddresses(UserIdRequest(userId))
                if (res.isSuccessful && res.body()?.status == "success") {
                    addressList = res.body()?.addresses ?: emptyList()

                    val defaultAddr = addressList.find { it.is_default }
                    if (defaultAddr != null) {
                        sharedPrefs.edit().putString("SAVED_ADDRESS_STRING", defaultAddr.address_text).apply()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId != -1) fetchAddresses()
    }

    fun setDefault(address: AddressItem) {
        coroutineScope.launch {
            try {
                val res = RetrofitClient.instance.setDefaultAddress(SetDefaultAddressRequest(userId, address.id))
                if (res.isSuccessful) {
                    sharedPrefs.edit().putString("SAVED_ADDRESS_STRING", address.address_text).apply()
                    fetchAddresses()
                }
            } catch (e: Exception) { }
        }
    }

    fun deleteAddress(addressId: Int) {
        coroutineScope.launch {
            try {
                val res = RetrofitClient.instance.deleteAddress(DeleteAddressRequest(addressId))
                if (res.isSuccessful) {
                    Toast.makeText(context, "Address Deleted", Toast.LENGTH_SHORT).show()
                    fetchAddresses()
                }
            } catch (e: Exception) { }
        }
    }

    if (showAddDialog) {
        AddAddressDialog(userId = userId, onDismiss = { showAddDialog = false }, onSuccess = { fetchAddresses() })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Addresses", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFAFAFA))
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFF57C00),
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add") },
                text = { Text("Add New Address", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF57C00)) }
        } else if (addressList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LocationOff, contentDescription = "No Address", tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No saved addresses found.", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFAFAFA))
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }

                items(addressList) { address ->
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(if (address.is_default) 6.dp else 2.dp, RoundedCornerShape(16.dp)).clickable { setDefault(address) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = if (address.is_default) BorderStroke(1.5.dp, Color(0xFFF57C00)) else BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = address.is_default,
                                onClick = { setDefault(address) },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF57C00), unselectedColor = Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = "Pin", tint = if(address.is_default) Color(0xFFF57C00) else Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(address.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                    if (address.is_default) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(6.dp)) {
                                            Text("DEFAULT", color = Color(0xFFE65100), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(address.address_text, color = Color.DarkGray, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                            IconButton(onClick = { deleteAddress(address.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFE53935))
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun AddAddressDialog(userId: Int, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var addressDetails by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf<String?>(null) }
    var lng by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val mapPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lat = result.data?.getStringExtra("LATITUDE")
            lng = result.data?.getStringExtra("LONGITUDE")
            if (lat != null) Toast.makeText(context, "Map Location Pinned!", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lat = location.latitude.toString()
                    lng = location.longitude.toString()
                    Toast.makeText(context, "GPS Location Captured!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun fetchGPS() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lat = location.latitude.toString()
                    lng = location.longitude.toString()
                    Toast.makeText(context, "GPS Location Captured!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("Add New Address", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pinpoint your location for precise delivery.", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color(0xFFE3F2FD), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF2196F3)),
                        modifier = Modifier.weight(1f).clickable { fetchGPS() }.height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "GPS", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use GPS", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), fontSize = 13.sp)
                        }
                    }
                    Surface(
                        color = Color(0xFFFFF3E0), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFF57C00)),
                        modifier = Modifier.weight(1f).clickable { mapPickerLauncher.launch(Intent(context, OSMMapPickerActivity::class.java)) }.height(48.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Map, contentDescription = "Map", tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick on Map", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 13.sp)
                        }
                    }
                }

                if (lat != null && lng != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Locked", tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Coordinates Locked!", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("⚠️ Please select a location method above.", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, label = { Text("Save As (e.g. Hostel, Dept)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF57C00), unfocusedBorderColor = Color(0xFFE0E0E0))
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = addressDetails, onValueChange = { addressDetails = it }, label = { Text("Block, Room No, Details") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF57C00), unfocusedBorderColor = Color(0xFFE0E0E0))
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold) }
                    Button(
                        enabled = !isSaving && lat != null && title.isNotBlank() && addressDetails.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            isSaving = true
                            coroutineScope.launch {
                                try {
                                    val res = RetrofitClient.instance.addAddress(AddAddressRequest(userId, title, addressDetails, lat, lng))
                                    if (res.isSuccessful) {
                                        Toast.makeText(context, "Address Saved Successfully!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                        onDismiss()
                                    }
                                } catch (e: Exception) { Toast.makeText(context, "Error saving address", Toast.LENGTH_SHORT).show() }
                                finally { isSaving = false }
                            }
                        }
                    ) { if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp)) else Text("Save Address", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun UAddressPreview() { DialDishTheme { UAddressScreen() } }