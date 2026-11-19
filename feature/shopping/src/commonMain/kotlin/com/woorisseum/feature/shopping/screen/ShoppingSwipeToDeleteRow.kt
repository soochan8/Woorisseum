package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.shopping.resources.*
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private enum class RevealPosition { Closed, Open }

/** 스와이프는 버튼만 노출한다. 삭제는 버튼·접근성 동작으로 명시적으로 요청할 때만 수행한다. */
@Composable
internal fun ShoppingSwipeToDeleteRow(
    itemId: String,
    revealed: Boolean,
    onReveal: (String?) -> Unit,
    deleteLabel: String,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val actionWidth = 80.dp
    val widthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val drag = remember(itemId, widthPx) {
        AnchoredDraggableState(RevealPosition.Closed, DraggableAnchors {
            RevealPosition.Closed at 0f
            RevealPosition.Open at -widthPx
        })
    }
    val currentRevealed by rememberUpdatedState(revealed)
    val currentOnReveal by rememberUpdatedState(onReveal)
    LaunchedEffect(revealed, drag) {
        drag.animateTo(if (revealed) RevealPosition.Open else RevealPosition.Closed)
    }
    LaunchedEffect(drag) {
        snapshotFlow { drag.settledValue }.distinctUntilChanged().collect { position ->
            when (position) {
                RevealPosition.Open -> currentOnReveal(itemId)
                RevealPosition.Closed -> if (currentRevealed) currentOnReveal(null)
            }
        }
    }
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        if (drag.offset < -0.5f || revealed) {
            Box(Modifier.matchParentSize()) {
                TextButton(onClick = onDelete,
                    modifier = Modifier.align(Alignment.CenterEnd).width(actionWidth).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error)
                        .semantics { contentDescription = deleteLabel }) {
                    Text(stringResource(Res.string.shopping_delete), color = MaterialTheme.colorScheme.onError)
                }
            }
        }
        Box(Modifier.fillMaxWidth()
            .offset { IntOffset((if (drag.offset.isNaN()) 0f else drag.offset).roundToInt(), 0) }
            .background(MaterialTheme.colorScheme.background)
            .anchoredDraggable(drag, orientation = Orientation.Horizontal)
            .semantics { customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete(); true }) }) {
            content()
        }
    }
}
