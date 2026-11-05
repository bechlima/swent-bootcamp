package com.github.se.bootcamp.model.todo

import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo1
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo2
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo3
import com.github.se.bootcamp.utils.FirebaseEmulatedTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToDosRepositoryFirestoreTestB2 : FirebaseEmulatedTest(BootcampMilestone.B2) {
  private val todo1ForUser
    get() = todo1.copy(ownerId = currentUser.uid)

  private val todo2ForUser
    get() = todo2.copy(ownerId = currentUser.uid)

  private val todo3ForUser
    get() = todo3.copy(ownerId = currentUser.uid)

  @Test
  fun canAddToDosToRepository() = runTest {
    repository.addTodo(todo1ForUser)
    assertEquals(1, getTodosCount())
    val todos = repository.getAllTodos()

    assertEquals(1, todos.size)
    // Discard uid and ownerId for comparison since they don't matter in this test
    val expectedTodo = todo1ForUser.copy(uid = "None", ownerId = "None")
    val storedTodo = todos.first().copy(uid = expectedTodo.uid, ownerId = expectedTodo.ownerId)

    assertEquals(expectedTodo, storedTodo)
  }

  @Test
  fun addTodoWithTheCorrectID() = runTest {
    repository.addTodo(todo1ForUser)
    assertEquals(1, getTodosCount())
    val storedTodo = repository.getTodo(todo1ForUser.uid)
    assertEquals(storedTodo, todo1ForUser)
  }

  @Test
  fun canAddMultipleToDosToRepository() = runTest {
    repository.addTodo(todo1ForUser)
    repository.addTodo(todo2ForUser)
    repository.addTodo(todo3ForUser)
    assertEquals(3, getTodosCount())
    val todos = repository.getAllTodos()

    assertEquals(todos.size, 3)
    // Discard the ordering of the todos
    val expectedTodos = setOf(todo1ForUser, todo2ForUser, todo3ForUser)
    val storedTodos = todos.toSet()

    assertEquals(expectedTodos, storedTodos)
  }

  @Test
  fun uidAreUniqueInTheCollection() = runTest {
    val uid = "duplicate"
    val todo1Modified = todo1ForUser.copy(uid = uid)
    val todoDuplicatedUID = todo2ForUser.copy(uid = uid)
    // Depending on your implementation, adding a Todo with an existing UID
    // might not be permitted
    runCatching {
      repository.addTodo(todo1Modified)
      repository.addTodo(todoDuplicatedUID)
    }

    assertEquals(1, getTodosCount())

    val todos = repository.getAllTodos()
    assertEquals(todos.size, 1)
    val storedTodo = todos.first()
    assertEquals(storedTodo.uid, uid)
  }

  @Test
  fun getNewUidReturnsUniqueIDs() = runTest {
    val numberIDs = 100
    val uids = (0 until 100).toSet<Int>().map { repository.getNewUid() }.toSet()
    assertEquals(uids.size, numberIDs)
  }

  @Test
  fun canRetrieveATodoByID() = runTest {
    repository.addTodo(todo1ForUser)
    repository.addTodo(todo2ForUser)
    repository.addTodo(todo3ForUser)
    assertEquals(3, getTodosCount())
    val storedTodo = repository.getTodo(todo2ForUser.uid)
    assertEquals(storedTodo, todo2ForUser)
  }

  @Test
  fun canDeleteATodoByID() = runTest {
    repository.addTodo(todo1ForUser)
    repository.addTodo(todo2ForUser)
    repository.addTodo(todo3ForUser)

    repository.deleteTodo(todo2ForUser.uid)
    assertEquals(2, getTodosCount())
    val todos = repository.getAllTodos()
    assertEquals(todos.size, 2)

    val expectedTodos = setOf(todo1ForUser, todo3ForUser)
    assertEquals(expectedTodos, todos.toSet())
  }

  @Test
  fun canEditATodoByID() = runTest {
    repository.addTodo(todo1ForUser)
    assertEquals(1, getTodosCount())
    val todos = repository.getAllTodos()
    assertEquals(1, todos.size)

    val modifiedTodo = todo1ForUser.copy(name = "Modified Name", status = ToDoStatus.ARCHIVED)
    repository.editTodo(todo1ForUser.uid, modifiedTodo)
    assertEquals(1, getTodosCount())
    val todosAfterEdit = repository.getAllTodos()
    assertEquals(todosAfterEdit.size, 1)
    assertEquals(todosAfterEdit.first(), modifiedTodo)
  }

  @Test
  fun canEditTheCorrectTodoByID() = runTest {
    repository.addTodo(todo1ForUser)
    repository.addTodo(todo2ForUser)
    repository.addTodo(todo3ForUser)

    assertEquals(3, getTodosCount())

    val todos = repository.getAllTodos()
    assertEquals(todos.size, 3)

    val modifiedTodo = todo1ForUser.copy(name = "Modified Name", status = ToDoStatus.ARCHIVED)
    repository.editTodo(todo1ForUser.uid, modifiedTodo)
    val todosAfterEdit = repository.getAllTodos()

    assertEquals(3, getTodosCount())
    assertEquals(3, todosAfterEdit.size)

    val expectedTodos = setOf(modifiedTodo, todo2ForUser, todo3ForUser)
    assertEquals(expectedTodos, todosAfterEdit.toSet())
  }

  /** Handling invalid function parameters is not a requirement * */
}
