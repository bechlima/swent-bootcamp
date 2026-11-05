package com.github.se.bootcamp.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo1
import com.github.se.bootcamp.utils.InMemoryTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToDoScreenB2StaffTestTest : InMemoryTest(BootcampMilestone.B2) {
  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { AddTodoScreen() }
  }

  @Test
  fun descriptionHasSufficientHeight() {
    val description =
        "This is a very long description that should span multiple lines \n" +
            "in the input field to test whether the height of the description input is sufficient. \n" +
            "Adding even more text to be sure that it is really long and spans multiple lines. \n" +
            "Hopefully this is enough!"
    composeTestRule.enterAddTodoDescription(description)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_DESCRIPTION)
        .assertTextContains(description, substring = false)
    composeTestRule
        .onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing1() = checkNoTodoWereAdded {
    val invalidDate = "12/03" // Invalid date format
    composeTestRule.enterAddTodoDetails(todo = todo1, date = invalidDate)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing2() = checkNoTodoWereAdded {
    val invalidDate = "12/03/25" // Invalid date format
    composeTestRule.enterAddTodoDetails(todo = todo1, date = invalidDate)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing3() = checkNoTodoWereAdded {
    val invalidDate = "12-03-2025" // Invalid date format
    composeTestRule.enterAddTodoDetails(todo = todo1, date = invalidDate)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing4() = checkNoTodoWereAdded {
    val invalidDate = "1/03/2025" // Invalid date format
    composeTestRule.enterAddTodoDetails(todo = todo1, date = invalidDate)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidDateShouldDoNothing5() = checkNoTodoWereAdded {
    val invalidDate = "10/3/2025" // Invalid date format
    composeTestRule.enterAddTodoDetails(todo = todo1, date = invalidDate)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage2() {
    val invalidDate = "11-01-2025" // Invalid date format
    composeTestRule.enterAddTodoDate(invalidDate)
    composeTestRule.checkErrorMessageIsDisplayedForAddTodo()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage3() {
    val invalidDate = "11/01/25" // Invalid date format
    composeTestRule.enterAddTodoDate(invalidDate)
    composeTestRule.checkErrorMessageIsDisplayedForAddTodo()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage4() {
    val invalidDate = "1/01/2025" // Invalid date format
    composeTestRule.enterAddTodoDate(invalidDate)
    composeTestRule.checkErrorMessageIsDisplayedForAddTodo()
  }

  @Test
  fun enteringInvalidDateShowsErrorMessage5() {
    val invalidDate = "10/1/2025" // Invalid date format
    composeTestRule.enterAddTodoDate(invalidDate)
    composeTestRule.checkErrorMessageIsDisplayedForAddTodo()
  }
}
