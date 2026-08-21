package com.ecomap.app.presentation.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ecomap.app.domain.model.RoleCode
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

@Composable
fun MapEcomapScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAI: () -> Unit = {},
    onNavigateToPublish: () -> Unit = {},
    onNavigateToOfferCenter: () -> Unit = {},
    onNavigateToMaterials: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val userRole = remember { mutableStateOf("CIUDADANO") }
    val userName = remember { mutableStateOf("Cargando...") }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("usuarios").document(uid).get().addOnSuccessListener {
                userRole.value = it.getString("rol") ?: "CIUDADANO"
                userName.value = it.getString("nombre") ?: "Usuario"
            }
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    val mapView = remember { MapView(context) }
    var annotationManager by remember { mutableStateOf<PointAnnotationManager?>(null) }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 20000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    p0.lastLocation?.let { viewModel.updateLocation(it) }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                val point = loc?.let { Point.fromLngLat(it.longitude, it.latitude) } 
                            ?: Point.fromLngLat(-66.16, -17.35)
                mapView.mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(14.0).build())
            }
        }
    }

    val users by viewModel.users.collectAsState()
    val publications by viewModel.publications.collectAsState()

    LaunchedEffect(users, publications, annotationManager) {
        annotationManager?.let { manager ->
            manager.deleteAll()
            users.forEach { user ->
                if (user.lat != null && user.lon != null) {
                    manager.create(PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(user.lon, user.lat))
                        .withTextField("${user.nombre} (${user.rolId})")
                    )
                }
            }
            publications.forEach { pub ->
                manager.create(PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(pub.lon, pub.lat))
                    .withTextField("📦 ${pub.titulo}")
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        loc?.let {
                            mapView.mapboxMap.setCamera(CameraOptions.Builder().center(Point.fromLngLat(it.longitude, it.latitude)).zoom(15.0).build())
                        }
                    }
                }) { Text("📍") }
                
                if (userRole.value == "RECOLECTOR") {
                    FloatingActionButton(onClick = onNavigateToOfferCenter, containerColor = MaterialTheme.colorScheme.secondary) {
                        Text("🤝")
                    }
                } else if (userRole.value == "CIUDADANO") {
                    FloatingActionButton(onClick = onNavigateToPublish, containerColor = MaterialTheme.colorScheme.primary) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                
                FloatingActionButton(onClick = onNavigateToAI) { Text("🤖") }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Text("🗺️") }, label = { Text("Mapa") }, selected = true, onClick = {})
                val label = when(userRole.value) {
                    RoleCode.CIUDADANO.name -> "Mis Publicaciones"
                    RoleCode.RECOLECTOR.name -> "Cercanas"
                    RoleCode.CENTRO_ACOPIO.name -> "Ofertas"
                    else -> "Materiales"
                }
                NavigationBarItem(icon = { Text("📦") }, label = { Text(label) }, selected = false, onClick = onNavigateToMaterials)
                NavigationBarItem(icon = { Text("👤") }, label = { Text("Perfil") }, selected = false, onClick = onNavigateToProfile)
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (hasLocationPermission) {
                AndroidView(
                    factory = { 
                        mapView.apply {
                            mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                                annotationManager = annotations.createPointAnnotationManager()
                                location.enabled = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Se requiere permiso de ubicación para el mapa.", color = Color.Black)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                        Text("Reintentar")
                    }
                }
            }
            
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Hola, ${userName.value}", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                        Text(userRole.value, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Text("🔔")
                }
            }
        }
    }
}
