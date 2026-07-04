package com.novafinance.feature.dashboard.studio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaSectionHeader
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.DashboardPreset
import com.novafinance.core.domain.model.DashboardWidgetConfig
import com.novafinance.core.domain.model.DreamBackground
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.GoalVisualizationMode
import com.novafinance.core.domain.model.WidgetCatalogCategory
import com.novafinance.core.domain.model.WidgetCatalogEntry
import com.novafinance.core.domain.model.WidgetSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardStudioRoute(
    onBack: () -> Unit,
    viewModel: DashboardStudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // A picked SAF uri only keeps resolving across process restarts
        // if the app explicitly takes a persistable permission grant —
        // without this, the background would silently stop loading the
        // next time the app is killed and reopened.
        context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        viewModel.onBackgroundSelected(DreamBackground.DeviceImage(uri.toString()))
    }

    DashboardStudioScreen(
        uiState = uiState,
        onBack = onBack,
        onApplyPreset = viewModel::onApplyPreset,
        onToggleHidden = viewModel::onToggleHidden,
        onRemove = viewModel::onRemove,
        onAddGoalWidget = viewModel::onAddGoalWidget,
        onAddWidget = viewModel::onAddWidget,
        onCatalogSearchChange = viewModel::onCatalogSearchChange,
        onMoveUp = viewModel::onMoveUp,
        onMoveDown = viewModel::onMoveDown,
        onResize = viewModel::onResize,
        onVisualizationModeChange = viewModel::onVisualizationModeChange,
        onPickBackground = { backgroundPickerLauncher.launch(arrayOf("image/*")) },
        onClearBackground = { viewModel.onBackgroundSelected(DreamBackground.None) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardStudioScreen(
    uiState: DashboardStudioUiState,
    onBack: () -> Unit,
    onApplyPreset: (DashboardPreset) -> Unit,
    onToggleHidden: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddGoalWidget: (GoalForecast) -> Unit,
    onAddWidget: (WidgetCatalogEntry) -> Unit,
    onCatalogSearchChange: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onResize: (String, WidgetSize) -> Unit,
    onVisualizationModeChange: (String, GoalVisualizationMode) -> Unit,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit
) {
    val layout = uiState.layout

    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NovaIcons.Close, contentDescription = "Back", tint = Nova.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Nova.colors.background,
                    titleContentColor = Nova.colors.textPrimary
                )
            )
        }
    ) { scaffoldPadding ->
        if (layout == null) return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Nova.colors.background),
            contentPadding = PaddingValues(
                horizontal = Nova.spacing.screenHorizontal,
                vertical = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(Nova.spacing.lg)
        ) {
            item {
                NovaSectionHeader(title = "Presets")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
                ) {
                    DashboardPreset.entries.forEach { preset ->
                        PresetChip(
                            preset = preset,
                            isActive = layout.activePreset == preset,
                            onClick = { onApplyPreset(preset) }
                        )
                    }
                }
                Text(
                    text = "Switching a preset rebuilds your widget list — customize freely afterward.",
                    style = Nova.typography.labelSmall,
                    color = Nova.colors.textTertiary
                )
            }

            item {
                NovaSectionHeader(title = "Background")
                NovaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (layout.background) {
                                is DreamBackground.None -> "No background set"
                                is DreamBackground.DeviceImage -> "Custom image set"
                                is DreamBackground.AiGenerated -> "AI-generated (coming soon)"
                            },
                            style = Nova.typography.bodyMedium,
                            color = Nova.colors.textPrimary
                        )
                        Row {
                            TextButton(onClick = onPickBackground) { Text("Choose image", color = Nova.colors.primary) }
                            if (layout.background !is DreamBackground.None) {
                                TextButton(onClick = onClearBackground) { Text("Clear", color = Nova.colors.textSecondary) }
                            }
                        }
                    }
                }
            }

            if (uiState.goalsWithoutWidgets.isNotEmpty()) {
                item {
                    NovaSectionHeader(title = "Add a goal widget")
                    Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                        uiState.goalsWithoutWidgets.forEach { forecast ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = forecast.goal.name, style = Nova.typography.bodyMedium, color = Nova.colors.textPrimary)
                                TextButton(onClick = { onAddGoalWidget(forecast) }) {
                                    Text("Add", color = Nova.colors.primary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                NovaSectionHeader(title = "Add a widget")
                Text(
                    text = "Includes widgets you've hidden — adding one back here restores it with its previous settings.",
                    style = Nova.typography.labelSmall,
                    color = Nova.colors.textTertiary
                )
                NovaTextField(
                    value = uiState.catalogQuery,
                    onValueChange = onCatalogSearchChange,
                    label = "Search widgets",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.catalogQuery.isNotBlank() && uiState.catalogResults.isEmpty()) {
                item {
                    Text(text = "No widgets match \"${uiState.catalogQuery}\".", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
                }
            } else {
                WidgetCatalogCategory.entries.forEach { category ->
                    val entriesInCategory = uiState.catalogResults.filter { it.category == category }
                    if (entriesInCategory.isNotEmpty()) {
                        item {
                            Text(text = category.displayName, style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                        }
                        items(entriesInCategory, key = { "catalog-${it.type}" }) { entry ->
                            CatalogEntryRow(entry = entry, onAdd = { onAddWidget(entry) })
                        }
                    }
                }
            }

            item {
                NovaSectionHeader(title = "Your widgets")
            }

            items(layout.widgets.sortedBy { it.order }, key = { it.id }) { widget ->
                WidgetConfigRow(
                    widget = widget,
                    onToggleHidden = { onToggleHidden(widget.id) },
                    onRemove = { onRemove(widget.id) },
                    onMoveUp = { onMoveUp(widget.id) },
                    onMoveDown = { onMoveDown(widget.id) },
                    onResize = { size -> onResize(widget.id, size) },
                    onVisualizationModeChange = { mode -> onVisualizationModeChange(widget.id, mode) }
                )
            }
        }
    }
}

@Composable
private fun CatalogEntryRow(entry: WidgetCatalogEntry, onAdd: () -> Unit) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.displayName, style = Nova.typography.bodyMedium, color = Nova.colors.textPrimary)
                Text(text = entry.description, style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
            }
            TextButton(onClick = onAdd) {
                Text("Add", color = Nova.colors.primary)
            }
        }
    }
}

@Composable
private fun PresetChip(preset: DashboardPreset, isActive: Boolean, onClick: () -> Unit) {
    Text(
        text = preset.displayName,
        style = Nova.typography.labelLarge,
        color = if (isActive) Nova.colors.onPrimary else Nova.colors.textPrimary,
        modifier = Modifier
            .background(
                if (isActive) Nova.colors.primary else Nova.colors.elevatedSurface,
                Nova.shapes.full
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Nova.spacing.lg, vertical = Nova.spacing.sm)
    )
}

@Composable
private fun WidgetConfigRow(
    widget: DashboardWidgetConfig,
    onToggleHidden: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onResize: (WidgetSize) -> Unit,
    onVisualizationModeChange: (GoalVisualizationMode) -> Unit
) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = widget.type.displayName, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
                if (!widget.isVisible) {
                    Text(text = "Hidden", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
                }
            }
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = NovaIcons.ChevronUp, contentDescription = "Move up", tint = Nova.colors.textSecondary)
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = NovaIcons.ChevronDown, contentDescription = "Move down", tint = Nova.colors.textSecondary)
                }
                IconButton(onClick = onToggleHidden) {
                    Icon(
                        imageVector = if (widget.isVisible) NovaIcons.EyeOff else NovaIcons.Eye,
                        contentDescription = if (widget.isVisible) "Hide" else "Show",
                        tint = Nova.colors.textSecondary
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(imageVector = NovaIcons.Close, contentDescription = "Remove", tint = Nova.colors.textTertiary)
                }
            }
        }

        Text(text = "Size", style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
        NovaChipRow(
            options = WidgetSize.entries.map { NovaChipOption(it, it.name.lowercase().replaceFirstChar(Char::uppercase)) },
            selected = widget.size,
            onSelect = onResize
        )

        if (widget.goalId != null) {
            Text(text = "Visualization", style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
            NovaChipRow(
                options = GoalVisualizationMode.entries.map { NovaChipOption(it, it.displayName) },
                selected = widget.visualizationMode,
                onSelect = onVisualizationModeChange
            )
        }
    }
}
