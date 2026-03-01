package com.simats.directdine.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun UserBottomNav(currentSelection: Int, context: Context) {
    val items = listOf(
        Triple("Home", Icons.Filled.Home, UHomeActivity::class.java),
        Triple("Favorites", Icons.Filled.Favorite, UFavoritesActivity::class.java), // We will create this next
        Triple("Orders", Icons.Filled.ShoppingBag, UOrdersActivity::class.java),    // We will create this next
        Triple("Profile", Icons.Filled.Person, UProfileActivity::class.java)        // We will create this next
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFFF57C00) // Orange theme from your image
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = currentSelection == index,
                onClick = {
                    if (currentSelection != index) {
                        val intent = Intent(context, item.third)
                        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        context.startActivity(intent)
                        (context as? Activity)?.overridePendingTransition(0, 0) // Removes flicker
                    }
                },
                icon = { Icon(item.second, contentDescription = item.first) },
                label = { Text(item.first) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFF57C00), // Active Orange
                    selectedTextColor = Color(0xFFF57C00),
                    indicatorColor = Color(0xFFFFF3E0),    // Light orange background for active icon
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray
                )
            )
        }
    }
}