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
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.presentation.annotation.AnnotationScreen
import com.cursor.mobile.presentation.auth.AuthScreen
import com.cursor.mobile.presentation.chat.ChatScreen
import com.cursor.mobile.presentation.create.CreateAgentScreen
import com.cursor.mobile.presentation.detail.AgentDetailScreen
import com.cursor.mobile.presentation.inbox.InboxScreen
import com.cursor.mobile.presentation.local.LocalRemoteChatScreen
import com.cursor.mobile.presentation.local.LocalRemoteHomeScreen
import com.cursor.mobile.presentation.mcp.McpServersScreen
import com.cursor.mobile.presentation.prreview.PrReviewScreen
import com.cursor.mobile.presentation.home.ModeSelectScreen
import com.cursor.mobile.presentation.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val AUTH = "auth/{mode}"
    const val INBOX = "inbox"
    const val CREATE = "create"
    const val CHAT = "chat/{agentId}"
    const val DETAIL = "detail/{agentId}"
    const val ANNOTATE = "annotate/{imageUri}"
    const val SETTINGS = "settings"
    const val MCP_SERVERS = "mcp_servers"
    const val PR_REVIEW = "pr_review/{prId}"
    const val LOCAL_HOME = "local_home"
    const val LOCAL_CHAT = "local_chat/{composerId}"

    fun chat(agentId: String) = "chat/$agentId"
    fun detail(agentId: String) = "detail/$agentId"
    fun annotate(imageUri: String) = "annotate/${java.net.URLEncoder.encode(imageUri, "UTF-8")}"
    fun prReview(prId: String) = "pr_review/${java.net.URLEncoder.encode(prId, "UTF-8")}"
    fun localChat(composerId: String) = "local_chat/${java.net.URLEncoder.encode(composerId, "UTF-8")}"
    fun auth(mode: ConnectionMode? = null) = when (mode) {
        ConnectionMode.CLOUD -> "auth/cloud"
        ConnectionMode.LOCAL_REMOTE -> "auth/local"
        else -> "auth/any"
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val startDestination = Routes.HOME
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
        composable(Routes.HOME) {
            ModeSelectScreen(
                onCloudSelected = {
                    navController.navigate(Routes.INBOX) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onCloudNeedsAuth = {
                    navController.navigate(Routes.auth(ConnectionMode.CLOUD))
                },
                onLocalSelected = {
                    navController.navigate(Routes.LOCAL_HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onLocalNeedsAuth = {
                    navController.navigate(Routes.auth(ConnectionMode.LOCAL_REMOTE))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.AUTH,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val modeArg = backStackEntry.arguments?.getString("mode")
            val initialMode = when (modeArg) {
                "cloud" -> ConnectionMode.CLOUD
                "local" -> ConnectionMode.LOCAL_REMOTE
                else -> null
            }
            AuthScreen(
                initialMode = initialMode,
                onBack = { navController.popBackStack() },
                onAuthenticated = { mode ->
                    val destination = when (mode) {
                        ConnectionMode.LOCAL_REMOTE -> Routes.LOCAL_HOME
                        ConnectionMode.CLOUD -> Routes.INBOX
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.HOME) { inclusive = true }
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChooseMode = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
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

        composable(Routes.LOCAL_HOME) {
            LocalRemoteHomeScreen(
                onChatClick = { composerId ->
                    navController.navigate(Routes.localChat(composerId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.LOCAL_CHAT,
            arguments = listOf(navArgument("composerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedComposerId = backStackEntry.arguments?.getString("composerId") ?: ""
            val composerId = java.net.URLDecoder.decode(encodedComposerId, "UTF-8")
            LocalRemoteChatScreen(
                composerId = composerId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
