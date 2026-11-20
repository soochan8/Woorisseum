package com.woorisseum.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woorisseum.core.designsystem.theme.WoorisseumSpacing

/** 하단 실행 영역의 배경·여백·내비게이션 바 inset을 담당한다. 동작과 버튼은 호출자가 소유한다. */
@Composable
fun WoorisseumBottomActionBar(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = WoorisseumSpacing.screenHorizontal,
        vertical = WoorisseumSpacing.medium,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(contentPadding),
            content = content,
        )
    }
}
