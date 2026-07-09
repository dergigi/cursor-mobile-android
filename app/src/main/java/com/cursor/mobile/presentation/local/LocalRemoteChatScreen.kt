package com.cursor.mobile.presentation.local

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cursor.mobile.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalRemoteChatScreen(
    composerId: String,
    onBack: () -> Unit,
    viewModel: LocalRemoteChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(composerId) {
        viewModel.setComposerId(composerId)
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.activeTabTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1
                        )
                        Text(
                            uiState.agentStatus.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.pendingApprovals.isNotEmpty()) {
                        BadgedBox(
                            badge = { Badge { Text("${uiState.pendingApprovals.size}") } }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Approvals")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState.inputAvailable) {
                MessageInputBar(
                    value = uiState.draftMessage,
                    onValueChange = viewModel::onDraftChange,
                    onSend = { viewModel.sendMessage() },
                    enabled = uiState.socketConnected
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { element ->
                when (element) {
                    is HumanMessage -> HumanMessageItem(message = element)
                    is AssistantMessage -> AssistantMessageItem(message = element)
                    is ToolCallElement -> ToolCallItem(
                        tool = element,
                        onApprove = { element.actions.firstOrNull { it.type == "approve" }?.let { viewModel.approve(it.selectorPath) } },
                        onReject = { element.actions.firstOrNull { it.type == "reject" }?.let { viewModel.reject(it.selectorPath) } }
                    )
                    is RunCommand -> RunCommandItem(
                        command = element,
                        onRun = { element.actions.firstOrNull { it.type == "run" }?.let { viewModel.runCommand(it.selectorPath) } },
                        onSkip = { element.actions.firstOrNull { it.type == "skip" }?.let { viewModel.skipCommand(it.selectorPath) } }
                    )
                    is PlanBlock -> PlanItem(
                        plan = element,
                        onViewPlan = { element.actions.firstOrNull { it.type == "view_plan" }?.let { viewModel.viewPlan(it.selectorPath) } },
                        onBuild = { element.actions.firstOrNull { it.type == "build" }?.let { viewModel.buildPlan(it.selectorPath) } }
                    )
                    is ThoughtBlock -> ThoughtItem(thought = element)
                    is LoadingIndicator -> LoadingItem(indicator = element)
                    is TodoListBlock -> TodoListItem(block = element)
                }
            }
        }
    }
}

@Composable
private fun HumanMessageItem(message: HumanMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AssistantMessageItem(message: AssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                message.codeBlocks.forEach { block ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = block.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCallItem(
    tool: ToolCallElement,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = tool.action,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tool.details,
                style = MaterialTheme.typography.bodyMedium
            )
            if (tool.filename != null) {
                Text(
                    text = tool.filename ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tool.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tool.actions.forEach { action ->
                        when (action.type) {
                            "approve" -> Button(
                                onClick = onApprove,
                                modifier = Modifier.weight(1f)
                            ) { Text(action.label) }
                            "reject" -> OutlinedButton(
                                onClick = onReject,
                                modifier = Modifier.weight(1f)
                            ) { Text(action.label) }
                            else -> OutlinedButton(
                                onClick = { /* generic click handled per type */ },
                                modifier = Modifier.weight(1f)
                            ) { Text(action.label) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunCommandItem(
    command: RunCommand,
    onRun: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = command.description,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                color = Color.Black.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = command.command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                command.actions.forEach { action ->
                    when (action.type) {
                        "run" -> Button(
                            onClick = onRun,
                            modifier = Modifier.weight(1f)
                        ) { Text(action.label) }
                        "skip" -> OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f)
                        ) { Text(action.label) }
                        else -> OutlinedButton(
                            onClick = { },
                            modifier = Modifier.weight(1f)
                        ) { Text(action.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanItem(
    plan: PlanBlock,
    onViewPlan: () -> Unit,
    onBuild: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = plan.title,
                fontWeight = FontWeight.SemiBold
            )
            plan.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${plan.todosCompleted}/${plan.todosTotal} tasks",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.actions.forEach { action ->
                    when (action.type) {
                        "view_plan" -> OutlinedButton(onClick = onViewPlan) { Text(action.label) }
                        "build" -> Button(onClick = onBuild) { Text(action.label) }
                        else -> OutlinedButton(onClick = { }) { Text(action.label) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThoughtItem(thought: ThoughtBlock) {
    Text(
        text = "${thought.action ?: "Thinking"} ${thought.detail ?: ""} (${thought.duration})",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun LoadingItem(indicator: LoadingIndicator) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = indicator.text ?: "Cursor is working...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodoListItem(block: TodoListBlock) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = block.title,
                fontWeight = FontWeight.SemiBold
            )
            block.todos.forEach { todo ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (todo.status == "completed") Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (todo.status == "completed") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = todo.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Message Cursor...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = false,
                maxLines = 5
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
