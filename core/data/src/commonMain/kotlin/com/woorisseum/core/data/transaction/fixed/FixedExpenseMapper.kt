package com.woorisseum.core.data.transaction.fixed

import com.woorisseum.core.data.local.FixedExpenseEntity
import com.woorisseum.core.data.local.FixedExpenseSnapshot
import com.woorisseum.core.data.local.FixedExpenseVersionEntity
import com.woorisseum.core.domain.transaction.fixed.*
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number

internal fun YearMonth.toStorageMonth(): Long = year.toLong() * 12 + month.number - 1
internal fun Long.toYearMonth(): YearMonth {
    val year = floorDiv(12L)
    if (year !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) throw InvalidFixedExpenseRecord()
    return YearMonth(year.toInt(), mod(12L).toInt() + 1)
}

internal fun FixedExpenseInput.toVersion(id: String, month: YearMonth) = FixedExpenseVersionEntity(
    id, month.toStorageMonth(), type.toStorageCode(), name?.trim()?.takeIf { it.isNotEmpty() },
    amount, billingDay, memo?.trim()?.takeIf { it.isNotEmpty() },
)

internal fun FixedExpenseSnapshot.toDomain(): FixedExpensePlan = try {
    FixedExpensePlan(planId, type.toFixedExpenseType(), name, amount, billingDay,
        fromMonth.toYearMonth(), memo, stoppedFromMonth?.toYearMonth(), revision)
} catch (_: IllegalArgumentException) { throw InvalidFixedExpenseRecord() }

internal fun FixedExpenseVersionEntity.toDomain(plan: FixedExpenseEntity): FixedExpensePlan =
    FixedExpenseSnapshot(planId, fromMonth, type, name, amount, billingDay, memo,
        plan.revision, plan.stoppedFromMonth).toDomain()

internal class InvalidFixedExpenseRecord : IllegalStateException()

private fun FixedExpenseType.toStorageCode(): String = when (this) {
    FixedExpenseType.RENT -> "RENT"
    FixedExpenseType.TELECOMMUNICATION -> "TELECOMMUNICATION"
    FixedExpenseType.SUBSCRIPTION -> "SUBSCRIPTION"
    FixedExpenseType.INSURANCE -> "INSURANCE"
    FixedExpenseType.UTILITIES -> "UTILITIES"
    FixedExpenseType.OTHER -> "OTHER"
}
private fun String.toFixedExpenseType(): FixedExpenseType = when (this) {
    "RENT" -> FixedExpenseType.RENT
    "TELECOMMUNICATION" -> FixedExpenseType.TELECOMMUNICATION
    "SUBSCRIPTION" -> FixedExpenseType.SUBSCRIPTION
    "INSURANCE" -> FixedExpenseType.INSURANCE
    "UTILITIES" -> FixedExpenseType.UTILITIES
    "OTHER" -> FixedExpenseType.OTHER
    else -> throw InvalidFixedExpenseRecord()
}
