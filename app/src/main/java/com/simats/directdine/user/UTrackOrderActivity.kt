package com.simats.directdine.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.ui.theme.directdineTheme

class UTrackOrderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                UTrackOrderScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun UTrackOrderScreen(onClose: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F6F8))) {

        // --- 1. Map Background Placeholder ---
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EAED)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Map, contentDescription = "Map", modifier = Modifier.size(80.dp), tint = Color.LightGray)
                Text("Live Route Tracking", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        // --- 2. Top Floating Info Card ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp).shadow(12.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF2196F3), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Navigation, contentDescription = "Nav", tint = Color.White, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("HEADING TO HOSTEL", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("1.2 km • 4 mins away", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        }

        // --- 3. Bottom Sheet (User Perspective) ---
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).shadow(24.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Handle
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color.LightGray, CircleShape).align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(24.dp))

                // Delivery Partner Info
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Ranjithkumar R", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("directdine Delivery Partner", color = Color.Gray, fontSize = 14.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Color(0xFF4CAF50), modifier = Modifier.padding(12.dp))
                        }
                        Surface(shape = CircleShape, color = Color(0xFFE3F2FD), modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Send, contentDescription = "Message", tint = Color(0xFF2196F3), modifier = Modifier.padding(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Amount & Secure OTP Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF8F0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AMOUNT TO PAY:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                            Text("₹250", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE53935))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("SHARE THIS OTP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            // Large display of the OTP
                            Surface(color = Color.White, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp) {
                                Text(
                                    text = "4 9 2 1",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 4.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Informational text
                Text(
                    text = "Please hand over the exact cash and share the OTP above to complete your delivery securely.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UTrackOrderPreview() { directdineTheme { UTrackOrderScreen() } }