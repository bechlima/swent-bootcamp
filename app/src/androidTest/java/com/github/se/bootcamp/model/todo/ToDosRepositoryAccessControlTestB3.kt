// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.model.todo

import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo1
import com.github.se.bootcamp.utils.FirebaseEmulatedTest
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToDosRepositoryAccessControlTestB3 : FirebaseEmulatedTest(BootcampMilestone.B3) {

  @Test
  fun userCannotReadOrMutateAnotherUsersTodo() = runTest {
    val owner = FirebaseEmulator.auth.signInAnonymously().await().user!!
    val todo = todo1.copy(ownerId = owner.uid, uid = "access-control-${owner.uid}")
    repository.addTodo(todo)

    FirebaseEmulator.auth.signOut()
    val otherUser = FirebaseEmulator.auth.signInAnonymously().await().user!!
    val otherRepository = ToDosRepositoryFirestore(FirebaseEmulator.firestore)

    try {
      assertPermissionDenied { otherRepository.getTodo(todo.uid) }
      assertPermissionDenied { otherRepository.editTodo(todo.uid, todo.copy(name = "changed")) }
      assertPermissionDenied { otherRepository.deleteTodo(todo.uid) }
      assertEquals(emptyList<ToDo>(), otherRepository.getAllTodos())
      assertTrue(otherUser.uid != owner.uid)
    } finally {
      FirebaseEmulator.clearFirestoreEmulator()
    }
  }

  private suspend fun assertPermissionDenied(operation: suspend () -> Unit) {
    var exception: FirebaseFirestoreException? = null
    try {
      operation()
    } catch (error: FirebaseFirestoreException) {
      exception = error
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }
}
