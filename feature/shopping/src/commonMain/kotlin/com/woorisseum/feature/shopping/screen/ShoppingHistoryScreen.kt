package com.woorisseum.feature.shopping.screen

import com.woorisseum.core.designsystem.component.WoorisseumTextButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woorisseum.core.domain.shopping.ShoppingTrip
import com.woorisseum.feature.shopping.resources.*
import com.woorisseum.feature.shopping.viewmodel.ShoppingListViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 목록 진입의 ViewModel을 공유한다. 화면 이동 인자로 전체 목록이나 ViewModel을 전달하지 않는다. */
@Composable
fun ShoppingHistoryRoute(onBack: () -> Unit, viewModelStoreOwner: ViewModelStoreOwner) {
    val viewModel: ShoppingListViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShoppingHistoryScreen(state.trips, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingHistoryScreen(trips: List<ShoppingTrip>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.shopping_history_title)) },
            navigationIcon = { WoorisseumTextButton(onClick = onBack) { Text(stringResource(Res.string.shopping_back)) } }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "notice") { Text(stringResource(Res.string.shopping_history_notice),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (trips.isEmpty()) item(key = "empty") { Text(stringResource(Res.string.shopping_history_empty)) }
            trips.forEach { trip ->
                item(key = "trip:${trip.id}") {
                    val date = trip.completedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HorizontalDivider()
                        Text(stringResource(Res.string.shopping_history_date, date.year, date.month.ordinal + 1, date.day,
                            date.hour.toString().padStart(2, '0'), date.minute.toString().padStart(2, '0')),
                            style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.shopping_history_item_count, trip.items.size),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(trip.items, key = { "trip:${trip.id}:item:${it.id}" }) { item ->
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(item.name, Modifier.weight(1f))
                            item.quantity?.let { Text(it, Modifier.widthIn(max = 120.dp)) }
                        }
                        Text(shoppingCategoryLabel(item.category), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        item.memo?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}
