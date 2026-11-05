package com.github.se.bootcamp.ui.authentication

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.bootcamp.R
import com.github.se.bootcamp.model.authentication.AuthRepository
import com.github.se.bootcamp.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUIState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false,
)

class SignInViewModel(
    private val repository: AuthRepository = AuthRepositoryFirebase(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState(user = repository.currentUser))
  val uiState: StateFlow<AuthUIState> = _uiState

  fun signIn(context: Context, credentialManager: CredentialManager) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null, signedOut = false)
      try {
        val googleOption =
            GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id))
                .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
        val credential = credentialManager.getCredential(context, request).credential
        val result = repository.signInWithGoogle(credential)
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        _uiState.value =
            result.fold(
                onSuccess = { user -> AuthUIState(user = user) },
                onFailure = { error ->
                  AuthUIState(errorMsg = error.message ?: "Unable to sign in")
                },
            )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        _uiState.value = AuthUIState(errorMsg = error.message ?: "Unable to sign in")
      }
    }
  }

  fun signOut(credentialManager: CredentialManager) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true, errorMsg = null)
      try {
        val result = repository.signOut()
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        if (result.isSuccess) {
          _uiState.value = AuthUIState(signedOut = true)
          try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
          } catch (error: CancellationException) {
            throw error
          } catch (error: Exception) {
            _uiState.value =
                AuthUIState(
                    errorMsg = error.message ?: "Unable to clear credential state",
                    signedOut = true,
                )
          }
        } else {
          _uiState.value =
              AuthUIState(errorMsg = result.exceptionOrNull()?.message ?: "Unable to sign out")
        }
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        _uiState.value = AuthUIState(errorMsg = error.message ?: "Unable to sign out")
      }
    }
  }
}
