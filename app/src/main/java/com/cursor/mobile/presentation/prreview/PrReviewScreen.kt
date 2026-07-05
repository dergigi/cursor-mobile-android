package com.cursor.mobile.presentation.prreview

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cursor.mobile.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrReviewScreen(
    prId: String?,
    onBack: () -> Unit,
    viewModel: PrReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMergeDialog by remember { mutableStateOf(false) }
    var deleteBranch by remember { mutableStateOf(false) }

    LaunchedEffect(prId) {
        if (!prId.isNullOrBlank()) {
            viewModel.loadPrDetail()
        }
    }

    if (showMergeDialog) {
        MergeConfirmDialog(
            mergeMethod = uiState.selectedMergeMethod,
            deleteBranch = deleteBranch,
            onDeleteBranchChange = { deleteBranch = it },
            onMergeMethodChange = viewModel::onMergeMethodChange,
            onConfirm = {
                viewModel.mergePullRequest(deleteBranch)
                showMergeDialog = false
            },
            onDismiss = { showMergeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review PR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.prDetail?.pr?.url?.let { url ->
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            uiState.prDetail?.pr?.let { pr ->
                BottomActionBar(
                    pr = pr,
                    isLoading = uiState.isActionLoading,
                    onMergeClick = { showMergeDialog = true },
                    onUpdateBranchClick = viewModel::updateBranch,
                    onReadyClick = viewModel::markReady,
                    onAutoMergeClick = { viewModel.toggleAutoMerge(true) },
                    onPublishClick = viewModel::publishPullRequest,
                    onCloseClick = viewModel::closePullRequest,
                    onReopenClick = viewModel::reopenPullRequest,
                    onFixClick = viewModel::fixWithAgent
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.error?.let { error ->
                item {
                    ErrorCard(message = error, onDismiss = viewModel::dismissMessage)
                }
            }

            uiState.successMessage?.let { message ->
                item {
                    SuccessCard(message = message, onDismiss = viewModel::dismissMessage)
                }
            }

            uiState.prDetail?.let { detail ->
                item {
                    PrHeaderCard(pr = detail.pr)
                }

                if (detail.files.isNotEmpty()) {
                    item {
                        Text(
                            "Files changed (${detail.files.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(detail.files) { file ->
                        DiffFileCard(file = file)
                    }
                }

                if (detail.commits.isNotEmpty()) {
                    item {
                        Text(
                            "Commits (${detail.commits.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(detail.commits) { commit ->
                        CommitCard(commit = commit)
                    }
                }

                if (detail.deployments.isNotEmpty()) {
                    item {
                        Text(
                            "Deployments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(detail.deployments) { deployment ->
                        DeploymentCard(deployment = deployment)
                    }
                }

                if (detail.reviewThreads.isNotEmpty()) {
                    item {
                        Text(
                            "Review threads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(detail.reviewThreads) { thread ->
                        ReviewThreadCard(
                            thread = thread,
                            isReplying = uiState.replyingToThreadId == thread.id,
                            replyText = uiState.replyText,
                            onReplyTextChange = viewModel::onReplyTextChange,
                            onStartReply = { viewModel.startReply(thread.id) },
                            onCancelReply = viewModel::cancelReply,
                            onSubmitReply = { viewModel.submitReply(thread.id) },
                            onResolve = { viewModel.resolveThread(thread.id) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun PrHeaderCard(pr: PullRequest) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "#${pr.number} ${pr.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(pr.state, fontSize = 11.sp) },
                    modifier = Modifier.height(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (pr.isDraft) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Draft", fontSize = 11.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                pr.mergeable?.let { mergeable ->
                    Text(
                        if (mergeable) "Mergeable" else "Not mergeable",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (mergeable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${pr.repoUrl.substringAfterLast("/")} • ${pr.branch ?: "unknown"} → ${pr.baseBranch ?: "main"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiffFileCard(file: DiffFile) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (file.changeType.lowercase()) {
                    "added" -> Icons.Default.AddCircle
                    "deleted" -> Icons.Default.Delete
                    else -> Icons.Default.Edit
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    file.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    "+${file.additions}",
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "-${file.deletions}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            file.patch?.let { patch ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        patch,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 8,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitCard(commit: CommitInfo) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Commit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    commit.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    "${commit.sha.take(7)}${commit.author?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeploymentCard(deployment: Deployment) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = when (deployment.state.lowercase()) {
                "success" -> Color(0xFF4CAF50)
                "failure", "error" -> MaterialTheme.colorScheme.error
                "pending" -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    deployment.environment,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    deployment.state,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun ReviewThreadCard(
    thread: ReviewThread,
    isReplying: Boolean = false,
    replyText: String = "",
    onReplyTextChange: (String) -> Unit = {},
    onStartReply: () -> Unit = {},
    onCancelReply: () -> Unit = {},
    onSubmitReply: () -> Unit = {},
    onResolve: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (thread.isResolved)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (thread.isResolved) Icons.Default.CheckCircle else Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = null,
                    tint = if (thread.isResolved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    thread.author ?: "Reviewer",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (thread.isResolved) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Resolved",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                thread.body,
                style = MaterialTheme.typography.bodyMedium
            )
            thread.path?.let {
                Text(
                    "$it${thread.line?.let { line -> ":$line" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Replies
            if (thread.replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                thread.replies.forEach { reply ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                reply.author ?: "Reviewer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                reply.body,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Reply actions
            Spacer(modifier = Modifier.height(8.dp))
            if (isReplying) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = onReplyTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write a reply...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancelReply) {
                        Text("Cancel")
                    }
                    Button(onClick = onSubmitReply, enabled = replyText.isNotBlank()) {
                        Text("Reply")
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onStartReply) {
                        Text("Reply")
                    }
                    if (!thread.isResolved) {
                        TextButton(onClick = onResolve) {
                            Text("Resolve")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    pr: PullRequest,
    isLoading: Boolean,
    onMergeClick: () -> Unit,
    onUpdateBranchClick: () -> Unit,
    onReadyClick: () -> Unit,
    onAutoMergeClick: () -> Unit,
    onPublishClick: () -> Unit,
    onCloseClick: () -> Unit,
    onReopenClick: () -> Unit,
    onFixClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column {
            if (expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUpdateBranchClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Update", fontSize = 12.sp)
                    }
                    if (pr.isDraft) {
                        OutlinedButton(
                            onClick = onReadyClick,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("Ready", fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = onAutoMergeClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Auto-merge", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onPublishClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Publish", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onFixClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Fix", fontSize = 12.sp)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMergeClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && pr.mergeable != false,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Merge", fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedIconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "More actions"
                    )
                }
                if (pr.state.equals("closed", ignoreCase = true)) {
                    OutlinedIconButton(
                        onClick = onReopenClick,
                        modifier = Modifier.size(44.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Replay,
                            contentDescription = "Reopen PR",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    OutlinedIconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(44.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close PR",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeConfirmDialog(
    mergeMethod: String,
    deleteBranch: Boolean,
    onDeleteBranchChange: (Boolean) -> Unit,
    onMergeMethodChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge pull request") },
        text = {
            Column {
                Text("Choose merge method", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("merge", "squash", "rebase").forEach { method ->
                        FilterChip(
                            selected = mergeMethod == method,
                            onClick = { onMergeMethodChange(method) },
                            label = { Text(method.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delete branch after merge")
                    Switch(checked = deleteBranch, onCheckedChange = onDeleteBranchChange)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                modifier = Modifier.weight(1f),
                color = Color(0xFF1B5E20),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
