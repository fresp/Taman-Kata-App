package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.*

@Composable
fun TamanKataNavHost(
    navController: NavHostController,
    viewModel: TamanKataViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
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
                onSessionFinished = { duration, itemsCount, avgScore, passed ->
                    viewModel.saveSession(duration, itemsCount, avgScore, stageId, passed)
                    navController.navigate("result/$avgScore/$passed") {
                        popUpTo("session/{stageId}") { inclusive = true }
                    }
                }
            )
        }
        composable("result/{avgScore}/{passed}") { backStackEntry ->
            val avgScoreStr = backStackEntry.arguments?.getString("avgScore")
            val avgScore = avgScoreStr?.toIntOrNull() ?: 0
            val passedStr = backStackEntry.arguments?.getString("passed")
            val passed = passedStr?.toBoolean() ?: false

            ResultScreen(
                avgScore = avgScore,
                passed = passed,
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
                }
            )
        }
    }
}
