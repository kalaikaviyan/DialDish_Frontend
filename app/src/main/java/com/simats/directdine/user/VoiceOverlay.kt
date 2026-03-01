package com.simats.directdine.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simats.directdine.ui.theme.directdineTheme

@Composable
fun VoiceSearchDialog(
    onDismiss: () -> Unit,
    onVoiceResult: (String) -> Unit // We will pass the spoken text back here later
) {
    // A premium dark overlay covering the screen
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE121212)), // Sleek semi-transparent dark glass
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {

                // 1. Animated Sound Waves
                AnimatedSoundWaves()

                Spacer(modifier = Modifier.height(32.dp))

                // 2. Listening Text
                Text(
                    text = "Listening...",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Speak now, e.g., \"Call Royal Biryani\"",
                    fontSize = 16.sp,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(48.dp))

                // 3. Suggestion Chips (Upgraded UI)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SuggestionChip("Order Pizza")
                    SuggestionChip("Call Spicy Bites")
                }
                Spacer(modifier = Modifier.height(12.dp))
                SuggestionChip("Find Veg Food")

                Spacer(modifier = Modifier.height(64.dp))

                // 4. Cancel Button
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel Voice Search",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.DarkGray),
        modifier = Modifier.clip(RoundedCornerShape(24.dp))
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AnimatedSoundWaves() {
    // Creates a glowing, pulsing sound wave effect
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(60.dp)
    ) {
        val delays = listOf(0, 150, 300, 150, 0)

        delays.forEach { delay ->
            val height by infiniteTransition.animateFloat(
                initialValue = 16f,
                targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_height"
            )

            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF57C00)) // directdine Orange
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceOverlayPreview() {
    directdineTheme {
        VoiceSearchDialog(onDismiss = {}, onVoiceResult = {})
    }
}