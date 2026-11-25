package com.woorisseum.feature.transaction.model

/** 표시용 값만 받는다. 개인별 집계·축 상한 계산은 데이터 연결 시 화면 밖에서 담당한다. */
data class ExpenseComparisonUiModel(
    val first: ExpenseComparisonPersonUiModel,
    val second: ExpenseComparisonPersonUiModel,
    val maximumAmountText: String,
    val midpointAmountText: String,
)

data class ExpenseComparisonPersonUiModel(
    val name: String,
    val emoji: String,
    val amountText: String,
    /** 두 사람에게 같은 축을 적용한 0~1 사이의 막대 높이다. */
    val heightFraction: Float,
) {
    init { require(heightFraction.isFinite() && heightFraction in 0f..1f) }
}
