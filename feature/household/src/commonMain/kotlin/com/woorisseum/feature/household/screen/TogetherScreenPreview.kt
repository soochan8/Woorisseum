package com.woorisseum.feature.household.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woorisseum.core.designsystem.theme.WoorisseumTheme
import com.woorisseum.feature.household.model.*
import com.woorisseum.core.domain.household.*

// 예시 상대 정보와 연결 결과는 이 파일에서만 만든다.
private fun invitation(status: InvitationStatus = InvitationStatus.Available) = TogetherUiState(
    step = TogetherStep.Confirmation, role = TogetherRole.Invitee,
    name = "수찬", emoji = TogetherEmoji.Cat,
    invitation = InvitationDisplay("지영", TogetherEmoji.Rabbit, status),
)

@Preview
@Composable
private fun TogetherStartPreview() = WoorisseumTheme {
    TogetherScreen(TogetherUiState(snapshot = TogetherSnapshot(null, null, null)), {}, {})
}

@Preview
@Composable
private fun TogetherInvitationPreview() = WoorisseumTheme {
    TogetherScreen(invitation(), {}, {})
}

@Preview
@Composable
private fun TogetherConnectedPreview() = WoorisseumTheme {
    TogetherScreen(invitation().copy(step = TogetherStep.Connected, snapshot = TogetherSnapshot(
        HouseholdProfile("preview-self", "수찬", ProfileEmoji.Cat, 1),
        Household("preview-home", listOf(HouseholdProfile("preview-self", "수찬", ProfileEmoji.Cat, 1),
            HouseholdProfile("preview-partner", "지영", ProfileEmoji.Rabbit, 1))), null)), {}, {})
}

@Preview
@Composable
private fun TogetherExpiredPreview() = WoorisseumTheme {
    TogetherScreen(invitation(InvitationStatus.Expired), {}, {})
}

@Preview
@Composable
private fun TogetherUsedPreview() = WoorisseumTheme {
    TogetherScreen(invitation(InvitationStatus.Used), {}, {})
}

@Preview
@Composable
private fun TogetherFullPreview() = WoorisseumTheme {
    TogetherScreen(invitation(InvitationStatus.Full), {}, {})
}

@Preview(widthDp = 320, fontScale = 1.5f)
@Composable
private fun TogetherProfileLargeTextPreview() = WoorisseumTheme {
    TogetherScreen(invitation().copy(step = TogetherStep.Profile, name = "", emoji = null, profileSubmitted = true), {}, {})
}
