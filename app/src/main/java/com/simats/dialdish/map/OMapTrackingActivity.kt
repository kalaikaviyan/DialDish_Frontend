package com.simats.dialdish.map

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.simats.dialdish.ui.theme.DialDishTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OMapTrackingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // IMPORTANT: OSMDroid requires this configuration BEFORE the map is loaded to prevent crashes
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        enableEdgeToEdge()
        setContent {
            DialDishTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Live Order Tracking", fontWeight = FontWeight.Bold) },
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
                    TrackingMapScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TrackingMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Set map starting point to Madurai (Lat: 9.9252, Lon: 78.1198)
    val startPoint = GeoPoint(9.9252, 78.1198)

    // AndroidView is used because OSMDroid is traditionally an XML View, this wraps it for Jetpack Compose!
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                // Set the visual style of the map
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true) // Allows pinch-to-zoom

                // Set starting zoom and location
                controller.setZoom(17.0)
                controller.setCenter(startPoint)

                // Add a mock marker for your delivery staff
                val deliveryMarker = Marker(this).apply {
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Ramesh (AD3214) - Moving"
                    snippet = "Order #1024 - In Transit"
                }
                overlays.add(deliveryMarker)
            }
        },
        update = { mapView ->
            // In the future, this is where we will write the logic to fetch PHP coordinates
            // every 10 seconds and update the marker's live position!
        }
    )
}