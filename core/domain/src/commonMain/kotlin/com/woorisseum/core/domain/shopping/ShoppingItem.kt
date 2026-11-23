package com.woorisseum.core.domain.shopping

/** 장보기 한 항목이다. 구매 체크는 결제·지출 등록을 의미하지 않는다. */
data class ShoppingItem(
    val id: String,
    val name: String,
    val quantity: String? = null,
    val memo: String? = null,
    val purchased: Boolean = false,
    val category: ShoppingCategory? = null,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank() && name == name.trim())
        require(quantity == null || quantity.isNotBlank())
        require(memo == null || memo.isNotBlank())
    }
}

/** 필드별 변경을 구분한다. 서버 연결 시 버전·충돌 정책은 요청 경계에서 별도로 처리한다. */
sealed interface ShoppingItemChange {
    data class Rename(val name: String) : ShoppingItemChange
    data class ChangeQuantity(val quantity: String?) : ShoppingItemChange
    data class ChangeMemo(val memo: String?) : ShoppingItemChange
    data class SetPurchased(val purchased: Boolean) : ShoppingItemChange
    data class ChangeCategory(val category: ShoppingCategory?) : ShoppingItemChange
}

/** 다른 필드·식별자를 유지한다. 토글 대신 목표 상태를 전달해 중복 적용 시 뒤집히지 않는다. */
fun ShoppingItem.changed(change: ShoppingItemChange): ShoppingItem = when (change) {
    is ShoppingItemChange.Rename -> copy(name = change.name.trim())
    is ShoppingItemChange.ChangeQuantity -> copy(quantity = change.quantity?.trim()?.takeIf { it.isNotEmpty() })
    is ShoppingItemChange.ChangeMemo -> copy(memo = change.memo?.trim()?.takeIf { it.isNotEmpty() })
    is ShoppingItemChange.SetPurchased -> copy(purchased = change.purchased)
    is ShoppingItemChange.ChangeCategory -> copy(category = change.category)
}

/** 구매 완료 항목도 포함한다. 대소문자·내부 공백이 다르면 다른 이름이며 자동 병합하지 않는다. */
fun findDuplicateShoppingItem(items: List<ShoppingItem>, name: String, excludingId: String? = null): ShoppingItem? =
    items.firstOrNull { it.id != excludingId && it.name == name.trim() }
