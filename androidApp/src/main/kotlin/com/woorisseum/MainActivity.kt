package com.woorisseum

import android.os.Bundle
import android.content.Intent
import com.woorisseum.invitation.InvitationLinks
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        receiveInvitation(intent)
        setContent {
            App()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        receiveInvitation(intent)
        setIntent(intent)
    }

    private fun receiveInvitation(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) intent.dataString?.let(InvitationLinks::open)
        intent.data = null
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}