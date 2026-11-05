package com.github.se.bootcamp.ui.navigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.fromDate
import com.github.se.bootcamp.utils.InMemoryTest
import com.github.se.bootcamp.utils.StateCheckerBootcampTest
import com.github.se.bootcamp.utils.StateCheckerBootcampTest.ScrollableToDosRepository.Companion.firstTodo
import com.github.se.bootcamp.utils.StateCheckerBootcampTest.ScrollableToDosRepository.Companion.lastTodo
import com.google.firebase.Timestamp
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// *****************************************************************************
// ***                                                                       ***
// *** THIS FILE WILL BE OVERWRITTEN DURING GRADING. IT SHOULD BE LOCATED IN ***
// *** app/src/androidTest/java/com/github/se/bootcamp/ui/navigation.        ***
// *** DO **NOT** IMPLEMENT YOUR OWN TESTS IN THIS FILE                      ***
// ***                                                                       ***
// *****************************************************************************

class NavigationB2StaffTest : StateCheckerBootcampTest(InMemoryTest(BootcampMilestone.B2)) {
  @get:Rule val composeTestRule = createComposeRule()

  private val validTodo =
      ToDo(
          uid = "uid_not_used",
          name = "title",
          description = "description",
          assigneeName = "test",
          dueDate = Timestamp.fromDate(2025, Calendar.SEPTEMBER, 1),
          location = Location(46.5191, 6.5668, "Any"),
          status = ToDoStatus.CREATED,
          ownerId = "user",
      )

  val invalidTodo =
      ToDo(
          uid = "uid_not_used",
          name = " ",
          description = " ",
          assigneeName = " ",
          dueDate = Timestamp.fromDate(2025, Calendar.SEPTEMBER, 1),
          location = Location(46.5191, 6.5668, "Any"),
          status = ToDoStatus.CREATED,
          ownerId = "user",
      )

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { BootcampApp() }
  }

  @Test
  fun addTodo_saveButtonDoesNothingIfNoInputWereEntered() {
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.checkAddToDoScreenIsDisplayed()
  }

  @Test
  fun addTodo_saveButtonDoNotRestoreState() {
    composeTestRule.scrollToLastTodo()
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoDetails(todo = validTodo)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.checkOverviewScreenIsDisplayed()
    composeTestRule.checkTodoListIsNotScrolledDown()
  }

  @Test
  fun editTodo_saveButtonDoesNothingIfInputAreInvalid() {
    composeTestRule.navigateToEditToDoScreen(firstTodo)
    composeTestRule.enterEditTodoDetails(todo = invalidTodo, date = "This is not a date")
    composeTestRule.clickOnSaveForEditTodo()
    composeTestRule.checkEditToDoScreenIsDisplayed()
  }

  @Test
  fun editTodo_saveButtonDoNotRestoreState() {
    composeTestRule.scrollToLastTodo()
    composeTestRule.navigateToEditToDoScreen(lastTodo)
    composeTestRule.enterEditTodoDetails(todo = validTodo)
    composeTestRule.clickOnSaveForEditTodo()
    composeTestRule.checkOverviewScreenIsDisplayed()
    composeTestRule.checkTodoListIsNotScrolledDown()
  }

  @Test
  fun editTodo_goBackRestoresState() {
    composeTestRule.scrollToLastTodo()
    composeTestRule.navigateToEditToDoScreen(lastTodo)
    composeTestRule.navigateBack()
    composeTestRule.checkOverviewScreenIsDisplayed()
    composeTestRule.checkTodoListIsStillScrolledDown()
  }

  @Test
  fun editTodo_deleteButtonDoesNotRestoreState() {
    composeTestRule.scrollToLastTodo()
    composeTestRule.navigateToEditToDoScreen(lastTodo)
    composeTestRule.clickOnDeleteForEditTodo()
    composeTestRule.checkOverviewScreenIsDisplayed()
    composeTestRule.checkTodoListIsNotScrolledDown()
  }
}
