package org.fdroid.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import org.fdroid.ui.utils.BackButton

@Composable
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
fun TopSearchBar(
  searchFieldState: TextFieldState = rememberTextFieldState(),
  actions: @Composable (RowScope.() -> Unit) = {},
  onSearch: suspend (String) -> Unit,
  onSearchCleared: () -> Unit,
  onHideSearch: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val isDark = isSystemInDarkTheme()
  val insets = WindowInsets.systemBars.asPaddingValues()

  Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            top = insets.calculateTopPadding() + 16.dp, 
            start = 8.dp, 
            end = 8.dp, 
            bottom = 12.dp
        ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BackButton(onClick = onHideSearch)
    Spacer(modifier = Modifier.width(4.dp))
    
    Box(modifier = Modifier
        .weight(1f)
        .padding(end = 8.dp)
        .background(if (isDark) Color(0xFF1E1E1E).copy(alpha=0.75f) else Color.White.copy(alpha=0.85f), RoundedCornerShape(24.dp))
        .border(1.dp, if (isDark) Color.White.copy(alpha=0.15f) else Color.Black.copy(alpha=0.1f), RoundedCornerShape(24.dp))
    ) {
      AppSearchInputField(
        searchBarState = rememberSearchBarState(),
        textFieldState = searchFieldState,
        onSearch = onSearch,
        onSearchCleared = {
          searchFieldState.setTextAndPlaceCursorAtEnd("")
          onSearchCleared()
        },
        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
      )
    }
  }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    keyboardController?.show()
  }
}
