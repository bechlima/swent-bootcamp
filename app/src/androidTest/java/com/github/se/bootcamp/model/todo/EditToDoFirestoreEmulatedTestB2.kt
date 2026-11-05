package com.github.se.bootcamp.model.todo

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.fromDate
import com.github.se.bootcamp.utils.FirebaseEmulatedTest
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditToDoFirestoreEmulatedTestB2 : FirebaseEmulatedTest(BootcampMilestone.B2) {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      repository.addTodo(todo1)
      repository.addTodo(todo2)
    }

    composeTestRule.setContent { BootcampApp() }
  }

  private val todo1
    get() =
        ToDo(
            uid = "0",
            name = "Buy groceries",
            description = "Milk, eggs, bread, and butter",
            assigneeName = "Alice",
            dueDate = Timestamp.fromDate(2025, Calendar.SEPTEMBER, 1),
            location = Location(46.5191, 6.5668, "Lausanne Coop"),
            status = ToDoStatus.CREATED,
            ownerId = currentUser.uid,
        )

  private val todo2
    get() =
        ToDo(
            uid = "1",
            name = "Walk the dog",
            description = "Take Fido for a walk in the park",
            assigneeName = "Bob",
            dueDate = Timestamp.fromDate(2025, Calendar.OCTOBER, 15),
            location = Location(46.5210, 6.5790, "Parc de Mon Repos"),
            status = ToDoStatus.STARTED,
            ownerId = currentUser.uid,
        )

  private val initialRepositoryContent
    get() = listOf(todo1, todo2)

  @Test
  fun editingATodoUpdateTheRepositoryContent() {
    val editedTodo1 =
        todo1.copy(
            name = "This is the new name",
            description = "New description",
            assigneeName = "New assignee",
            dueDate = Timestamp.fromDate(2025, Calendar.DECEMBER, 24),
            location = Location(46.5210, 6.5790, "New location"),
        )
    composeTestRule.navigateToEditToDoScreen(todo1)
    composeTestRule.enterEditTodoDetails(editedTodo1)
    composeTestRule.clickOnSaveForEditTodo(waitForRedirection = true)
    runTest {
      val storedTodos = repository.getAllTodos()
      assertEquals(storedTodos.size, initialRepositoryContent.size)
      val storedEditedTodo = storedTodos.find { it.name == editedTodo1.name }
      assertNotNull(storedEditedTodo)
      assertTrue(editedTodo1.b2Equals(storedEditedTodo!!))
    }
  }

  @Test
  fun canDeleteTodo() {
    val savedTodo1 = repository.getTodoByName(todo1.name)
    composeTestRule.navigateToEditToDoScreen(editedToDo = savedTodo1)
    composeTestRule.clickOnDeleteForEditTodo(waitForRedirection = true)
    runTest {
      val storedTodos = repository.getAllTodos()
      assertEquals(storedTodos.size, initialRepositoryContent.size - 1)
      val deletedTodo = storedTodos.find { it.name == todo1.name }
      Assert.assertNull(deletedTodo)
    }
  }
}
