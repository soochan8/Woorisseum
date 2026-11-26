package com.woorisseum.invitation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberInvitationShare(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) { { text ->
        if (controller.presentedViewController == null) {
            val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
            sheet.popoverPresentationController?.apply {
                sourceView = controller.view
                sourceRect = controller.view.bounds
            }
            controller.presentViewController(sheet, animated = true, completion = null)
        }
    } }
}
