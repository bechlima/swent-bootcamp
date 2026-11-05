package com.github.se.bootcamp.ui.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.bootcamp.R

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

@Composable
fun SignInScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    signInViewModel: SignInViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by signInViewModel.uiState.collectAsState()

  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Image(
        painter = painterResource(R.drawable.app_logo),
        contentDescription = "App logo",
        modifier = Modifier.testTag(SignInScreenTestTags.APP_LOGO),
    )
    Text("Sign in", modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_TITLE))
    Button(
        onClick = { signInViewModel.signIn(context, credentialManager) },
        enabled = !uiState.isLoading,
        modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_BUTTON),
    ) {
      Text("Sign in with Google")
    }
    uiState.errorMsg?.let { Text(it) }
  }
}
