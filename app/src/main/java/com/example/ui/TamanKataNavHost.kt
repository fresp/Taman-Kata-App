package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.*
import com.example.ui.theme.LightBackground
import com.example.ui.theme.PrimaryGreen

@Composable
fun TamanKataNavHost(
    navController: NavHostController,
    viewModel: TamanKataViewModel,
    modifier: Modifier = Modifier
) {
    val hasConsented by viewModel.hasConsented.collectAsState()
    val consentTimestamp by viewModel.consentTimestamp.collectAsState()

    if (hasConsented == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(LightBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    LaunchedEffect(hasConsented) {
        if (hasConsented == false) {
            navController.navigate("consent") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (hasConsented == true) "splash" else "consent"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("consent") {
            ConsentScreen(
                isReviewMode = false,
                consentTimestamp = consentTimestamp,
                onAcceptConsent = {
                    viewModel.setConsent(true)
                    navController.navigate("splash") {
                        popUpTo("consent") { inclusive = true }
                    }
                }
            )
        }
        composable("consent_review") {
            ConsentScreen(
                isReviewMode = true,
                consentTimestamp = consentTimestamp,
                onAcceptConsent = {},
                onRevokeConsent = {
                    viewModel.revokeConsent()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("splash") {
            SplashScreen(
                onTimeout = {
                    navController.navigate("roadmap") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("roadmap") {
            RoadmapScreen(
                viewModel = viewModel,
                onStageSelected = { stageId ->
                    navController.navigate("session/$stageId")
                },
                onNavigateToDashboard = {
                    navController.navigate("dashboard")
                }
            )
        }
        composable("session/{stageId}") { backStackEntry ->
            val stageIdStr = backStackEntry.arguments?.getString("stageId")
            val stageId = stageIdStr?.toIntOrNull() ?: 0

            SessionScreen(
                stageId = stageId,
                viewModel = viewModel,
                onSessionFinished = { duration, itemsCount, avgScore, passed, isTimeLimit ->
                    navController.navigate("result/$avgScore/$passed/$isTimeLimit") {
                        popUpTo("session/{stageId}") { inclusive = true }
                    }
                }
            )
        }
        composable("result/{avgScore}/{passed}/{isTimeLimit}") { backStackEntry ->
            val avgScoreStr = backStackEntry.arguments?.getString("avgScore")
            val avgScore = avgScoreStr?.toIntOrNull() ?: 0
            val passedStr = backStackEntry.arguments?.getString("passed")
            val passed = passedStr?.toBoolean() ?: false
            val isTimeLimitStr = backStackEntry.arguments?.getString("isTimeLimit")
            val isTimeLimit = isTimeLimitStr?.toBoolean() ?: false

            ResultScreen(
                avgScore = avgScore,
                passed = passed,
                isTimeLimit = isTimeLimit,
                onBackToRoadmap = {
                    navController.navigate("roadmap") {
                        popUpTo("roadmap") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            ParentDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPrivacyInfo = {
                    navController.navigate("consent_review")
                }
            )
        }
    }
}

