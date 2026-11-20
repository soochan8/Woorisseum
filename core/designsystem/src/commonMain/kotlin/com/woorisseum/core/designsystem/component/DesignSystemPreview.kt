package com.woorisseum.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.core.designsystem.theme.amountUnit
import com.woorisseum.core.designsystem.theme.listAmount

@Preview(name = "디자인 시스템 · 라이트", widthDp = 360, locale = "ko")
@Composable
private fun DesignSystemLightPreview() {
    WoorisseumTheme(darkTheme = false) { DesignSystemSamples() }
}

@Preview(name = "디자인 시스템 · 다크", widthDp = 360, locale = "ko")
@Composable
private fun DesignSystemDarkPreview() {
    WoorisseumTheme(darkTheme = true) { DesignSystemSamples() }
}

@Preview(name = "디자인 시스템 · 작은 화면·큰 글씨", widthDp = 320, fontScale = 1.5f, locale = "ko")
@Composable
private fun DesignSystemLargeTextPreview() {
    WoorisseumTheme(darkTheme = false) { DesignSystemSamples() }
}

/** 예시 문구·데이터는 Preview에서만 사용한다. DI나 기능 모델에 의존하지 않는다. */
@Composable
private fun DesignSystemSamples() {
    var input by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(true) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(WoorisseumSpacing.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(WoorisseumSpacing.large),
            ) {
                Text("공통 디자인 시스템", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(WoorisseumSpacing.small)) {
                    val colors = MaterialTheme.colorScheme
                    listOf(
                        colors.primary,
                        colors.primaryContainer,
                        colors.secondary,
                        colors.onSurface,
                    ).forEach { color -> Box(Modifier.size(40.dp).background(color, MaterialTheme.shapes.small)) }
                }
                Text("지출 · 화면 제목", style = MaterialTheme.typography.headlineSmall)
                Text(buildAnnotatedString {
                    append("1,234,567")
                    withStyle(MaterialTheme.typography.amountUnit.toSpanStyle()) { append("원") }
                }, style = MaterialTheme.typography.displaySmall)
                Text("목록 금액 17,000원", style = MaterialTheme.typography.listAmount)
                Text("이번 달 지출 · 섹션 제목", style = MaterialTheme.typography.titleMedium)
                Text("본문과 목록에 사용하는 글자", style = MaterialTheme.typography.bodyLarge)
                Text("설명 문장은 넉넉한 줄 높이로 표시합니다. 글자가 커지면 자연스럽게 줄바꿈합니다.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("보조 정보와 안내", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("짧은 분류 표시", style = MaterialTheme.typography.labelSmall)
                WoorisseumPrimaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("사용할 수 없는 동작")
                }
                WoorisseumSecondaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("보조 동작") }
                WoorisseumSecondaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("보조 동작 비활성") }
                WoorisseumTextButton(onClick = {}) { Text("취소") }
                WoorisseumTextField(input, { input = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("항목 이름") }, singleLine = true)
                WoorisseumTextField("", {}, modifier = Modifier.fillMaxWidth(),
                    label = { Text("필수 입력") }, isError = true, singleLine = true,
                    supportingText = { Text("이름을 입력해 주세요") })
                WoorisseumTextField("변경할 수 없는 값", {}, modifier = Modifier.fillMaxWidth(),
                    label = { Text("비활성 입력") }, enabled = false, singleLine = true)
                WoorisseumTextField("여러 줄의 메모도 높이를 제한하지 않고 표시합니다.", {},
                    modifier = Modifier.fillMaxWidth(), label = { Text("메모") }, minLines = 2)
                Column {
                    WoorisseumFilterChip(selected, { selected = !selected }, label = { Text("선택형 필터") })
                    WoorisseumFilterChip(false, {}, label = { Text("미선택") })
                    WoorisseumFilterChip(true, {}, label = { Text("사용 불가") }, enabled = false)
                }
            }
            WoorisseumBottomActionBar {
                WoorisseumPrimaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("확인") }
            }
        }
    }
}
