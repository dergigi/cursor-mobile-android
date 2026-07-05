package com.cursor.mobile

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cursor.mobile.presentation.annotation.AnnotationScreen
import com.cursor.mobile.presentation.chat.ChatScreen
import com.cursor.mobile.presentation.create.CreateAgentScreen
import com.cursor.mobile.presentation.detail.AgentDetailScreen
import com.cursor.mobile.presentation.inbox.InboxScreen
import com.cursor.mobile.presentation.mcp.McpServersScreen
import com.cursor.mobile.presentation.prreview.PrReviewScreen
import com.cursor.mobile.presentation.settings.SettingsScreen
import com.cursor.mobile.presentation.auth.AuthScreen

object Routes {
    const val AUTH = "auth"
    const val INBOX = "inbox"
    const val CREATE = "create"
    const val CHAT = "chat/{agentId}"
    const val DETAIL = "detail/{agentId}"
    const val ANNOTATE = "annotate/{imageUri}"
    const val SETTINGS = "settings"
    const val MCP_SERVERS = "mcp_servers"
    const val PR_REVIEW = "pr_review/{prId}"

    fun chat(agentId: String) = "chat/$agentId"
    fun detail(agentId: String) = "detail/$agentId"
    fun annotate(imageUri: String) = "annotate/${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
    fun prReview(prId: String) = "pr_review/${java.net.URLEncoder.encode(prId, "UTF-8")}"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier
) {
    val startDestination = if (isAuthenticated) Routes.INBOX else Routes.AUTH
    var pendingAnnotatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingChatAgentId by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn() + slideInHorizontally { it / 3 } },
        exitTransition = { fadeOut() + slideOutHorizontally { -it / 3 } },
        popEnterTransition = { fadeIn() + slideInHorizontally { -it / 3 } },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 3 } }
    ) {
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Routes.INBOX) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.INBOX) {
            InboxScreen(
                onAgentClick = { agentId ->
                    navController.navigate(Routes.detail(agentId))
                },
                onCreateClick = {
                    navController.navigate(Routes.CREATE)
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(Routes.CREATE) {
            CreateAgentScreen(
                onBack = { navController.popBackStack() },
                onAgentCreated = { agentId ->
                    navController.navigate(Routes.chat(agentId)) {
                        popUpTo(Routes.INBOX)
                    }
                }
            )
        }

        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("agentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val agentId = backStackEntry.arguments?.getString("agentId")
            ChatScreen(
                agentId = agentId,
                onBack = { navController.popBackStack() },
                onAnnotate = { uri ->
                    navController.navigate(Routes.annotate(uri.toString()))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("agentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val agentId = backStackEntry.arguments?.getString("agentId")
            AgentDetailScreen(
                agentId = agentId,
                onBack = { navController.popBackStack() },
                onChatClick = { id ->
                    navController.navigate(Routes.chat(id))
                },
                onPrClick = { prId ->
                    navController.navigate(Routes.prReview(prId))
                }
            )
        }

        composable(
            route = Routes.ANNOTATE,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val uri = android.net.Uri.parse(java.net.URLDecoder.decode(encodedUri, "UTF-8"))

            AnnotationScreen(
                imageUri = uri,
                onBack = { navController.popBackStack() },
                onAnnotated = { bitmap ->
                    pendingAnnotatedBitmap = bitmap
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.PR_REVIEW,
            arguments = listOf(navArgument("prId") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPrId = backStackEntry.arguments?.getString("prId") ?: ""
            val prId = java.net.URLDecoder.decode(encodedPrId, "UTF-8")

            PrReviewScreen(
                prId = prId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onMcpServersClick = {
                    navController.navigate(Routes.MCP_SERVERS)
                }
            )
        }

        composable(Routes.MCP_SERVERS) {
            McpServersScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
