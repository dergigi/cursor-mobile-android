package com.cursor.mobile.presentation.chat

import androidx.lifecycle.SavedStateHandle
import com.cursor.mobile.MainDispatcherRule
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AgentRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle().apply { set("agentId", "agent-1") }
        viewModel = ChatViewModel(savedStateHandle, repository)
    }

    @Test
    fun `sendMessage triggers remote control dialog for slash command`() = runTest {
        coEvery { repository.listWorkers() } returns WorkerListResponse(
            items = listOf(Worker(id = "local-1", type = "local", name = "MacBook"))
        )

        viewModel.onInputChange("/remote-control")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showRemoteControlDialog)
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `applySlashCommand updates input text`() = runTest {
        val command = SlashCommand(id = "fix", name = "fix", description = "Fix with agent")
        viewModel.applySlashCommand(command)

        assertEquals("/fix ", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.showSlashCommands)
    }

    @Test
    fun `sendMessage creates run for normal message`() = runTest {
        val agent = Agent(id = "agent-1", name = "Agent", status = "ACTIVE")
        val run = Run(id = "run-1", agentId = "agent-1", status = "RUNNING")

        coEvery { repository.getAgent("agent-1") } returns agent
        coEvery { repository.listRuns(any(), any()) } returns RunListResponse(items = emptyList())
        coEvery { repository.createRun(any(), any()) } returns run
        coEvery { repository.streamRun(any(), any()) } returns flowOf(SseEvent.Done)

        viewModel.onInputChange("Hello agent")
        viewModel.sendMessage()
        advanceUntilIdle()

        coVerify { repository.createRun("agent-1", match { it.prompt.text == "Hello agent" }) }
        assertEquals("run-1", viewModel.uiState.value.currentRunId)
    }

    @Test
    fun `connectRemoteControl requires active run`() = runTest {
        val worker = Worker(id = "local-1", type = "local", name = "MacBook")

        viewModel.connectRemoteControl(worker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Expected error about missing active run. State: $state",
            state.error?.contains("No active run") == true || state.messages.any { it.role == MessageRole.SYSTEM }
        )
    }
}
