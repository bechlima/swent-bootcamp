package com.github.se.bootcamp.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.HttpClientProvider
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.ui.navigation.NavigationTestTags
import com.github.se.bootcamp.ui.overview.AddToDoScreenTestTags
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo1
import com.github.se.bootcamp.utils.BootcampTest.Companion.todo3
import com.github.se.bootcamp.utils.FakeHttpClient
import com.github.se.bootcamp.utils.FakeHttpClient.FakeLocation
import com.github.se.bootcamp.utils.FakeHttpClient.locationSuggestions
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.github.se.bootcamp.utils.InMemoryTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GoogleMapsB3StaffTest : InMemoryTest(BootcampMilestone.B3) {

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
  fun mapScreen_displaysEpflTodoInRepository() {
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoDetails(
        todo3.copy(location = FakeLocation.EPFL.locationSuggestions.first())
    )
    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()

    composeTestRule.checkMapScreenIsDisplayed()
  }

  @Test
  fun addTodoWithLocation_thenMarkerAppearsInRepositoryAndMapScreen() {
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoTitle(todo1.name)
    composeTestRule.enterAddTodoDescription(todo1.description)
    composeTestRule.enterAddTodoAssignee(todo1.assigneeName)
    composeTestRule.enterAddTodoDate("01/01/2030")

    composeTestRule.enterAddTodoLocation(FakeLocation.EPFL)

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(AddToDoScreenTestTags.LOCATION_SUGGESTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule
        .onAllNodesWithTag(AddToDoScreenTestTags.LOCATION_SUGGESTION, useUnmergedTree = true)[0]
        .assertIsDisplayed()
        .performClick()

    composeTestRule.clickOnSaveForAddTodo(waitForRedirection = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).assertIsDisplayed().performClick()

    composeTestRule.checkMapScreenIsDisplayed()

    val todos = runBlocking { repository.getAllTodos() }
    assert(todos.any { it.location?.name == "Fake EPFL" || it.location?.name == "EPFL" })
  }
}
