package com.simats.directdine.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.simats.directdine.network.*
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.launch

class UStallMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            directdineTheme {
                val stallId = intent.getIntExtra("STALL_ID", -1)
                if (stallId != -1) {
                    UStallMenuScreen(stallId = stallId, onBackClick = { finish() })
                } else {
                    Toast.makeText(this, "Error: Invalid Stall", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}

@Composable
fun UStallMenuScreen(stallId: Int, onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = context.getSharedPreferences("directdinePrefs", Context.MODE_PRIVATE)
    val userId = sharedPrefs.getString("LOGGED_IN_USER_ID", "1") ?: "1"
    val userName = sharedPrefs.getString("LOGGED_IN_USER_NAME", "User") ?: "User"
    val baseUrl = RetrofitClient.BASE_URL

    var menuResponse by remember { mutableStateOf<MenuResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }

    val cartCounts = remember { mutableStateMapOf<Int, Int>() }
    var cartTotal by remember { mutableDoubleStateOf(0.0) }

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var isSubmittingReview by remember { mutableStateOf(false) }
    var editingReviewId by remember { mutableStateOf<Int?>(null) }

    fun fetchMenuData() {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.getStallMenu(FetchMenuRequest(stallId, userId))
                if (response.isSuccessful && response.body()?.status == "success") {
                    menuResponse = response.body()
                    isFavorite = menuResponse?.stall_data?.is_favorite == true
                }
            } catch (e: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(stallId) { fetchMenuData() }

    fun toggleFav() {
        coroutineScope.launch {
            try {
                isFavorite = !isFavorite
                val response = RetrofitClient.instance.toggleFavorite(ToggleFavRequest(userId, stallId))
                if (!response.isSuccessful) isFavorite = !isFavorite
            } catch (e: Exception) { isFavorite = !isFavorite }
        }
    }

    fun deleteReview(reviewId: Int) {
        coroutineScope.launch {
            try {
                val response = RetrofitClient.instance.deleteReview(DeleteReviewRequest(reviewId, userName))
                if (response.isSuccessful) {
                    Toast.makeText(context, "Review deleted", Toast.LENGTH_SHORT).show()
                    fetchMenuData()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showReviewDialog) {
        Dialog(onDismissRequest = { showReviewDialog = false; editingReviewId = null; reviewComment = ""; reviewRating = 5 }) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (editingReviewId != null) "Edit Review" else "Rate ${menuResponse?.stall_data?.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.Center) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= reviewRating) Color(0xFFFFC107) else Color.Gray,
                                modifier = Modifier.size(40.dp).clickable { reviewRating = i }.padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = reviewComment, onValueChange = { reviewComment = it },
                        placeholder = { Text("Write your experience...", color = Color.LightGray) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFF57C00), unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { showReviewDialog = false; editingReviewId = null }) { Text("Cancel", color = Color.Gray) }
                        Button(
                            onClick = {
                                isSubmittingReview = true
                                coroutineScope.launch {
                                    try {
                                        val response = if (editingReviewId != null) {
                                            RetrofitClient.instance.editReview(EditReviewRequest(editingReviewId!!, userName, reviewRating.toFloat(), reviewComment))
                                        } else {
                                            RetrofitClient.instance.addReview(AddReviewRequest(stallId, userId, userName, reviewRating.toFloat(), reviewComment))
                                        }

                                        if (response.isSuccessful && response.body()?.status == "success") {
                                            Toast.makeText(context, if (editingReviewId != null) "Review Updated!" else "Review Published!", Toast.LENGTH_SHORT).show()
                                            showReviewDialog = false
                                            editingReviewId = null
                                            reviewComment = ""
                                            fetchMenuData()
                                        } else { Toast.makeText(context, "Failed to publish", Toast.LENGTH_SHORT).show() }
                                    } catch (e: Exception) { Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show() }
                                    finally { isSubmittingReview = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                        ) {
                            if (isSubmittingReview) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            else Text(if (editingReviewId != null) "Update" else "Publish", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (cartTotal > 0) {
                Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
                    Surface(
                        color = Color(0xFF2C3539),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (menuResponse != null) {
                                val selectedItems = menuResponse!!.menu.filter { (cartCounts[it.item_id] ?: 0) > 0 }
                                val cartSummary = selectedItems.joinToString("\n") { "${cartCounts[it.item_id]}x ${it.name}" }

                                CartData.stallId = stallId
                                CartData.stallName = menuResponse!!.stall_data.name
                                CartData.stallPhone = menuResponse!!.stall_data.contact_phone ?: ""
                                CartData.cartItemsText = cartSummary
                                CartData.cartTotal = cartTotal

                                context.startActivity(Intent(context, UCartActivity::class.java))
                            }
                        }
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${cartCounts.values.sum()} Items | ₹$cartTotal", color = Color.White, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("View Cart", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ChevronRight, contentDescription = "Go", tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        content = { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFFF57C00)) }
            } else if (menuResponse != null) {
                val stall = menuResponse!!.stall_data
                val menu = menuResponse!!.menu
                val highlight = menuResponse!!.highlight_item
                val mealType = menuResponse!!.current_meal_type
                val transitionMessage = menuResponse!!.transition_message
                val isMenuLocked = menuResponse!!.is_locked

                LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).padding(paddingValues)) {

                    item {
                        val vibrantAppBrush = Brush.verticalGradient(listOf(Color(0xFFFF9800), Color(0xFFFFCC80)))
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp).background(vibrantAppBrush)) {

                            val patternTint = Color.White.copy(alpha = 0.25f)
                            Icon(Icons.Filled.Fastfood, contentDescription = null, tint = patternTint, modifier = Modifier.size(120.dp).offset(x = (-20).dp, y = 20.dp))
                            Icon(Icons.Filled.LocalDining, contentDescription = null, tint = patternTint, modifier = Modifier.size(150.dp).offset(x = 220.dp, y = 40.dp))
                            Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = patternTint, modifier = Modifier.size(90.dp).offset(x = 100.dp, y = 140.dp))

                            // THE FIX: The Creative Decorative Wall!
                            // A grid of elegant floating icons representing a premium food environment.
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Left Side Details
                                Icon(Icons.Filled.Restaurant, contentDescription = null, tint = patternTint.copy(alpha = 0.15f), modifier = Modifier.size(80.dp).offset(x = 10.dp, y = 180.dp))
                                Icon(Icons.Filled.Cake, contentDescription = null, tint = patternTint.copy(alpha = 0.1f), modifier = Modifier.size(60.dp).offset(x = 90.dp, y = 30.dp))

                                // Right Side Details
                                Icon(Icons.Filled.LocalPizza, contentDescription = null, tint = patternTint.copy(alpha = 0.15f), modifier = Modifier.size(100.dp).offset(x = 280.dp, y = 150.dp))
                                Icon(Icons.Filled.EmojiFoodBeverage, contentDescription = null, tint = patternTint.copy(alpha = 0.2f), modifier = Modifier.size(70.dp).offset(x = 200.dp, y = (-10).dp))

                                // Center
                                Icon(Icons.Filled.SetMeal, contentDescription = null, tint = patternTint.copy(alpha = 0.2f), modifier = Modifier.size(130.dp).align(Alignment.Center))
                            }
                            // Keep the dark overlay so the white text still pops!
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape).clickable { onBackClick() }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape).clickable { toggleFav() }, contentAlignment = Alignment.Center) {
                                    Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Like", tint = if (isFavorite) Color.Red else Color.White)
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).align(Alignment.BottomCenter).offset(y = 20.dp).shadow(8.dp, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(stall.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(stall.rating, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(Icons.Filled.Star, contentDescription = "Star", tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (stall.is_open) "OPEN NOW" else "CLOSED", color = if (stall.is_open) Color(0xFF4CAF50) else Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("  •  Now Serving: $mealType", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    if (!transitionMessage.isNullOrEmpty()) {
                        item {
                            Surface(color = if (isMenuLocked) Color(0xFFFFEBEE) else Color(0xFFFFF3E0), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (isMenuLocked) Icons.Filled.Lock else Icons.Filled.Info, contentDescription = "Info", tint = if (isMenuLocked) Color.Red else Color(0xFFF57C00))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(transitionMessage, color = if (isMenuLocked) Color.Red else Color(0xFFE65100), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (highlight != null) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Today's Special 🌟", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(highlight.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("₹${highlight.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
                                    }
                                    val highlightImgUrl = if (highlight.image_url?.startsWith("uploads/") == true) "$baseUrl${highlight.image_url}" else "${baseUrl}uploads/${highlight.image_url}"
                                    if (!highlight.image_url.isNullOrEmpty()) {
                                        AsyncImage(model = highlightImgUrl.trim().replace(" ", "%20"), contentDescription = "Special", contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)))
                                    } else {
                                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Fastfood, contentDescription = "Special", tint = Color.Gray) }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Menu", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        if (menu.isEmpty()) { Text("No items listed yet.", color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp)) }
                    }

                    items(menu) { item ->
                        val count = cartCounts[item.item_id] ?: 0
                        MenuItemRow(
                            item = item, count = count, baseUrl = baseUrl, isLocked = isMenuLocked,
                            onAdd = {
                                if (!isMenuLocked) { cartCounts[item.item_id] = count + 1; cartTotal += item.price.toDouble() }
                                else { Toast.makeText(context, "Menu is currently locked.", Toast.LENGTH_SHORT).show() }
                            },
                            onRemove = { if (!isMenuLocked && count > 0) { cartCounts[item.item_id] = count - 1; cartTotal -= item.price.toDouble() } }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), color = Color(0xFFEEEEEE))
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Customer Reviews", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (menuResponse!!.reviews.isEmpty()) {
                        item { Text("No reviews yet. Be the first!", color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) }
                    } else {
                        items(menuResponse!!.reviews) { review ->
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row {
                                        Text(review.user, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("⭐ ${review.rating}", color = Color(0xFFF57C00), fontSize = 14.sp)
                                    }

                                    if (review.user == userName) {
                                        Row {
                                            Icon(
                                                Icons.Filled.Edit, contentDescription = "Edit",
                                                modifier = Modifier.clickable {
                                                    editingReviewId = review.review_id
                                                    reviewComment = review.comment
                                                    reviewRating = review.rating.toInt()
                                                    showReviewDialog = true
                                                }.size(18.dp),
                                                tint = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Icon(
                                                Icons.Filled.Delete, contentDescription = "Delete",
                                                modifier = Modifier.clickable { deleteReview(review.review_id) }.size(18.dp),
                                                tint = Color.Red.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                Text(review.comment, color = Color.DarkGray, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFFEEEEEE))
                        }
                    }

                    item {
                        Button(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF57C00))
                        ) {
                            Text("Write a Review", color = Color(0xFFF57C00), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun MenuItemRow(item: MenuItem, count: Int, baseUrl: String, isLocked: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.desc ?: "", color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text("₹${item.price}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFF57C00))
        }

        Box(contentAlignment = Alignment.BottomCenter) {
            val rowImgUrl = if (item.image_url?.startsWith("uploads/") == true) "$baseUrl${item.image_url}" else "${baseUrl}uploads/${item.image_url}"
            if (!item.image_url.isNullOrEmpty() && item.image_url != "Fetching...") {
                AsyncImage(
                    model = rowImgUrl.trim().replace(" ", "%20"),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)),
                    onState = { state ->
                        if (state is coil.compose.AsyncImagePainter.State.Error) {
                            android.util.Log.e("CoilError", "User Image failed: $rowImgUrl", state.result.throwable)
                        }
                    }
                )
            } else {
                Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFFDF5)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Fastfood, contentDescription = "Food", tint = Color(0xFFD7CCC8).copy(alpha = 0.6f)) }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isLocked) Color(0xFFEEEEEE) else if (count > 0) Color(0xFFF57C00) else Color.White,
                border = if (isLocked) BorderStroke(1.dp, Color.LightGray) else if (count == 0) BorderStroke(1.dp, Color.LightGray) else null,
                modifier = Modifier.padding(bottom = 0.dp).offset(y = 12.dp).shadow(if (isLocked) 0.dp else 2.dp, RoundedCornerShape(8.dp))
            ) {
                if (isLocked) {
                    Text("LOCKED 🔒", color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAdd() }.padding(horizontal = 16.dp, vertical = 6.dp), fontSize = 12.sp)
                } else if (count > 0) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.clickable { onRemove() })
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("$count", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.clickable { onAdd() })
                    }
                } else {
                    Text("ADD", color = Color(0xFFF57C00), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAdd() }.padding(horizontal = 24.dp, vertical = 6.dp))
                }
            }
        }
    }
}