package com.woorisseum.feature.transaction.model

import com.woorisseum.core.domain.common.AppError

/** 저장소 오류를 화면에서 처리할 수 있는 의미로 구분한다. */
enum class ExpenseOperationError { STORAGE, NOT_FOUND, CONFLICT, INVALID_INPUT }

internal fun AppError.toExpenseOperationError(): ExpenseOperationError = when (this) {
    is AppError.Invitation -> ExpenseOperationError.STORAGE
    is AppError.Remote -> ExpenseOperationError.STORAGE
    AppError.Storage -> ExpenseOperationError.STORAGE
    AppError.NotFound -> ExpenseOperationError.NOT_FOUND
    AppError.Conflict -> ExpenseOperationError.CONFLICT
    AppError.InvalidInput -> ExpenseOperationError.INVALID_INPUT
}
