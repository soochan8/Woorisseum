package com.woorisseum.core.domain.shopping

import kotlin.time.Instant

/** 장보기를 마친 시점의 구매 항목이다. 현재 목록의 후속 변경과 독립적인 기록이다. */
data class ShoppingTrip(val id: String, val completedAt: Instant, val items: List<ShoppingItem>) {
    init {
        require(id.isNotBlank())
        require(items.isNotEmpty() && items.all { it.purchased })
        require(items.map { it.id }.distinct().size == items.size)
    }
}

data class ShoppingCompletion(val trip: ShoppingTrip, val remainingItems: List<ShoppingItem>)

/** 확인한 대상 중 아직 구매 완료인 항목만 옮긴다. 새로 체크된 항목과 미구매 항목은 남긴다. */
fun completeShopping(
    items: List<ShoppingItem>,
    confirmedIds: Set<String>,
    tripId: String,
    completedAt: Instant,
): ShoppingCompletion? {
    val (completed, remaining) = items.partition { it.purchased && it.id in confirmedIds }
    if (completed.isEmpty()) return null
    return ShoppingCompletion(ShoppingTrip(tripId, completedAt, completed), remaining)
}
