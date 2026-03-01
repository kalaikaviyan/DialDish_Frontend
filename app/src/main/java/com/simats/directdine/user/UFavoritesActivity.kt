package com.simats.directdine.user

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.directdine.network.*
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch

class UFavoritesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                UFavoritesScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UFavoritesScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)
    val userId = sharedPrefs.getString("LOGGED_IN_USER_ID", "") ?: ""

    // States
    var favoriteStalls by remember { mutableStateOf<List<Stall>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // FETCH FAVORITES LOGIC
    LaunchedEffect(Unit) {
        try {
            // 1. Get List of Favorite IDs
            val favResponse = RetrofitClient.instance.getFavorites(GetFavRequest(userId))
            if (favResponse.isSuccessful && favResponse.body()?.status == "success") {
                val favIds = favResponse.body()?.favorites ?: emptyList()

                // 2. Get All Stalls to display details (Efficient enough for now)
                val stallsResponse = RetrofitClient.instance.fetchStalls()
                if (stallsResponse.isSuccessful) {
                    val allStalls = stallsResponse.body()?.stalls ?: emptyList()
                    // Filter: Keep only stalls whose ID is in the favorites list
                    favoriteStalls = allStalls.filter { it.stall_id in favIds }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load favorites", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // REMOVE FUNCTION
    fun removeFavorite(stallId: Int) {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.toggleFavorite(ToggleFavRequest(userId, stallId))
                if (response.isSuccessful && response.body()?.status == "removed") {
                    // Update UI immediately by removing item from list
                    favoriteStalls = favoriteStalls.filter { it.stall_id != stallId }
                    Toast.makeText(context, "Removed from Favorites", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error removing favorite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        bottomBar = { UserBottomNav(currentSelection = 1, context = context) },
        topBar = {
            TopAppBar(
                title = { Text("My Favorites", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (isLoading) {
                item { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF57C00)) } }
            } else if (favoriteStalls.isEmpty()) {
                item { Text("No favorites yet!", color = Color.Gray) }
            }

            items(favoriteStalls) { stall ->
                FavoriteCard(
                    stallName = stall.stall_name,
                    tags = stall.tags,
                    rating = stall.rating,
                    onRemove = { removeFavorite(stall.stall_id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun FavoriteCard(stallName: String, tags: String, rating: String, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.RestaurantMenu, contentDescription = "Food", tint = Color(0xFFF57C00), modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(stallName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(4.dp))
                Text(tags, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(rating, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Favorite, contentDescription = "Remove", tint = Color(0xFFE53935))
            }
        }
    }
}