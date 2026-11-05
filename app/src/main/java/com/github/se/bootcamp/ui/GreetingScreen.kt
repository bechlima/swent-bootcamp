package com.github.se.bootcamp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.se.bootcamp.ui.theme.BootcampTheme

object GreetingScreenTestTags {
  const val NAME_INPUT = "GreetingScreenNameInput"
  const val BUTTON = "GreetingScreenButton"
  const val GREETING_MESSAGE = "GreetingScreenGreetingMessage"
}

/**
 * GreetingScreen composable that displays a text field to input a name, a button to validate the
 * name and a greeting message.
 */
@Composable
fun GreetingScreen(modifier: Modifier = Modifier) {
  var name by remember { mutableStateOf("") }
  var displayedName by remember { mutableStateOf("") }
  var confirmed by remember { mutableStateOf(false) }

  Surface(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
    ) {
      Text(
          text = if (!confirmed) "What's your name ?" else "Hi $displayedName",
          modifier = Modifier.testTag(GreetingScreenTestTags.GREETING_MESSAGE),
      )

      TextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          modifier = Modifier.testTag(GreetingScreenTestTags.NAME_INPUT),
      )
      Button(
          onClick = {
            if (name.isNotBlank()) {
              confirmed = true
              displayedName = name
            }
          },
          modifier = Modifier.testTag(GreetingScreenTestTags.BUTTON),
      ) {
        Text("Confirm")
      }
    }
  }
}

// Switch to the `Split` view to see the preview alongside the code.
@Preview
@Composable
fun GreetingScreenPreview() {
  BootcampTheme { GreetingScreen() }
}
