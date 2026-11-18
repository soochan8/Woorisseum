package com.woorisseum.core.domain.transaction.model

/** 일반 지출 금액의 업무 규칙이다. 환불·취소는 별도 정책으로 다룬다. */
fun isValidExpenseAmount(amount: Long): Boolean = amount > 0L
