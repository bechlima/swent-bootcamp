package com.github.se.bootcamp.utils

import android.util.Log
import com.github.se.bootcamp.model.todo.TODOS_COLLECTION_PATH
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.github.se.bootcamp.model.todo.ToDosRepositoryFirestore
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * A super-class intended to be extended by Android tests requiring the Firebase local emulators.
 *
 * Emulator connection is handled by [FirebaseEmulator]. This class wires a Firestore repository,
 * clears leftover documents around each test, and signs out afterwards.
 *
 * If a custom `@Before` method is needed, it should be overridden like so:
 * ```kotlin
 * @Before
 * override fun setUp() {
 *     super.setUp()
 *
 *     ...
 * }
 * ```
 *
 * And similarly for `@After` methods.
 */
open class FirebaseEmulatedTest(milestone: BootcampMilestone) : BootcampTest(milestone) {

  private suspend fun todosForMilestone() =
      FirebaseEmulator.firestore.collection(TODOS_COLLECTION_PATH).let { collection ->
        if (milestone == BootcampMilestone.B3) {
          val user = FirebaseEmulator.auth.currentUser ?: return@let null
          collection.whereEqualTo("ownerId", user.uid).get().await()
        } else {
          collection.get().await()
        }
      }

  suspend fun getTodosCount(): Int = todosForMilestone()?.size() ?: 0

  private suspend fun clearTestCollection() {
    todosForMilestone()?.forEach { it.reference.delete().await() }
  }

  suspend fun signInAnonymously(): FirebaseUser {
    return FirebaseEmulator.auth.signInAnonymously().await().user!!
  }

  override fun createInitializedRepository(): ToDosRepository {
    return ToDosRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      val todosCount = getTodosCount()
      if (todosCount > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $todosCount",
        )
        clearTestCollection()
      }
      assert(getTodosCount() == 0) {
        "Test collection is not empty at the beginning of the test, clearing failed."
      }
    }
  }

  @After
  override fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
