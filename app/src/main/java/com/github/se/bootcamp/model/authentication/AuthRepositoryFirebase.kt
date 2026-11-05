package com.github.se.bootcamp.model.authentication

import androidx.credentials.Credential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthRepositoryFirebase(
    private val firebaseAuth: FirebaseAuth = Firebase.auth,
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelperDefault(),
) : AuthRepository {
  override val currentUser: FirebaseUser?
    get() = firebaseAuth.currentUser

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      val idToken = googleSignInHelper.getIdToken(credential)
      val authCredential = googleSignInHelper.getFirebaseCredential(idToken)
      Result.success(
          firebaseAuth.signInWithCredential(authCredential).await().user
              ?: throw IllegalStateException("Firebase sign-in returned no user")
      )
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      Result.failure(exception)
    }
  }

  override suspend fun signOut(): Result<Unit> {
    return runCatching { firebaseAuth.signOut() }
  }
}
