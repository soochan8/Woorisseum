package com.woorisseum.core.data.transaction

import com.woorisseum.core.data.local.ExpenseEntity
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.model.ExpenseCategory
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import kotlinx.datetime.LocalDate

/** enum 순서·표시명과 독립적인 저장 코드를 사용한다. */
internal fun ExpenseCategory.toStorageCode(): String = when (this) {
    ExpenseCategory.FIXED -> "FIXED"
    ExpenseCategory.VARIABLE -> "VARIABLE"
    ExpenseCategory.IMPULSE -> "IMPULSE"
}

internal fun Expense.toEntity(): ExpenseEntity {
    val card = paymentMethod as? PaymentMethod.Card
    return ExpenseEntity(
        id = id, amount = amount, dateEpochDay = date.toEpochDays(), merchant = merchant,
        category = category.toStorageCode(),
        paymentType = when (paymentMethod) {
            PaymentMethod.Cash -> "CASH"
            PaymentMethod.BankTransfer -> "BANK_TRANSFER"
            is PaymentMethod.Card -> "CARD"
        },
        cardId = card?.cardId, cardName = card?.name, cardIssuer = card?.issuer,
        memo = memo, revision = revision,
    )
}

/** 알 수 없는 저장 코드는 다른 결제수단으로 바꾸지 않고 조회 실패로 처리한다. */
internal fun ExpenseEntity.toDomain(): Expense {
    val category = when (category) {
        "FIXED" -> ExpenseCategory.FIXED
        "VARIABLE" -> ExpenseCategory.VARIABLE
        "IMPULSE" -> ExpenseCategory.IMPULSE
        else -> throw InvalidExpenseRecord()
    }
    val paymentMethod = when (paymentType) {
        "CASH" -> PaymentMethod.Cash
        "BANK_TRANSFER" -> PaymentMethod.BankTransfer
        "CARD" -> PaymentMethod.Card(
            cardId = cardId ?: throw InvalidExpenseRecord(),
            name = cardName ?: throw InvalidExpenseRecord(),
            issuer = cardIssuer ?: throw InvalidExpenseRecord(),
        )
        else -> throw InvalidExpenseRecord()
    }
    val date = try { LocalDate.fromEpochDays(dateEpochDay) }
        catch (_: IllegalArgumentException) { throw InvalidExpenseRecord() }
    return Expense(id, amount, date, merchant, category, paymentMethod, memo, revision)
}

/** 저장 형식을 도메인 타입으로 복원할 수 없을 때 쓰는 data 내부 오류다. */
internal class InvalidExpenseRecord : IllegalStateException()
