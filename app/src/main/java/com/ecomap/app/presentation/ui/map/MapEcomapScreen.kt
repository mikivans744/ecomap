package com.ecomap.app.presentation.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ecomap.app.domain.model.Publicacion
import com.ecomap.app.domain.model.RoleCode
import com.ecomap.app.domain.model.Usuario
import com.ecomap.app.presentation.theme.getRoleColor
import com.ecomap.app.util.NetworkUtils
import com.ecomap.app.util.OfflineBanner
import com.ecomap.app.util.ProfileCompletionCalculator
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

sealed class SelectedMapItem {
    data class PubItem(val pub: Publicacion) : SelectedMapItem()
    data class UserItem(val user: Usuario) : SelectedMapItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapEcomapScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCompleteProfile: () -> Unit = {},
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
    val currentUserDoc = remember { mutableStateOf<Usuario?>(null) }

    var selectedItem by remember { mutableStateOf<SelectedMapItem?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = remember(notifications) { notifications.count { !it.leido } }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("usuarios").document(uid).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userRole.value = doc.getString("rol") ?: "CIUDADANO"
                    userName.value = doc.getString("nombre") ?: "Usuario"
                    currentUserDoc.value = Usuario(
                        id = uid,
                        email = doc.getString("email") ?: "",
                        firebaseUid = uid,
                        nombre = doc.getString("nombre") ?: "",
                        rolId = doc.getString("rol") ?: "CIUDADANO",
                        telefono = doc.getString("telefono"),
                        capacidadKg = doc.getDouble("capacidad_kg"),
                        zonaCobertura = doc.getString("zona_cobertura"),
                        lat = doc.getDouble("lat"),
                        lon = doc.getDouble("lon")
                    )
                }
            }
        }
    }

    val completionPercent = ProfileCompletionCalculator.calculate(currentUserDoc.value)
    val roleColor = getRoleColor(userRole.value)

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

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(-17.35, -66.16))
        }
    }

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
                loc?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(geoPoint)
                }
            }
        }
    }

    val users by viewModel.users.collectAsState()
    val publications by viewModel.publications.collectAsState()

    LaunchedEffect(users, publications) {
        mapView.overlays.clear()
        
        users.forEach { user ->
            if (user.lat != null && user.lon != null && (user.lat != 0.0 || user.lon != 0.0)) {
                if (user.rolId == RoleCode.RECOLECTOR.name || user.rolId == RoleCode.CENTRO_ACOPIO.name || user.rolId == "CENTRO") {
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(user.lat!!, user.lon!!)
                        title = "${user.nombre} (${user.rolId})"
                        snippet = "Rol: ${user.rolId}"
                        setOnMarkerClickListener { m, _ ->
                            selectedItem = SelectedMapItem.UserItem(user)
                            m.showInfoWindow()
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }
        }
        
        publications.forEach { pub ->
            if (pub.lat != 0.0 || pub.lon != 0.0) {
                val marker = Marker(mapView).apply {
                    position = GeoPoint(pub.lat, pub.lon)
                    title = "📦 ${pub.titulo}"
                    snippet = "Categoría: ${pub.categoriaId} | Peso: ${pub.pesoKg} kg"
                    setOnMarkerClickListener { m, _ ->
                        selectedItem = SelectedMapItem.PubItem(pub)
                        m.showInfoWindow()
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }
        
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

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                            loc?.let {
                                mapView.controller.animateTo(GeoPoint(it.latitude, it.longitude))
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("📍")
                        Text("Mi ubicación", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                if (userRole.value == "RECOLECTOR") {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToOfferCenter,
                        containerColor = roleColor,
                        contentColor = Color.White,
                        icon = { Text("🤝") },
                        text = { Text("Ofertar a Centro") }
                    )
                } else if (userRole.value == "CIUDADANO") {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToPublish,
                        containerColor = roleColor,
                        contentColor = Color.White,
                        icon = { Text("+") },
                        text = { Text("Publicar") }
                    )
                }
                
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAI,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = { Text("🤖") },
                    text = { Text("Asistente") }
                )
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            OfflineBanner(visible = !NetworkUtils.isNetworkAvailable(context))

            Box(Modifier.fillMaxSize().weight(1f)) {
                if (hasLocationPermission) {
                    AndroidView(
                        factory = { mapView },
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
                
                Card(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hola, ${userName.value}", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                            AssistChip(
                                onClick = {},
                                label = { Text(userRole.value) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = roleColor.copy(alpha = 0.15f), labelColor = roleColor)
                            )
                        }
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            IconButton(onClick = { showNotificationsSheet = true }) {
                                Text("🔔", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = completionPercent < 100,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "💡 Perfil $completionPercent% completado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onNavigateToCompleteProfile) {
                                Text("Completar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                selectedItem?.let { item ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (item) {
                                is SelectedMapItem.PubItem -> {
                                    Text("📦 ${item.pub.titulo}", style = MaterialTheme.typography.titleLarge)
                                    Text("Categoría: ${item.pub.categoriaId}")
                                    Text("Peso: ${item.pub.pesoKg} kg")
                                    if (item.pub.descripcion.isNotBlank()) {
                                        Text("Descripción: ${item.pub.descripcion}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                is SelectedMapItem.UserItem -> {
                                    Text("👤 ${item.user.nombre}", style = MaterialTheme.typography.titleLarge)
                                    Text("Rol: ${item.user.rolId}")
                                    item.user.telefono?.let { Text("Teléfono: $it") }
                                }
                            }
                            Button(
                                onClick = { selectedItem = null },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(onDismissRequest = { showNotificationsSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Notificaciones 🔔", style = MaterialTheme.typography.headlineSmall)
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔔", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("No tienes notificaciones por ahora", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifications) { notif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.markNotificationAsRead(notif.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notif.leido) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(notif.titulo, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                                        if (!notif.leido) {
                                            Badge { Text("Nueva") }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(notif.cuerpo, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
