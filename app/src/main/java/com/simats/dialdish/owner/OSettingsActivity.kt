package com.simats.dialdish.owner

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
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

        // 1. Check system default to know what to display on the very first launch
        val isSystemDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val sharedPrefs = getSharedPreferences("DialDishPrefs", Context.MODE_PRIVATE)
        val savedTheme = sharedPrefs.getBoolean("isDarkTheme", isSystemDark)

        enableEdgeToEdge()
        setContent {
            // 2. Load the saved theme into the UI State
            var isDarkTheme by remember { mutableStateOf(savedTheme) }

            DialDishTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                    OSettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { newThemeValue ->
                            // 3. Update UI state instantly
                            isDarkTheme = newThemeValue

                            // 4. Save to SharedPreferences so EVERY OTHER PAGE reads it globally
                            sharedPrefs.edit().putBoolean("isDarkTheme", newThemeValue).apply()

                            // 5. Force the app to switch system modes
                            val mode = if (newThemeValue) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                            AppCompatDelegate.setDefaultNightMode(mode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OSettingsScreen(modifier: Modifier = Modifier, isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val context = LocalContext.current

    // States for Popups
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // States for Toggles
    var isAcceptingOrders by remember { mutableStateOf(true) }
    var notifyNewOrders by remember { mutableStateOf(true) }
    var notifyDelivery by remember { mutableStateOf(true) }
    var notifyInquiries by remember { mutableStateOf(true) }

    // ==========================================
    // DIALOGS (POP-UPS)
    // ==========================================
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
                        val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                        context.startActivity(intent, options)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Yes, Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
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
                        val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                        context.startActivity(intent, options)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Yes, Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // ==========================================
    // MAIN LAYOUT
    // ==========================================
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // CARD 1: STORE STATUS (Emergency Toggle)
        SettingsSectionTitle("STORE STATUS")
        SettingsCard {
            SettingsToggleRow(
                icon = Icons.Filled.Store,
                title = "Accepting Orders",
                subtitle = if (isAcceptingOrders) "Store is Open" else "Store is Temporarily Closed",
                isChecked = isAcceptingOrders,
                onCheckedChange = { isAcceptingOrders = it }
            )
        }

        // CARD 2: PREFERENCES (Notifications & Theme)
        SettingsSectionTitle("PREFERENCES")
        SettingsCard {
            SettingsToggleRow(icon = Icons.Filled.NotificationsActive, title = "New Order Alerts", subtitle = "Ring when users place an order", isChecked = notifyNewOrders, onCheckedChange = { notifyNewOrders = it })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsToggleRow(icon = Icons.Filled.CheckCircle, title = "Delivery Completed", subtitle = "Notify when staff delivers an order", isChecked = notifyDelivery, onCheckedChange = { notifyDelivery = it })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsToggleRow(icon = Icons.Filled.Phone, title = "Inquiries & Calls", subtitle = "Alert for special user requests", isChecked = notifyInquiries, onCheckedChange = { notifyInquiries = it })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsToggleRow(icon = Icons.Filled.DarkMode, title = "Dark Theme", subtitle = "Toggle app visual style", isChecked = isDarkTheme, onCheckedChange = onThemeChange)
        }

        // CARD 3: SUPPORT & LEGAL (App Info)
        SettingsSectionTitle("SUPPORT & INFO")
        SettingsCard {
            SettingsClickableRow(icon = Icons.Filled.MenuBook, title = "How to use the App", onClick = {
                val intent = Intent(context, OHowToUseActivity::class.java)
                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                context.startActivity(intent, options)
            })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.Star, title = "App Features", onClick = {
                val intent = Intent(context, OAppFeaturesActivity::class.java)
                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                context.startActivity(intent, options)
            })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.PrivacyTip, title = "Privacy Policy & Legal", onClick = {
                val intent = Intent(context, OPrivacyPolicyActivity::class.java)
                val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                context.startActivity(intent, options)
            })
        }

        // CARD 4: DANGER ZONE
        SettingsSectionTitle("ACCOUNT MANAGEMENT")
        SettingsCard {
            SettingsClickableRow(icon = Icons.Filled.Logout, title = "Logout", titleColor = MaterialTheme.colorScheme.primary, onClick = { showLogoutDialog = true })
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            SettingsClickableRow(icon = Icons.Filled.DeleteForever, title = "Delete Account", titleColor = Color(0xFFF44336), onClick = { showDeleteDialog = true })
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Reusable Components
@Composable
fun SettingsSectionTitle(title: String) {
    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp, top = 16.dp))
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)) { Column(content = content) }
}

@Composable
fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = title, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
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
        OSettingsScreen(
            isDarkTheme = false,
            onThemeChange = {}
        )
    }
}