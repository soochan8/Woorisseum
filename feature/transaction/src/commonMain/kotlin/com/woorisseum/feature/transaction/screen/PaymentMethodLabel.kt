package com.woorisseum.feature.transaction.screen

import androidx.compose.runtime.Composable
import com.woorisseum.core.domain.transaction.model.PaymentMethod
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun paymentLabel(payment: PaymentMethod): String = when (payment) {
    PaymentMethod.Cash -> stringResource(Res.string.expense_payment_cash)
    PaymentMethod.BankTransfer -> stringResource(Res.string.expense_payment_transfer)
    is PaymentMethod.Card -> if (payment.issuer.isBlank()) payment.name
        else stringResource(Res.string.card_payment_label, payment.name, payment.issuer)
}
