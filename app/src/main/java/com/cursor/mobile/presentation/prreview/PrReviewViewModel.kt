package com.cursor.mobile.presentation.prreview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrReviewUiState(
    val prDetail: PullRequestDetail? = null,
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val selectedMergeMethod: String = "squash",
    val replyingToThreadId: String? = null,
    val replyText: String = ""
)

@HiltViewModel
class PrReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AgentRepository
) : ViewModel() {

    private val prId: String = savedStateHandle["prId"] ?: ""

    private val _uiState = MutableStateFlow(PrReviewUiState())
    val uiState: StateFlow<PrReviewUiState> = _uiState.asStateFlow()

    init {
        if (prId.isNotBlank()) {
            loadPrDetail()
        }
    }

    fun loadPrDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val detail = repository.getPullRequest(prId)
                _uiState.update {
                    it.copy(
                        prDetail = detail,
                        isLoading = false,
                        selectedMergeMethod = when (detail.pr.mergeState) {
                            "squash" -> "squash"
                            "rebase" -> "rebase"
                            else -> "merge"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load PR")
                }
            }
        }
    }

    fun onMergeMethodChange(method: String) {
        _uiState.update { it.copy(selectedMergeMethod = method) }
    }

    fun mergePullRequest(deleteBranch: Boolean = false) {
        val detail = _uiState.value.prDetail ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val request = MergeRequest(
                    prId = prId,
                    method = _uiState.value.selectedMergeMethod,
                    title = detail.pr.title,
                    deleteBranch = deleteBranch
                )
                val response = repository.mergePullRequest(prId, request)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Pull request merged successfully"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to merge PR")
                }
            }
        }
    }

    fun updateBranch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.updatePullRequestBranch(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Branch updated"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to update branch")
                }
            }
        }
    }

    fun markReady() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.markPullRequestReady(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Marked ready for review"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to mark ready")
                }
            }
        }
    }

    fun toggleAutoMerge(enable: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.toggleAutoMerge(prId, enable)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: if (enable) "Auto-merge enabled" else "Auto-merge disabled"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to toggle auto-merge")
                }
            }
        }
    }

    fun closePullRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.closePullRequest(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Pull request closed"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to close PR")
                }
            }
        }
    }

    fun reopenPullRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.reopenPullRequest(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Pull request reopened"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to reopen PR")
                }
            }
        }
    }

    fun publishPullRequest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.publishPullRequest(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Pull request published"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to publish PR")
                }
            }
        }
    }

    fun fixWithAgent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                repository.fixWithAgent(prId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = "Started agent to fix failing checks"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to start fix agent")
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun startReply(threadId: String) {
        _uiState.update { it.copy(replyingToThreadId = threadId, replyText = "") }
    }

    fun onReplyTextChange(text: String) {
        _uiState.update { it.copy(replyText = text) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingToThreadId = null, replyText = "") }
    }

    fun submitReply(threadId: String) {
        val body = _uiState.value.replyText.trim()
        if (body.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val request = AddReviewReplyRequest(body = body)
                val response = repository.addReviewReply(prId, threadId, request)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        replyingToThreadId = null,
                        replyText = "",
                        successMessage = response.message ?: "Reply added"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to add reply")
                }
            }
        }
    }

    fun resolveThread(threadId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, error = null, successMessage = null) }
            try {
                val response = repository.resolveReviewThread(prId, threadId)
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        successMessage = response.message ?: "Thread resolved"
                    )
                }
                loadPrDetail()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, error = e.message ?: "Failed to resolve thread")
                }
            }
        }
    }
}
