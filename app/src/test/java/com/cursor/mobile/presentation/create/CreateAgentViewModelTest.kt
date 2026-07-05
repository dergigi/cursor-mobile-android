package com.cursor.mobile.presentation.create

import com.cursor.mobile.MainDispatcherRule
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAgentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AgentRepository
    private lateinit var viewModel: CreateAgentViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        viewModel = CreateAgentViewModel(repository)
    }

    @Test
    fun `createAgent sends request with selected worker`() = runTest {
        val worker = Worker(id = "local-1", type = "local", name = "MacBook Pro")
        val model = ModelInfo(id = "gpt-4o", displayName = "GPT-4o")
        val repo = RepositoryItem(url = "https://github.com/acme/app")

        coEvery { repository.listWorkers() } returns WorkerListResponse(items = listOf(worker))
        coEvery { repository.listModels() } returns ModelsResponse(items = listOf(model))
        coEvery { repository.listRepositories() } returns RepositoriesResponse(items = listOf(repo))
        coEvery { repository.createAgent(any()) } returns AgentWithRun(
            agent = Agent(id = "agent-1", name = "Test Agent", status = "ACTIVE"),
            run = Run(id = "run-1", agentId = "agent-1", status = "RUNNING")
        )

        viewModel.onPromptChange("Implement login screen")
        viewModel.onWorkerChange(worker.id, worker.type)
        viewModel.onRepoChange(repo.url)
        viewModel.onModelChange(model.id)
        viewModel.onAutoCreatePRChange(true)

        viewModel.createAgent()
        advanceUntilIdle()

        coVerify {
            repository.createAgent(
                match { request ->
                    request.prompt.text == "Implement login screen" &&
                        request.agentId == "local-1" &&
                        request.autoCreatePR == true &&
                        request.model?.id == "gpt-4o"
                }
            )
        }

        assertEquals("agent-1", viewModel.uiState.value.createdAgentId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `createAgent shows error when prompt empty`() = runTest {
        viewModel.createAgent()
        advanceUntilIdle()
        assertEquals("Prompt cannot be empty", viewModel.uiState.value.error)
    }
}
