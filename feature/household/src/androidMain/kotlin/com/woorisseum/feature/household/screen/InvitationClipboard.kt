package com.woorisseum.feature.household.screen

actual fun invitationClipEntry(text: String) = androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText("", text))
