package com.ecomap.app.presentation.ui.logistics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RequestPickupScreen(onBack: () -> Unit = {}) {
    var address by remember { mutableStateOf("") }
    val textFieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Solicitar recolección", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        OutlinedTextField(
            value = address, 
            onValueChange = { address = it }, 
            label = { Text("Dirección de recolección") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Text("Estado: SOLICITADA", color = Color.Blue)
        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Solicitar recolector cercano") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun ActiveRouteScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // MEJORA FUTURA: Integrar motor de ruteo turn-by-turn (ej. OSRM u osmdroid BonusPack) para navegación paso a paso con guía de voz.
    val sampleRoutePoints = remember {
        listOf(
            GeoPoint(-17.3895, -66.1568),
            GeoPoint(-17.3850, -66.1530),
            GeoPoint(-17.3780, -66.1500),
            GeoPoint(-17.3700, -66.1450)
        )
    }

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            if (sampleRoutePoints.isNotEmpty()) {
                controller.setCenter(sampleRoutePoints.first())
            }
        }
    }

    LaunchedEffect(sampleRoutePoints) {
        mapView.overlays.clear()
        
        val startMarker = Marker(mapView).apply {
            position = sampleRoutePoints.first()
            title = "Punto de partida"
        }
        mapView.overlays.add(startMarker)

        val endMarker = Marker(mapView).apply {
            position = sampleRoutePoints.last()
            title = "Punto de recolección"
        }
        mapView.overlays.add(endMarker)

        val polyline = Polyline(mapView).apply {
            setPoints(sampleRoutePoints)
            outlinePaint.color = android.graphics.Color.BLUE
            outlinePaint.strokeWidth = 8f
        }
        mapView.overlays.add(polyline)

        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ruta activa", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Text("Estado: EN_RUTA", color = Color.Green)
        Text("Sigue el trazado en el mapa para llegar al punto de recolección.", color = Color.DarkGray)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }

        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Marcar material recolectado") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}

@Composable
fun RegisterDeliveryScreen(onBack: () -> Unit = {}) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.Black, unfocusedTextColor = Color.Black)

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Registrar entrega", style = MaterialTheme.typography.headlineMedium, color = Color.Black)
        Text("Confirma el peso y adjunta evidencia fotográfica.", color = Color.DarkGray)
        OutlinedTextField(
            value = "", 
            onValueChange = {}, 
            label = { Text("Peso recibido en kg") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors
        )
        Button(onClick = { /* Cámara */ }, modifier = Modifier.fillMaxWidth()) { Text("Adjuntar fotografía") }
        Button(onClick = { /* ViewModel */ }, modifier = Modifier.fillMaxWidth()) { Text("Confirmar entrega") }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
    }
}
