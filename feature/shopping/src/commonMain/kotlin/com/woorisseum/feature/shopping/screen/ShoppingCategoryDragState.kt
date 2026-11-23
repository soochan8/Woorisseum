package com.woorisseum.feature.shopping.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.woorisseum.core.domain.shopping.ShoppingCategory
import com.woorisseum.core.domain.shopping.ShoppingCategoryDefinition

/** 드래그 표시만 보관한다. 실제 순서는 Intent를 통해 ViewModel이 소유한다. */
internal class ShoppingCategoryDragState(
    private val listState: LazyListState,
    private val categories: () -> List<ShoppingCategoryDefinition>,
    private val onMove: (ShoppingCategory, ShoppingCategory?) -> Unit,
) {
    var dragging: ShoppingCategory? by mutableStateOf(null)
        private set
    private var center by mutableFloatStateOf(0f)
    private var initialOffset = 0
    private var itemSize = 0

    val translation: Float get() {
        val id = dragging ?: return 0f
        val offset = listState.layoutInfo.visibleItemsInfo.find { it.key == id.id }?.offset ?: initialOffset
        return center - itemSize / 2f - offset
    }

    fun start(category: ShoppingCategory) {
        if (dragging != null) return
        val item = listState.layoutInfo.visibleItemsInfo.find { it.key == category.id } ?: return
        initialOffset = item.offset
        itemSize = item.size
        center = item.offset + item.size / 2f
        dragging = category
    }

    fun dragBy(distance: Float) {
        if (dragging == null) return
        val layout = listState.layoutInfo
        val ids = categories().map { it.category.id }.toSet()
        val rows = layout.visibleItemsInfo.filter { it.key in ids }
        // 첫·마지막 항목 밖의 안내문이나 빈 여백까지 행을 끌고 가지 않는다.
        val top = if (!listState.canScrollBackward) rows.minOfOrNull { it.offset } ?: layout.viewportStartOffset
            else layout.viewportStartOffset
        val bottom = if (!listState.canScrollForward) rows.maxOfOrNull { it.offset + it.size } ?: layout.viewportEndOffset
            else layout.viewportEndOffset
        val minCenter = top + itemSize / 2f
        val maxCenter = (bottom - itemSize / 2f).coerceAtLeast(minCenter)
        center = (center + distance).coerceIn(minCenter, maxCenter)
        moveIfNeeded()
    }

    fun stop() { dragging = null }

    fun moveIfNeeded() {
        val id = dragging ?: return
        val current = categories()
        val from = current.indexOfFirst { it.category == id }
        if (from < 0) { stop(); return }
        val visible = listState.layoutInfo.visibleItemsInfo
        val down = visible.filter { info ->
            val index = current.indexOfFirst { it.category.id == info.key }
            index > from && center > info.offset + info.size / 2f
        }.maxByOrNull { it.index }
        val up = visible.filter { info ->
            val index = current.indexOfFirst { it.category.id == info.key }
            index in 0 until from && center < info.offset + info.size / 2f
        }.minByOrNull { it.index }
        when {
            down != null -> {
                val target = current.indexOfFirst { it.category.id == down.key }
                onMove(id, current.getOrNull(target + 1)?.category)
            }
            up != null -> onMove(id, current.find { it.category.id == up.key }?.category)
        }
    }

    /** 가장자리에 가까울수록 스크롤한다. 프레임 시간에 비례시켜 기기 주사율의 영향을 줄인다. */
    fun scrollSpeed(edge: Float): Float {
        if (dragging == null) return 0f
        val layout = listState.layoutInfo
        val top = center - itemSize / 2f
        val bottom = center + itemSize / 2f
        return when {
            top < layout.viewportStartOffset + edge && listState.canScrollBackward ->
                -((layout.viewportStartOffset + edge - top) / edge).coerceIn(0f, 1f) * edge * 8
            bottom > layout.viewportEndOffset - edge && listState.canScrollForward ->
                ((bottom - layout.viewportEndOffset + edge) / edge).coerceIn(0f, 1f) * edge * 8
            else -> 0f
        }
    }
}

@Composable
internal fun rememberShoppingCategoryDragState(
    listState: LazyListState,
    categories: List<ShoppingCategoryDefinition>,
    onMove: (ShoppingCategory, ShoppingCategory?) -> Unit,
): ShoppingCategoryDragState {
    val currentCategories by rememberUpdatedState(categories)
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(listState) { ShoppingCategoryDragState(listState, { currentCategories }, { id, before -> currentOnMove(id, before) }) }
}
