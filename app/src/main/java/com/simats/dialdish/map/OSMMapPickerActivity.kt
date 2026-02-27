package com.simats.dialdish.map

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import com.simats.dialdish.ui.theme.DialDishTheme
import android.preference.PreferenceManager

class OSMMapPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSM requires this configuration to load map tiles
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContent {
            DialDishTheme {
                MapPickerScreen(
                    onLocationPicked = { lat, lon ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("LATITUDE", lat.toString())
                        resultIntent.putExtra("LONGITUDE", lon.toString())
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(onLocationPicked: (Double, Double) -> Unit, onBackClick: () -> Unit) {
    var mapView: MapView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pinpoint Location", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text("Back", color = Color.Gray) }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    val center = mapView?.mapCenter
                    if (center != null) {
                        onLocationPicked(center.latitude, center.longitude)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
            ) {
                Text("Confirm Location", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    MapView(context).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(16.0)
                        // Centered on Chennai by default
                        controller.setCenter(GeoPoint(13.0827, 80.2707))
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // The static center pin
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Center Pin",
                tint = Color.Red,
                modifier = Modifier.align(Alignment.Center).size(48.dp).offset(y = (-24).dp)
            )
        }
    }
}