package com.simats.dialdish

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.dialdish.ui.theme.DialDishTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Using the custom DialDishTheme we set up earlier
            DialDishTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SplashScreen(
                        modifier = Modifier.padding(innerPadding),
                        onTimeout = {
                            // Logic to navigate to LoginActivity after 2 seconds
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish() // Closes MainActivity so the user can't press 'back' to return to the splash screen
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(modifier: Modifier = Modifier, onTimeout: () -> Unit) {
    // This LaunchedEffect runs once when the screen is shown
    LaunchedEffect(Unit) {
        delay(2000L) // Delays for exactly 2000 milliseconds (2 seconds)
        onTimeout()   // Triggers the navigation
    }

    // Design: Recreating the image_1343e6.jpg Splash Screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Uses the warm Cream background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // The Orange App Icon Box
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp)) // Smooth rounded corners
                    .background(MaterialTheme.colorScheme.primary), // Vibrant Orange
                contentAlignment = Alignment.Center
            ) {
                // A temporary emoji until you add your official fork/knife PNG icon
                Text(
                    text = "🍽️",
                    fontSize = 48.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Title
            Text(
                text = "DialDish",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground // Dark Text
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle exactly as seen in your UI design
            Text(
                text = "DIRECT • ZERO COMMISSION • FAST",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF555555), // Subtle grey
                letterSpacing = 1.5.sp
            )
        }
    }
}

// Preview allows you to see the design in Android Studio without running the app
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    DialDishTheme {
        SplashScreen(onTimeout = {})
    }
}