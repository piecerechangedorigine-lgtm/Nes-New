package com.novafinance.feature.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.AssistantAction
import com.novafinance.core.domain.model.AssistantActionType
import com.novafinance.core.domain.model.AssistantMessage
import com.novafinance.core.domain.model.AssistantSender
import com.novafinance.core.domain.model.AssistantSuggestedPrompt

/**
 * Route-level composable wired into navigation. Delegates to the
 * stateless [AssistantScreen] so the screen itself stays trivially previewable.
 */
@Composable
fun AssistantRoute(
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { type ->
            when (type) {
                AssistantActionType.OPEN_BUDGETS -> onOpenBudgets()
                AssistantActionType.OPEN_GOALS -> onOpenGoals()
                AssistantActionType.OPEN_ANALYTICS -> onOpenAnalytics()
                AssistantActionType.ADD_TRANSACTION -> onAddTransaction()
                AssistantActionType.OPEN_DEBT -> onOpenGoals()
            }
        }
    }

    AssistantScreen(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onSuggestionClick = viewModel::onSuggestionClick,
        onActionClick = viewModel::onActionClick
    )
}

@Composable
private fun AssistantScreen(
    uiState: AssistantUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (AssistantSuggestedPrompt) -> Unit,
    onActionClick: (AssistantAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Nova.colors.background)
    ) {
        when {
            uiState.isLoading && uiState.messages.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Nova.colors.primary
                )
            }

            uiState.errorMessage != null && uiState.messages.isEmpty() -> {
                NovaEmptyState(
                    icon = NovaIcons.Close,
                    title = "Couldn't load your assistant",
                    message = uiState.errorMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    AssistantHeader()
                    ChatTranscript(
                        messages = uiState.messages,
                        onActionClick = onActionClick,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.suggestedPrompts.isNotEmpty()) {
                        SuggestionRow(
                            prompts = uiState.suggestedPrompts,
                            onSuggestionClick = onSuggestionClick
                        )
                    }
                    InputBar(
                        text = uiState.inputText,
                        onTextChange = onInputChange,
                        onSend = onSend
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Nova.spacing.screenHorizontal, vertical = Nova.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Nova.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = NovaIcons.Sparkle,
                contentDescription = null,
                tint = Nova.colors.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(text = "Nova Assistant", style = Nova.typography.titleLarge, color = Nova.colors.textPrimary)
    }
}

@Composable
private fun ChatTranscript(
    messages: List<AssistantMessage>,
    onActionClick: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Nova.spacing.screenHorizontal, vertical = Nova.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.md)
    ) {
        items(messages, key = { it.id }) { message ->
            ChatBubble(message = message, onActionClick = onActionClick)
        }
    }
}

@Composable
private fun ChatBubble(message: AssistantMessage, onActionClick: (AssistantAction) -> Unit) {
    val isUser = message.sender == AssistantSender.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(if (isUser) Nova.shapes.large else Nova.shapes.medium)
                    .background(if (isUser) Nova.colors.primary else Nova.colors.elevatedSurface)
                    .padding(horizontal = Nova.spacing.md, vertical = Nova.spacing.sm)
            ) {
                Text(
                    text = message.text,
                    style = Nova.typography.bodyMedium,
                    color = if (isUser) Nova.colors.onPrimary else Nova.colors.textPrimary
                )
            }

            if (message.actions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs),
                    modifier = Modifier.padding(top = Nova.spacing.xs)
                ) {
                    message.actions.forEach { action ->
                        ActionChip(action = action, onClick = { onActionClick(action) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(action: AssistantAction, onClick: () -> Unit) {
    Text(
        text = action.label,
        style = Nova.typography.labelLarge,
        color = Nova.colors.primary,
        modifier = Modifier
            .clip(Nova.shapes.full)
            .background(Nova.colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = Nova.spacing.md, vertical = Nova.spacing.xs)
    )
}

@Composable
private fun SuggestionRow(prompts: List<AssistantSuggestedPrompt>, onSuggestionClick: (AssistantSuggestedPrompt) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Nova.spacing.screenHorizontal, vertical = Nova.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
    ) {
        prompts.forEach { prompt ->
            Text(
                text = prompt.label,
                style = Nova.typography.labelLarge,
                color = Nova.colors.textPrimary,
                modifier = Modifier
                    .clip(Nova.shapes.full)
                    .background(Nova.colors.elevatedSurfaceHigh)
                    .clickable { onSuggestionClick(prompt) }
                    .padding(horizontal = Nova.spacing.lg, vertical = Nova.spacing.sm)
            )
        }
    }
}

@Composable
private fun InputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Nova.spacing.screenHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
    ) {
        NovaTextField(
            value = text,
            onValueChange = onTextChange,
            label = "Ask about your money",
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onSend,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (text.isNotBlank()) Nova.colors.primary else Nova.colors.elevatedSurfaceHigh)
        ) {
            Icon(
                imageVector = NovaIcons.Send,
                contentDescription = "Send",
                tint = if (text.isNotBlank()) Nova.colors.onPrimary else Nova.colors.textDisabled
            )
        }
    }
}
