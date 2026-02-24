package com.simats.dialdish.owner

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.simats.dialdish.ui.theme.DialDishTheme

@Composable
fun OwnerBottomNavBar(currentSelected: String) {
    val context = LocalContext.current

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        // HOME TAB
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home", fontWeight = FontWeight.Medium) },
            selected = currentSelected == "Home",
            onClick = {
                if (currentSelected != "Home") {
                    val intent = Intent(context, OHomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                }
            },
            colors = navColors()
        )

        // ORDERS TAB
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ListAlt, contentDescription = "Orders") },
            label = { Text("Orders", fontWeight = FontWeight.Medium) },
            selected = currentSelected == "Orders",
            onClick = {
                if (currentSelected != "Orders") {
                    // Make sure you create OOrdersActivity!
                    val intent = Intent(context, Class.forName("com.simats.dialdish.owner.OOrdersActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)
                }
            },
            colors = navColors()
        )

        // MENU TAB
        NavigationBarItem(
            icon = { Icon(Icons.Filled.RestaurantMenu, contentDescription = "Menu") },
            label = { Text("Menu", fontWeight = FontWeight.Medium) },
            selected = currentSelected == "Menu",
            onClick = {
                if (currentSelected != "Menu") {
                    // Make sure you create OMenuActivity!
                    val intent = Intent(context, Class.forName("com.simats.dialdish.owner.OMenuActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)                }
            },
            colors = navColors()
        )

        // PROFILE TAB
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile", fontWeight = FontWeight.Medium) },
            selected = currentSelected == "Profile",
            onClick = {
                if (currentSelected != "Profile") {
                    // Make sure you create OProfileActivity!
                    val intent = Intent(context, Class.forName("com.simats.dialdish.owner.OProfileActivity"))
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    val options = android.app.ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
                    context.startActivity(intent, options)                }
            },
            colors = navColors()
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = Color.Gray,
    unselectedTextColor = Color.Gray,
    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
)
@Preview(showBackground = true)
@Composable
fun OwnerBottomNavBarPreview() {
    DialDishTheme {
        // We pass "Home" just to show what it looks like when the Home tab is active
        OwnerBottomNavBar(currentSelected = "Home")
    }
}