package com.woorisseum.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.woorisseum.resources.*
import org.jetbrains.compose.resources.stringResource

/** 업무 상태를 소유하지 않는 앱 메뉴다. 기능 화면은 shared의 이동 콜백으로 연다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManagementScreen(onOpenCards: () -> Unit, onOpenFixedExpenses: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.nav_management), style = MaterialTheme.typography.headlineSmall) }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 16.dp)) {
            item(key = "cards") {
                ListItem(headlineContent = { Text(stringResource(Res.string.management_cards)) },
                    supportingContent = { Text(stringResource(Res.string.management_cards_description)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpenCards))
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            item(key = "fixed-expenses") {
                ListItem(headlineContent = { Text(stringResource(Res.string.management_fixed)) },
                    supportingContent = { Text(stringResource(Res.string.management_fixed_description)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onOpenFixedExpenses))
            }
        }
    }
}
