package com.apksinc.monitor.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.Habit
import com.apksinc.monitor.domain.HomeSummary
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.ErrorState
import com.apksinc.monitor.ui.components.HabitRow
import com.apksinc.monitor.ui.components.LinearProgress
import com.apksinc.monitor.ui.components.LoadingState
import com.apksinc.monitor.ui.components.MetricStatCard
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.theme.OmgSincTheme
import java.time.LocalTime

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmgSincTheme.colors.background),
    ) {
        when (val current = state) {
            is HomeUiState.Loading -> LoadingState()
            is HomeUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::refresh)
            is HomeUiState.Success -> HomeContent(
                summary = current.summary,
                onToggleHabit = { habit -> viewModel.toggleHabit(habit.id, habit.todayCompleted) },
            )
        }
    }
}

@Composable
private fun HomeContent(summary: HomeSummary, onToggleHabit: (Habit) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { GreetingHeader(name = summary.greetingName) }
        item { ProgressCard(summary) }
        item { NextActionCard(summary.nextActionMessage) }

        if (summary.metrics.any { it.value != null }) {
            item {
                Column {
                    SectionLabel("Saude e bem-estar")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        summary.metrics.filter { it.value != null }.take(3).forEach { snapshot ->
                            MetricStatCard(snapshot = snapshot, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item { SectionLabel("Habitos de hoje  ·  ${summary.habitsDoneToday}/${summary.habitsTotalToday}") }

        if (summary.habits.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.Checklist,
                    title = "Nenhum habito ainda",
                    message = "Adicione seu primeiro habito para comecar a acompanhar seu dia.",
                )
            }
        } else {
            items(summary.habits, key = { it.id }) { habit ->
                HabitRow(habit = habit, onToggle = { onToggleHabit(habit) })
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String) {
    val firstName = name.trim().substringBefore(" ").ifBlank { name }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    Column {
        Text(
            text = "$greeting, $firstName",
            style = MaterialTheme.typography.headlineMedium,
            color = OmgSincTheme.colors.textPrimary,
        )
        Text(
            text = "Aqui esta como voce esta indo hoje",
            style = MaterialTheme.typography.bodyMedium,
            color = OmgSincTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun ProgressCard(summary: HomeSummary) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Progresso da semana",
                    style = MaterialTheme.typography.titleSmall,
                    color = OmgSincTheme.colors.textSecondary,
                )
                DeltaPill(delta = summary.weeklyProgressDeltaPct)
            }

            Text(
                text = "${formatPct(summary.weeklyProgressPct)}%",
                style = MaterialTheme.typography.displayMedium,
                color = OmgSincTheme.colors.textPrimary,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = weeklyProgressMessage(summary.weeklyProgressDeltaPct),
                style = MaterialTheme.typography.bodyMedium,
                color = OmgSincTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )

            LinearProgress(progress = (summary.weeklyProgressPct / 100).toFloat())
        }
    }
}

@Composable
private fun DeltaPill(delta: Double) {
    val positive = delta >= 0
    val color = if (positive) OmgSincTheme.colors.success else OmgSincTheme.colors.warning
    val soft = if (positive) OmgSincTheme.colors.successSoft else OmgSincTheme.colors.warningSoft
    val sign = if (positive) "+" else ""
    Box(
        modifier = Modifier
            .background(soft, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$sign${formatPct(delta)}%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NextActionCard(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.accentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.TipsAndUpdates,
                contentDescription = null,
                tint = OmgSincTheme.colors.accent,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OmgSincTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

private fun weeklyProgressMessage(delta: Double): String = when {
    delta > 0.5 -> "Voce esta ${formatPct(delta)}% melhor que na semana passada."
    delta < -0.5 -> "Um pouco abaixo da semana passada - sem problema, hoje e um novo dia."
    else -> "No mesmo ritmo da semana passada."
}

private fun formatPct(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)
