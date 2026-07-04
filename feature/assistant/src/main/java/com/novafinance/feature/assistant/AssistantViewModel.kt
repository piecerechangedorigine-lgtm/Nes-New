package com.novafinance.feature.assistant

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.assistant.AssistantInsightEngine
import com.novafinance.core.domain.model.AssistantAction
import com.novafinance.core.domain.model.AssistantActionType
import com.novafinance.core.domain.model.AssistantContext
import com.novafinance.core.domain.model.AssistantMessage
import com.novafinance.core.domain.model.AssistantSender
import com.novafinance.core.domain.model.AssistantSuggestedPrompt
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.usecase.GetAssistantContextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val getAssistantContext: GetAssistantContextUseCase,
    private val insightEngine: AssistantInsightEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    /** One-off "open this screen" events fired by tapping an action chip inside a reply. */
    private val _navigationEvents = Channel<AssistantActionType>(Channel.BUFFERED)
    val navigationEvents: Flow<AssistantActionType> = _navigationEvents.receiveAsFlow()

    private var latestContext: AssistantContext? = null

    init {
        viewModelScope.launch {
            getAssistantContext(Unit).collect { result ->
                when (result) {
                    is NovaResult.Success -> onContextLoaded(result.data)
                    is NovaResult.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.failure.message)
                    }
                    is NovaResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isNotEmpty()) sendQuery(text)
    }

    fun onSuggestionClick(prompt: AssistantSuggestedPrompt) = sendQuery(prompt.query)

    fun onActionClick(action: AssistantAction) {
        viewModelScope.launch { _navigationEvents.send(action.type) }
    }

    private fun onContextLoaded(context: AssistantContext) {
        latestContext = context
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                errorMessage = null,
                suggestedPrompts = insightEngine.suggestedPrompts(context),
                messages = state.messages.ifEmpty { listOf(insightEngine.greeting(context)) }
            )
        }
    }

    private fun sendQuery(text: String) {
        val context = latestContext ?: return
        val userMessage = AssistantMessage(id = UUID.randomUUID().toString(), sender = AssistantSender.USER, text = text)
        val reply = insightEngine.respond(text, context)
        _uiState.update { it.copy(messages = it.messages + userMessage + reply, inputText = "") }
    }
}

@Immutable
data class AssistantUiState(
    val isLoading: Boolean = true,
    val messages: List<AssistantMessage> = emptyList(),
    val suggestedPrompts: List<AssistantSuggestedPrompt> = emptyList(),
    val inputText: String = "",
    val errorMessage: String? = null
)
