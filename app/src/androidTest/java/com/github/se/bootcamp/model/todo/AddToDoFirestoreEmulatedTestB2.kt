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
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToDoFirestoreEmulatedTestB2 : FirebaseEmulatedTest(BootcampMilestone.B2) {
  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { BootcampApp() }
    composeTestRule.navigateToAddToDoScreen()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  private val todo1 =
      ToDo(
          uid = "0",
          name = "Buy groceries",
          description = "Milk, eggs, bread, and butter",
          assigneeName = "Alice",
          dueDate = Timestamp.fromDate(2025, Calendar.SEPTEMBER, 1),
          location = Location(46.5191, 6.5668, "Lausanne Coop"),
          status = ToDoStatus.CREATED,
          ownerId = "user",
      )

  private val todo2 =
      ToDo(
          uid = "1",
          name = "Walk the dog",
          description = "Take Fido for a walk in the park",
          assigneeName = "Bob",
          dueDate = Timestamp.fromDate(2025, Calendar.OCTOBER, 15),
          location = Location(46.5210, 6.5790, "Parc de Mon Repos"),
          status = ToDoStatus.STARTED,
          ownerId = "user",
      )

  @Test
  fun canStoreNewTodoInDatabase() {
    composeTestRule.enterAddTodoDetails(todo1)
    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)
    runTest {
      assertEquals(1, getTodosCount())
      val todos = repository.getAllTodos()
      val storedTodo = todos.first()
      assertTrue(todo1.b2Equals(storedTodo))
    }
  }

  @Test
  fun canStoreMultipleTodosInDatabase() {
    composeTestRule.enterAddTodoDetails(todo1)
    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoDetails(todo2)
    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)

    runTest {
      assertEquals(2, getTodosCount())
      val todos = repository.getAllTodos()

      val storedTodo1 = todos.find { it.name == todo1.name }
      val storedTodo2 = todos.find { it.name == todo2.name }

      Assert.assertNotNull(storedTodo1)
      Assert.assertNotNull(storedTodo2)

      assertTrue(todo1.copy(status = ToDoStatus.CREATED).b2Equals(storedTodo1!!))
      assertTrue(todo2.copy(status = ToDoStatus.CREATED).b2Equals(storedTodo2!!))
    }
  }
}
