package com.woorisseum.core.domain.shopping

/** 선택 분류다. 미지정(null)과 명시적으로 선택한 기타는 구분한다. */
enum class ShoppingCategory {
    PRODUCE,
    MEAT_AND_SEAFOOD,
    DAIRY_AND_EGGS,
    PANTRY,
    HOUSEHOLD,
    OTHER,
}
