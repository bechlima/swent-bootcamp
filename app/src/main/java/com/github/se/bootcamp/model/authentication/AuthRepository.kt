package com.github.se.bootcamp.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
  val currentUser: FirebaseUser?
    get() = null

  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  suspend fun signOut(): Result<Unit