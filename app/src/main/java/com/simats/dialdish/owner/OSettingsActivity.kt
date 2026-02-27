package com.simats.dialdish.owner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme

class OSettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    OSettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun OSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of your stall account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        val intent = Intent(context, com.simats.dialdish.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Yes, Logout") }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = Color(0xFFF44336), fontWeight = FontWeight.Bold) },
            text = { Text("CAUTION: This action is permanent. All your stall data, menu items, order history, and staff records will be permanently erased. Do you wish to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        Toast.makeText(context, "Account Deleted.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, com.simats.dialdish.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Yes, Delete Forever") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        SettingsSectionTitle("SUPPORT & INFO")
        SettingsCard {
            SettingsClickableRow(icon = Icons.Filled.MenuBook, title = "How to use the App", onClick = {
                val intent = Intent(context, OInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "HOW_TO")
                context.startActivity(intent)
            })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.Star, title = "App Features", onClick = {
                val intent = Intent(context, OInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "FEATURES")
                context.startActivity(intent)
            })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.PrivacyTip, title = "Privacy Policy & Legal", onClick = {
                val intent = Intent(context, OInfoActivity::class.java)
                intent.putExtra("INFO_TYPE", "LEGAL")
                context.startActivity(intent)
            })
        }

        SettingsSectionTitle("ACCOUNT MANAGEMENT")
        SettingsCard {
            SettingsClickableRow(icon = Icons.Filled.Logout, title = "Logout", titleColor = MaterialTheme.colorScheme.primary, onClick = { showLogoutDialog = true })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.DeleteForever, title = "Delete Account", titleColor = Color(0xFFF44336), onClick = { showDeleteDialog = true })
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)) { Column(content = content) }
}

@Composable
fun SettingsClickableRow(icon: ImageVector, title: String, titleColor: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = title, tint = titleColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
    }
}

@Preview(showBackground = true)
@Composable
fun OSettingsScreenPreview() {
    DialDishTheme {
        OSettingsScreen()
    }
}