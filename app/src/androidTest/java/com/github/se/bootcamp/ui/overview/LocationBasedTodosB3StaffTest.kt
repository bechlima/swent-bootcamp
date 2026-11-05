package com.github.se.bootcamp.ui.overview

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.HttpClientProvider
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.toDateString
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo1
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo2
import com.github.se.bootcamp.utils.FakeHttpClient
import com.github.se.bootcamp.utils.FakeHttpClient.FakeLocation
import com.github.se.bootcamp.utils.FakeHttpClient.locationSuggestions
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.github.se.bootcamp.utils.InMemoryTest
import com.github.se.bootcamp.utils.UI_WAIT_TIMEOUT
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LocationBasedTodosB3StaffTest : InMemoryTest(BootcampMilestone.B3) {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    HttpClientProvider.client = FakeHttpClient.getClient()
    runBlocking { FirebaseEmulator.auth.signInAnonymously().await() }
    runBlocking { repository.addTodo(todo1.copy(location = Location(46.520278, 6.565556, "EPFL"))) }
    composeTestRule.setContent { BootcampApp() }
  }

  @Test
  fun addTodoScreen_selectsEpflLocation() {
    val expectedLocation = FakeLocation.EPFL.locationSuggestions.first()

    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoTitle("Task with EPFL")
    composeTestRule.enterAddTodoDescription("Testing AddTodo")
    composeTestRule.enterAddTodoAssignee("Tester")
    composeTestRule.enterAddTodoDate("01/01/2030")
    composeTestRule.enterAddTodoLocation(FakeLocation.EPFL)

    composeTestRule.onLocationSuggestion(expectedLocation).assertIsDisplayed().performClick()

    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)
  }

  @Test
  fun editTodoScreen_selectsEpflLocation() {
    val expectedLocation = FakeLocation.EPFL.locationSuggestions.first()

    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoDetails(todo2)
    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)

    composeTestRule.navigateToEditToDoScreen(todo2)
    composeTestRule.enterEditTodoLocation(FakeLocation.EPFL)
    composeTestRule.onLocationSuggestion(expectedLocation).assertIsDisplayed().performClick()
    composeTestRule.clickOnSaveForEditTodo(waitForRedirection = true)
  }

  @Test
  fun canAddATodoWithALocation() {
    composeTestRule.navigateToAddToDoScreen()
    val expectedLocation = FakeLocation.LAUSANNE.locationSuggestions.first()
    val queryForExpectedLocation = FakeLocation.LAUSANNE.queryName
    composeTestRule.enterAddTodoTitle(todo2.name)
    composeTestRule.enterAddTodoDescription(todo2.description)
    composeTestRule.enterAddTodoDate(todo2.dueDate.toDateString())
    composeTestRule.enterAddTodoAssignee(todo2.assigneeName)
    composeTestRule.enterAddTodoLocation(queryForExpectedLocation)
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AddToDoScreenTestTags.LOCATION_SUGGESTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onLocationSuggestion(expectedLocation).assertIsDisplayed().performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(AddToDoScreenTestTags.LOCATION_SUGGESTION)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)
    composeTestRule.checkOverviewScreenIsDisplayed()

    val storedTodo = repository.getTodoByName(todo2.name)
    Assert.assertEquals(expectedLocation, storedTodo.location)
  }
}
