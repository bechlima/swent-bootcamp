package com.github.se.bootcamp.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.fromDate
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo2
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo3
import com.github.se.bootcamp.utils.InMemoryTest
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditToDoScreenB2StaffTest : InMemoryTest(BootcampMilestone.B2) {
  @get:Rule val composeTestRule = createComposeRule()

  val todo =
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

  private fun withContent(
      editedTodo: ToDo = todo,
      todoList: List<ToDo> = listOf<ToDo>(editedTodo),
      block: (ToDo) -> Unit,
  ) {
    runTest {
      for (todo in todoList) {
        repository.addTodo(todo)
      }
    }

    composeTestRule.setContent { EditToDoScreen(todoUid = editedTodo.uid) }
    block(editedTodo)
  }

  @Test
  fun statusAppearedWithTheCorrectOrdering() = withContent {
    val targetStatus = ToDoStatus.ARCHIVED
    composeTestRule.enterEditTodoStatus(it.status, targetStatus)
    for (i in 1 until ToDoStatus.entries.size) {

      composeTestRule
          .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_STATUS)
          .assertIsDisplayed()
          .performClick()
          .assertTextContains(
              ToDoStatus.entries[(targetStatus.ordinal + i) % ToDoStatus.entries.size].toString(),
              substring = false,
              ignoreCase = true,
          )
    }
  }

  @Test
  fun savingWithInvalidTitleShouldDoNothing() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidTitle = " " // Title is mandatory
      composeTestRule.enterEditTodoTitle(invalidTitle)
      composeTestRule.clickOnSaveForEditTodo()
      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDescriptionShouldDoNothing() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDescription = " " // Description is mandatory
      composeTestRule.enterEditTodoDescription(invalidDescription)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidAssigneeShouldDoNothing() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidAssignee = " " // Assignee name is mandatory
      composeTestRule.enterEditTodoAssignee(invalidAssignee)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "This is not a date" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing1() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "12/03" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing2() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "12/03/25" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing3() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "12-03-2025" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing4() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "1/03/2025" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing5() = withContent { editedTodo ->
    checkTodoWasNotEdited(editedTodo) {
      val invalidDate = "10/3/2025" // Invalid date format
      composeTestRule.enterEditTodoDate(invalidDate)
      composeTestRule.clickOnSaveForEditTodo()

      // Check that we are still on the AddToDoScreen by verifying that the save button is still
      // displayed
      composeTestRule.onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
    }
  }

  @Test
  fun loadTheCorrectTodoData() =
      withContent(todoList = listOf(todo, todo2, todo3), editedTodo = todo2) { editedTodo ->
        composeTestRule
            .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_TITLE)
            .assertIsDisplayed()
            .assertTextContains(editedTodo.name)

        composeTestRule
            .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DESCRIPTION)
            .assertIsDisplayed()
            .assertTextContains(editedTodo.description)

        composeTestRule
            .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_ASSIGNEE)
            .assertIsDisplayed()
            .assertTextContains(editedTodo.assigneeName)

        composeTestRule
            .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DATE)
            .assertIsDisplayed()
            .assertTextContains("15/10/2025", ignoreCase = true)

        composeTestRule
            .onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_STATUS)
            .assertIsDisplayed()
            .assertTextContains(editedTodo.status.toString(), ignoreCase = true)
      }

  @Test
  fun allFieldsAreValidOnLoad() =
      withContent(todoList = listOf(todo, todo2, todo3), editedTodo = todo2) { editedTodo ->
        composeTestRule.onNodeWithTag(EditToDoScreenTestTags.ERROR_MESSAGE).assertIsNotDisplayed()
      }

  @Test
  fun descriptionHasSufficientHeight() = withContent {
    val description =
        "This is a very long description that should span multiple lines \n" +
            "in the input field to test whether the height of the description input is sufficient. \n" +
            "Adding even more text to be sure that it is really long and spans multiple lines. \n" +
            "Hopefully this is enough!"
    composeTestRule.enterEditTodoDescription(description)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_DESCRIPTION)
        .assertTextContains(description, substring = false)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun enteringBlankTitleShowsErrorMessage() = withContent {
    val invalidTitle = " " // Title is mandatory
    composeTestRule.enterEditTodoTitle(invalidTitle)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringBlankDescriptionShowsErrorMessage() = withContent {
    val invalidDescription = " " // Description is mandatory
    composeTestRule.enterEditTodoDescription(invalidDescription)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringBlankAssigneeNameShowsErrorMessage() = withContent {
    val invalidAssigneeName = " " // Description is mandatory
    composeTestRule.enterEditTodoAssignee(invalidAssigneeName)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage() = withContent {
    val invalidDate = "This is not a date" // Invalid date format
    composeTestRule.enterEditTodoDate(invalidDate)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage2() = withContent {
    val invalidDate = "11-01-2025" // Invalid date format
    composeTestRule.enterEditTodoDate(invalidDate)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage3() = withContent {
    val invalidDate = "11/01/25" // Invalid date format
    composeTestRule.enterEditTodoDate(invalidDate)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage4() = withContent {
    val invalidDate = "1/01/2025" // Invalid date format
    composeTestRule.enterEditTodoDate(invalidDate)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage5() = withContent {
    val invalidDate = "10/1/2025" // Invalid date format
    composeTestRule.enterEditTodoDate(invalidDate)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
