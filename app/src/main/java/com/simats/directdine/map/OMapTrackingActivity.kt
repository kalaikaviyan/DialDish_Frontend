package com.simats.directdine.map

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.simats.directdine.R // Make sure your R is imported!
import com.simats.directdine.network.RetrofitClient
import com.simats.directdine.network.TrackingRequest
import com.simats.directdine.ui.theme.directdineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class OMapTrackingActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = packageName

        val orderId = intent.getIntExtra("TRACKING_ORDER_ID", -1)

        enableEdgeToEdge()
        setContent {
            directdineTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Live Order Tracking", fontWeight = FontWeight.Bold) },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                ) { innerPadding ->
                    if (orderId == -1) {
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            Text("Invalid Order ID", color = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        TrackingMapScreen(modifier = Modifier.padding(innerPadding), orderId = orderId)
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingMapScreen(modifier: Modifier = Modifier, orderId: Int) {
    val context = LocalContext.current

    var mapView: MapView? by remember { mutableStateOf(null) }
    var driverMarker: Marker? by remember { mutableStateOf(null) }
    var homeMarker: Marker? by remember { mutableStateOf(null) }

    LaunchedEffect(orderId, mapView, driverMarker, homeMarker) {
        if (mapView != null && driverMarker != null && homeMarker != null) {
            while (true) {
                try {
                    val response = RetrofitClient.instance.getTrackingData(TrackingRequest(orderId))
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val data = response.body()!!

                        mapView?.let { map ->
                            if (data.user_lat != null && data.user_lng != null) {
                                val homePt = GeoPoint(data.user_lat.toDouble(), data.user_lng.toDouble())
                                homeMarker!!.position = homePt
                                if (!map.overlays.contains(homeMarker)) map.overlays.add(homeMarker)
                            }

                            if (data.driver_lat != null && data.driver_lng != null) {
                                val driverPt = GeoPoint(data.driver_lat.toDouble(), data.driver_lng.toDouble())
                                driverMarker!!.position = driverPt
                                if (!map.overlays.contains(driverMarker)) map.overlays.add(driverMarker)

                                map.controller.animateTo(driverPt)
                            }
                            map.invalidate()
                        }
                    }
                } catch (e: Exception) { }
                delay(3000)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val newMapView = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(18.0) // Slightly zoomed in closer for 3D effect
            }

            // --- IDEA 1: APPLYING YOUR CUSTOM 3D ASSETS ---
            val newDriverMarker = Marker(newMapView).apply {
                title = "Delivery Partner"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Points to your new 3D bike image in res/drawable!
                icon = ContextCompat.getDrawable(context, R.drawable.isometric_bike_3d)
            }

            val newHomeMarker = Marker(newMapView).apply {
                title = "Delivery Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Points to your new 3D house image in res/drawable!
                icon = ContextCompat.getDrawable(context, R.drawable.isometric_home_3d)
            }

            driverMarker = newDriverMarker
            homeMarker = newHomeMarker
            mapView = newMapView

            newMapView
        }
    )
}