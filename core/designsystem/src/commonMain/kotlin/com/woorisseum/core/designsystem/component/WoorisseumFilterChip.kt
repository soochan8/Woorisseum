package com.woorisseum.core.designsystem.component

import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 분류 정책 없이 선택 상태·클릭·표시 내용만 전달받는 필터다. */
@Composable
fun WoorisseumFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
    )
}
