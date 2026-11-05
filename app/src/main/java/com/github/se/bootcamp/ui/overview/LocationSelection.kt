// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.overview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.ui.navigation.NavigationTestTags

private const val LOCATION_PLACEHOLDER = "Enter an Address or Location"

@Composable
fun LocationSelection(
    query: String,
    suggestions: List<Location>,
    onQueryChange: (String) -> Unit,
    onLocationSelected: (Location) -> Unit,
    testTag: String,
    suggestionTag: String,
) {
  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(LOCATION_PLACEHOLDER) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
    )
    DropdownMenu(
        expanded = suggestions.isNotEmpty(),
        onDismissRequest = {},
    ) {
      suggestions.take(5).forEach { location ->
        DropdownMenuItem(
            text = { Text(location.name) },
            onClick = { onLocationSelected(location) },
            modifier = Modifier.testTag(suggestionTag),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTopBar(title: String, onBack: () -> Unit) {
  TopAppBar(
      title = { Text(title, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)) },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
      },
  )
}
