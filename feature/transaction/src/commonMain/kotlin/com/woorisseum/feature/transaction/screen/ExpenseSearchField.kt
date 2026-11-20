package com.woorisseum.feature.transaction.screen

import com.woorisseum.core.designsystem.theme.WoorisseumSpacing
import com.woorisseum.core.designsystem.component.WoorisseumTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.woorisseum.feature.transaction.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpenseSearchField(
    query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit,
    requestFocus: Boolean, onFocusRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
            onFocusRequested()
        }
    }
    Column(modifier.padding(horizontal = WoorisseumSpacing.screenHorizontal), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WoorisseumTextField(query, onQueryChange,
            label = { Text(stringResource(Res.string.expense_search_placeholder)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus(); keyboard?.hide() }),
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = onClear) {
                    Icon(painterResource(Res.drawable.ic_close), stringResource(Res.string.expense_search_clear))
                }
            })
    }
}
