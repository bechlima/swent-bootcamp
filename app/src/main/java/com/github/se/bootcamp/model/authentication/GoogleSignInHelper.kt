package com.github.se.bootcamp.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

interface GoogleSignInHelper {
  fun getIdToken(credential: Credential): String

  fun getFirebaseCredential(idToken: String): AuthCredential
}

class GoogleSignInHelperDefault : GoogleSignInHelper {
  override fun getIdToken(credential: Credential): String {
    if (credential !is CustomCredential || credential.type != TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      throw IllegalArgumentException("Credential is not a Google ID token credential")
    }

    return GoogleIdTokenCredential.createFrom(credential.data).idToken
  }

  override fun getFirebaseCredential(idToken: String): AuthCredential {
    return GoogleAuthProvider.getCredential(idToken, null)
  }
}
