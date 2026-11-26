package com.woorisseum.feature.transaction.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.feature.transaction.model.ExpenseComparisonPersonUiModel
import com.woorisseum.feature.transaction.model.ExpenseComparisonUiModel
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.stringArrayResource
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.roundToInt

/** 월 전체 비교 영역이다. 사용자별 정보가 없으면 이모지 후보를 소개하고 실제 금액을 만들지 않는다. */
@Composable
internal fun ExpenseComparisonSection(comparison: ExpenseComparisonUiModel?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = WoorisseumSpacing.screenHorizontal)
        .padding(top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.expense_comparison_title), style = MaterialTheme.typography.titleMedium)
        if (comparison == null) {
            BouncingEmojiPrompt()
        } else {
            ExpenseComparisonChart(comparison)
        }
    }
}

/** 장식용 모션은 화면 안에서만 관리하고, 화면을 벗어나면 중단한다. */
@Composable
private fun BouncingEmojiPrompt() {
    val emojis = stringArrayResource(Res.array.expense_comparison_emoji_candidates)
    val phases = remember(emojis) { List(emojis.size) { Animatable(0f) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val jumpHeight = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(lifecycle, phases) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            phases.forEach { it.snapTo(0f) }
            if (currentCoroutineContext()[MotionDurationScale]?.scaleFactor == 0f) return@repeatOnLifecycle
            coroutineScope {
                phases.forEach { phase ->
                    launch {
                        // 각 이모지가 독립적으로 쉬고 점프한다. 화면상 순서와 시작 시점을 연결하지 않는다.
                        while (isActive) {
                            delay(Random.nextLong(400L, 2400L))
                            if (currentCoroutineContext()[MotionDurationScale]?.scaleFactor == 0f) break
                            phase.animateTo(1f, tween(Random.nextInt(420, 720), easing = LinearEasing))
                            phase.snapTo(0f)
                        }
                    }
                }
            }
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            emojis.forEachIndexed { index, emoji ->
                Text(emoji, fontSize = 32.sp, lineHeight = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        // 한 주기 안에 짧게 점프한 뒤 쉰다. 측정 크기는 바꾸지 않아 목록이 흔들리지 않는다.
                        val progress = phases[index].value
                        translationY = -jumpHeight * sin(PI * progress).toFloat()
                    })
            }
        }
        Text(stringResource(Res.string.expense_comparison_emoji_title),
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text(stringResource(Res.string.expense_comparison_emoji_description),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

/** 이모지와 금액의 실제 측정 높이만큼 여백을 확보해 큰 글씨에서도 서로 겹치지 않는다. */
@Composable
private fun ExpenseComparisonChart(comparison: ExpenseComparisonUiModel) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val description = stringResource(Res.string.expense_comparison_description,
        comparison.first.name, comparison.first.amountText, comparison.second.name, comparison.second.amountText)
    Layout(
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        content = {
            AxisLabel(comparison.maximumAmountText)
            AxisLabel(comparison.midpointAmountText)
            AxisLabel(stringResource(Res.string.expense_comparison_zero))
            Canvas(Modifier) {
                listOf(0f, size.height / 2f, size.height - 1f).forEach { y ->
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            PersonLabel(comparison.first)
            PersonLabel(comparison.second)
            ComparisonBar(MaterialTheme.colorScheme.primaryContainer)
            ComparisonBar(MaterialTheme.colorScheme.tertiaryContainer)
            Text(comparison.first.name, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Text(comparison.second.name, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val axisLimit = (width * 0.23f).roundToInt()
        val axes = (0..2).map { measurables[it].measure(Constraints(maxWidth = axisLimit)) }
        val gap = 8.dp.roundToPx()
        val plotX = axes.maxOf { it.width } + gap
        val plotWidth = (width - plotX).coerceAtLeast(0)
        val slotWidth = plotWidth / 2
        val textLimit = (slotWidth - gap).coerceAtLeast(0)
        val avatars = (4..5).map { measurables[it].measure(Constraints(maxWidth = textLimit)) }
        val names = (8..9).map { measurables[it].measure(Constraints(maxWidth = textLimit)) }
        val plotTop = avatars.maxOf { it.height } + axes[0].height / 2 + gap
        val plotHeight = 132.dp.roundToPx()
        val baseline = plotTop + plotHeight
        val nameTop = baseline + 12.dp.roundToPx()
        val height = nameTop + names.maxOf { it.height }
        val grid = measurables[3].measure(Constraints.fixed(plotWidth, plotHeight))
        val fractions = listOf(comparison.first.heightFraction, comparison.second.heightFraction)
        val barWidth = minOf(64.dp.roundToPx(), (slotWidth * 0.58f).roundToInt())
        val bars = (6..7).mapIndexed { index, child ->
            measurables[child].measure(Constraints.fixed(barWidth, (plotHeight * fractions[index]).roundToInt()))
        }
        layout(width, height) {
            grid.placeRelative(plotX, plotTop)
            axes.forEachIndexed { index, label ->
                label.placeRelative(plotX - gap - label.width, plotTop + plotHeight * index / 2 - label.height / 2)
            }
            bars.forEachIndexed { index, bar ->
                val centerX = plotX + slotWidth * index + slotWidth / 2
                val barTop = baseline - bar.height
                bar.placeRelative(centerX - bar.width / 2, barTop)
                avatars[index].placeRelative(centerX - avatars[index].width / 2, barTop - avatars[index].height)
                names[index].placeRelative(centerX - names[index].width / 2, nameTop)
            }
        }
    }
}

@Composable
private fun AxisLabel(value: String) {
    Text(value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PersonLabel(person: ExpenseComparisonPersonUiModel) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.expense_amount_won, person.amountText),
            style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        // 기본 이모지 글리프를 사용하므로 Android·iOS에서 모양은 각 플랫폼의 글꼴을 따른다.
        Text(person.emoji, fontSize = 32.sp, lineHeight = 40.sp)
    }
}

@Composable
private fun ComparisonBar(color: Color) {
    Box(Modifier.background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
}
