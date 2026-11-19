package com.woorisseum.feature.transaction.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth

/** 월별 목록의 날짜 필터와 독립적인 검색 기간이다. */
enum class ExpenseSearchPreset { ALL, THIS_MONTH, RECENT_THREE_MONTHS, THIS_YEAR, CUSTOM }

data class ExpenseSearchPeriod(
    val preset: ExpenseSearchPreset = ExpenseSearchPreset.ALL,
    val range: ExpenseDateRange? = null,
) {
    init { require((preset == ExpenseSearchPreset.ALL) == (range == null)) }
}

/** 빠른 기간은 달력 기준이다. 현재 달을 포함한 최근 3개 월·올해 전체를 선택한다. */
internal fun ExpenseSearchPreset.toPeriod(today: LocalDate): ExpenseSearchPeriod {
    val month = YearMonth(today.year, today.month)
    val range = when (this) {
        ExpenseSearchPreset.ALL -> null
        ExpenseSearchPreset.THIS_MONTH -> ExpenseDateRange(month.firstDay, month.lastDay)
        ExpenseSearchPreset.RECENT_THREE_MONTHS -> ExpenseDateRange(month.minusMonth().minusMonth().firstDay, month.lastDay)
        ExpenseSearchPreset.THIS_YEAR -> ExpenseDateRange(LocalDate(today.year, 1, 1), LocalDate(today.year, 12, 31))
        ExpenseSearchPreset.CUSTOM -> error("직접 선택에는 시작일과 종료일이 필요합니다")
    }
    return ExpenseSearchPeriod(this, range)
}
