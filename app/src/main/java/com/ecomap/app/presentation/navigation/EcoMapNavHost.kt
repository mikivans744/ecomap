package com.ecomap.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ecomap.app.presentation.ui.auth.*
import com.ecomap.app.presentation.ui.material.*
import com.ecomap.app.presentation.ui.logistics.*
import com.ecomap.app.presentation.ui.map.MapEcomapScreen
import com.ecomap.app.presentation.ui.ai.*
import com.ecomap.app.domain.model.RoleCode
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val RegisterCitizen = "register_citizen"
    const val RegisterCollector = "register_collector"
    const val RegisterCenter = "register_center"
    const val Recover = "recover"
    const val Profile = "profile"
    const val Materials = "materials"
    const val Publish = "publish"
    const val OfferCenter = "offer_center"
    const val Negotiation = "negotiation"
    const val Pickup = "pickup"
    const val Route = "route"
    const val Delivery = "delivery"
    const val Map = "map"
    const val Classifier = "classifier"
    const val Assistant = "assistant"
    const val Reports = "reports"
    const val Support = "support"
}

@Composable
fun EcoMapNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        enterTransition = { slideInHorizontally(animationSpec = tween(500)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = tween(500)) { -it } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(500)) { -it } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(500)) { it } + fadeOut() }
    ) {
        composable(Routes.Splash) {
            SplashScreen(onFinished = {
                navController.navigate(Routes.Login) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            })
        }
        composable(Routes.Login) {
            LoginScreen(
                onRegister = { navController.navigate(Routes.Register) },
                onRecover = { navController.navigate(Routes.Recover) },
                onLoginSuccess = { navController.navigate(Routes.Map) }
            )
        }
        composable(Routes.Register) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterCitizen = { navController.navigate(Routes.RegisterCitizen) },
                onRegisterCollector = { navController.navigate(Routes.RegisterCollector) },
                onRegisterCenter = { navController.navigate(Routes.RegisterCenter) }
            )
        }
        composable(Routes.RegisterCitizen) {
            RegisterFormScreen(
                role = RoleCode.CIUDADANO,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigate(Routes.Map) }
            )
        }
        composable(Routes.RegisterCollector) {
            RegisterFormScreen(
                role = RoleCode.RECOLECTOR,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigate(Routes.Map) }
            )
        }
        composable(Routes.RegisterCenter) {
            RegisterFormScreen(
                role = RoleCode.CENTRO_ACOPIO,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigate(Routes.Map) }
            )
        }
        composable(Routes.Recover) { RecoverPasswordScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Profile) { 
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSupport = { navController.navigate(Routes.Support) },
                onLogout = {
                    navController.navigate(Routes.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            ) 
        }
        composable(Routes.Support) { SupportScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Materials) { MaterialsListScreen() }
        composable(Routes.Publish) { PublishMaterialScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.OfferCenter) { OfferToCenterScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Negotiation) { NegotiationScreen() }
        composable(Routes.Pickup) { RequestPickupScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Route) { ActiveRouteScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Delivery) { RegisterDeliveryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Map) {
            MapEcomapScreen(
                onNavigateToProfile = { navController.navigate(Routes.Profile) },
                onNavigateToAI = { navController.navigate(Routes.Assistant) },
                onNavigateToPublish = { navController.navigate(Routes.Publish) },
                onNavigateToOfferCenter = { navController.navigate(Routes.OfferCenter) },
                onNavigateToMaterials = { navController.navigate(Routes.Materials) }
            )
        }
        composable(Routes.Classifier) { MaterialClassifierScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Assistant) { AssistantScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.Reports) { ReportsScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("lottie/splash_reciclaje.json"))
    
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}
