package com.simats.directdine.delivery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.LoginActivity
import com.simats.directdine.ui.theme.directdineTheme

class DSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                DSettingsScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSettingsScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WarningAmber, contentDescription = "Warning", tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Logout", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Are you sure you want to log out? You will stop receiving campus orders until you log back in.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        sharedPrefs.edit().clear().apply()
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("Yes, Log Out") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("INFORMATION", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsNavRow(icon = Icons.Filled.HelpOutline, title = "How to use directdine", onClick = {
                val intent = Intent(context, DInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "HOW_TO")
                context.startActivity(intent)
            })
            SettingsNavRow(icon = Icons.Filled.StarOutline, title = "App Features", onClick = {
                val intent = Intent(context, DInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "FEATURES")
                context.startActivity(intent)
            })
            SettingsNavRow(icon = Icons.Filled.Policy, title = "Privacy Policy & Legal", onClick = {
                val intent = Intent(context, DInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "LEGAL")
                context.startActivity(intent)
            })

            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.padding(24.dp)) {
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF1F0))
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", color = Color(0xFFE53935), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsNavRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = Color.LightGray)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DSettingsActivityPreview() {
    directdineTheme {
        DSettingsScreen()
    }
}