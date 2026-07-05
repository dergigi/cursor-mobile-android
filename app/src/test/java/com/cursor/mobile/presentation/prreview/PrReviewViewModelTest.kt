package com.cursor.mobile.presentation.prreview

import androidx.lifecycle.SavedStateHandle
import com.cursor.mobile.MainDispatcherRule
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: AgentRepository
    private lateinit var viewModel: PrReviewViewModel

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        val savedStateHandle = SavedStateHandle().apply { set("prId", "pr-123") }
        viewModel = PrReviewViewModel(savedStateHandle, repository)
    }

    @Test
    fun `loadPrDetail populates state`() = runTest {
        val pr = PullRequest(
            id = "pr-123",
            number = 42,
            title = "Add feature",
            url = "https://github.com/acme/app/pull/42",
            state = "open",
            repoUrl = "https://github.com/acme/app",
            mergeable = true
        )
        val detail = PullRequestDetail(
            pr = pr,
            files = listOf(DiffFile(path = "app.kt", changeType = "added", additions = 10)),
            commits = emptyList()
        )

        coEvery { repository.getPullRequest("pr-123") } returns detail

        viewModel.loadPrDetail()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.prDetail)
        assertEquals(1, viewModel.uiState.value.prDetail?.files?.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `mergePullRequest calls repository with selected method`() = runTest {
        val pr = PullRequest(
            id = "pr-123",
            number = 42,
            title = "Add feature",
            url = "https://github.com/acme/app/pull/42",
            state = "open",
            repoUrl = "https://github.com/acme/app",
            mergeable = true
        )
        coEvery { repository.getPullRequest("pr-123") } returns PullRequestDetail(pr = pr)
        coEvery { repository.mergePullRequest(any(), any()) } returns MergeResponse(success = true)

        viewModel.onMergeMethodChange("squash")
        viewModel.mergePullRequest(deleteBranch = true)
        advanceUntilIdle()

        coVerify {
            repository.mergePullRequest(
                "pr-123",
                match { it.method == "squash" && it.deleteBranch }
            )
        }
    }

    @Test
    fun `toggleAutoMerge calls repository`() = runTest {
        coEvery { repository.toggleAutoMerge("pr-123", true) } returns PrActionResponse(success = true)

        viewModel.toggleAutoMerge(true)
        advanceUntilIdle()

        coVerify { repository.toggleAutoMerge("pr-123", true) }
        assertEquals("Auto-merge enabled", viewModel.uiState.value.successMessage)
    }
}
