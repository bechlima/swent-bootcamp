// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.authentication

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.test.core.app.ApplicationProvider
import com.github.se.bootcamp.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SignInViewModelTestB3 {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun signInSuccess_publishesAuthenticatedUser() = runTest {
    val user = mockk<FirebaseUser>()
    val repository = FakeAuthRepository(signInResult = Result.success(user))
    val viewModel = SignInViewModel(repository)

    viewModel.signIn(context, credentialManagerReturningCredential())
    advanceUntilIdle()

    assertEquals(user, viewModel.uiState.value.user)
    assertNull(viewModel.uiState.value.errorMsg)
    assertEquals(false, viewModel.uiState.value.isLoading)
  }

  @Test
  fun signInFailure_publishesRepositoryErrorAndNoUser() = runTest {
    val repository =
        FakeAuthRepository(signInResult = Result.failure(IllegalStateException("denied")))
    val viewModel = SignInViewModel(repository)

    viewModel.signIn(context, credentialManagerReturningCredential())
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.user)
    assertEquals("denied", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun credentialLoadingFailure_publishesUnauthenticatedError() = runTest {
    val viewModel = SignInViewModel(FakeAuthRepository())
    val credentialManager = mockk<CredentialManager>()
    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
        IllegalStateException("credential failure")

    viewModel.signIn(context, credentialManager)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.user)
    assertEquals("credential failure", viewModel.uiState.value.errorMsg)
    assertEquals(false, viewModel.uiState.value.isLoading)
  }

  @Test
  fun signOutSuccess_clearsUserAndPublishesSignedOutState() = runTest {
    val user = mockk<FirebaseUser>()
    val repository = FakeAuthRepository(currentUser = user)
    val viewModel = SignInViewModel(repository)

    viewModel.signOut(credentialManagerReturningCredential())
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.user)
    assertEquals(true, viewModel.uiState.value.signedOut)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signOutFailure_publishesRepositoryError() = runTest {
    val repository =
        FakeAuthRepository(signOutResult = Result.failure(IllegalStateException("failed")))
    val viewModel = SignInViewModel(repository)

    viewModel.signOut(credentialManagerReturningCredential())
    advanceUntilIdle()

    assertEquals("failed", viewModel.uiState.value.errorMsg)
    assertEquals(false, viewModel.uiState.value.signedOut)
  }

  private fun credentialManagerReturningCredential(): CredentialManager {
    val credentialManager = mockk<CredentialManager>()
    val response = mockk<GetCredentialResponse>()
    every { response.credential } returns mockk<Credential>()
    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        response
    coEvery { credentialManager.clearCredentialState(any<ClearCredentialStateRequest>()) } returns
        Unit
    return credentialManager
  }

  private class FakeAuthRepository(
      override val currentUser: FirebaseUser? = null,
      private val signInResult: Result<FirebaseUser> =
          Result.failure(IllegalStateException("not configured")),
      private val signOutResult: Result<Unit> = Result.success(Unit),
  ) : AuthRepository {
    override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
        signInResult

    override suspend fun signOut(): Result<Unit> = signOutResult
  }
}
