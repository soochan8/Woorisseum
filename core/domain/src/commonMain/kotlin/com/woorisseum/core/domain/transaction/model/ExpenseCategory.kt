package com.woorisseum.core.domain.transaction.model

/** 지출을 고정비·변동비·충동 중 하나로 분류한다. 화면에 표시할 이름은 UI 리소스로 관리한다. */
enum class ExpenseCategory {
    FIXED,
    VARIABLE,
    IMPULSE,
}
