package com.github.se.bootcamp.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.model.todo.ToDosRepositoryLocal
import com.github.se.bootcamp.model.todo.ToDosRepositoryProvider
import com.github.se.bootcamp.ui.overview.OverviewScreenTestTags
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.google.firebase.Timestamp
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// ***************************************************************************** //
// ***                                                                       *** //
// *** THIS FILE CONTAIN STAFF TESTS. IT SHOULD NOT BE PUSHED ON THE         *** //
// *** STUDENT'S REPOSITORY.                                                 *** //
// ***                                                                       *** //
// ***************************************************************************** //

class NavigationB1StaffTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  val repository = ToDosRepositoryLocal()

  private fun getTimestamp(year: Int, month: Int, day: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day)
    return Timestamp(calendar.time)
  }

  private val todo1 =
      ToDo(
          uid = "0",
          name = "Buy groceries",
          description = "Milk, eggs, bread, and butter",
          assigneeName = "Alice",
          dueDate = getTimestamp(2025, Calendar.SEPTEMBER, 1),
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
          dueDate = getTimestamp(2025, Calendar.OCTOBER, 15),
          location = Location(46.5210, 6.5790, "Parc de Mon Repos"),
          status = ToDoStatus.STARTED,
          ownerId = "user",
      )
  private val lastTodo =
      ToDo(
          uid = "1000",
          name = "Swent Bootcamp",
          description = "Complete the SE Bootcamp",
          assigneeName = "Me",
          dueDate = getTimestamp(2025, Calendar.SEPTEMBER, 29),
          location = null,
          status = ToDoStatus.STARTED,
          ownerId = "user",
      )

  init {
    runTest {
      repository.addTodo(todo1)
      repository.addTodo(todo2)
      for (i in 3..50) {
        val todo =
            ToDo(
                uid = "$i",
                name = "Task $i",
                description = "Description for task $i",
                assigneeName = if (i % 2 == 0) "Alice" else "Bob",
                dueDate = getTimestamp(2025, Calendar.NOVEMBER, i),
                location = null,
                status = ToDoStatus.CREATED,
                ownerId = "user",
            )
        repository.addTodo(todo)
      }
      repository.addTodo(lastTodo)
    }
  }

  private fun checkOverviewScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("overview", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertIsDisplayed()
  }

  private fun checkOverviewScreenIsNotDisplayed() {
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertDoesNotExist()
  }

  private fun checkMapScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("map", substring = true, ignoreCase = true)
  }

  private fun checkAddToDoScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Create a new task", substring = false, ignoreCase = true)
  }

  private fun scrollToLastTodo() {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.TODO_LIST)
        .performScrollToNode(hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo)))
  }

  @Before
  fun setUp() {
    ToDosRepositoryProvider.repository = repository
    if (FirebaseEmulator.isRunning) {
      runTest { FirebaseEmulator.auth.signInAnonymously().await() }
    }
    composeTestRule.setContent { BootcampApp() }
  }

  /**
   * This test verifies that navigating between the Overview and Map tabs does not stack multiple
   * instances of the same screen. The navigation flow tested is:
   * - Overview -> Map -> Overview -> Map -> Overview
   * - Press Back -> Exit the app (and not go back to Map or Overview)
   */
  @Test
  fun navigateBetweenTabsDoNotStack() {
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
    checkOverviewScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
    checkOverviewScreenIsDisplayed()
    // Cheeck that the activity is finished

  }

  /**
   * This test verifies that the following navigation flow works as expected:
   * - Overview -> Map -> Map
   * - Press Back -> Overview (and not Map)
   */
  @Test
  fun navigateToMapFromMapAndPressBackGoesToOverview() {
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    pressBack(shouldFinish = false)
    assertFalse(composeTestRule.activity.isFinishing)
    checkOverviewScreenIsDisplayed()
  }

  /**
   * This test verifies that clicking on the currently selected tab does not cause the screen to
   * recompose or reset its state.
   */
  @Test
  fun clickOnCurrentTabDoesNotRecompose() {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsNotDisplayed()
    scrollToLastTodo()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo1))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsDisplayed()
  }

  /**
   * This test verifies that navigating to the AddToDo screen and then back to the Overview screen
   * restores the previous state of the Overview screen (e.g., scroll position).
   */
  @Test
  fun navigateBackFromAddToDoRestoresState() {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsNotDisplayed()
    scrollToLastTodo()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).performClick()
    checkOverviewScreenIsNotDisplayed()
    checkAddToDoScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    checkOverviewScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsDisplayed()
  }

  /**
   * This test verifies that navigating between the Overview and Map tabs does not restore the
   * previous state of the screen (e.g., scroll position).
   */
  @Test
  fun navigateBetweenTabsDoNotRestoreState() {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsNotDisplayed()
    scrollToLastTodo()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
    checkOverviewScreenIsDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo))
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo1))
        .assertIsDisplayed()
  }

  /** This test verifies that pressing the system back button on the Overview tab exits the app. */
  @Test
  fun navigateBackOnOverviewTabExitsApp() {
    composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
    pressBack(shouldFinish = true)
  }

  private fun pressBack(shouldFinish: Boolean) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitUntil { composeTestRule.activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, composeTestRule.activity.isFinishing)
  }

  /**
   * This test verifies that pressing the system back button twice from the Map tab exits the app.
   * The navigation flow tested is:
   * - Overview -> Map -> Overview -> Map -> Overview -> AddToDo -> Back to Overview -> Map
   * - Press Back -> Overview
   * - Press Back -> Exit the app
   */
  @Test
  fun canExitTheAppFromMapWithTwoTapOnBackButton() {
    // Switch tabs multiple times
    for (i in 1..3) {
      composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
      checkMapScreenIsDisplayed()
      composeTestRule.onNodeWithTag(NavigationTestTags.OVERVIEW_TAB).performClick()
      checkOverviewScreenIsDisplayed()
    }
    checkOverviewScreenIsDisplayed()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).performClick()
    checkAddToDoScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    checkOverviewScreenIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.MAP_TAB).performClick()
    checkMapScreenIsDisplayed()
    // First back press should go to Overview
    pressBack(shouldFinish = false)
    assertFalse(composeTestRule.activity.isFinishing)
    checkOverviewScreenIsDisplayed()
    // Second back press should exit the app
    pressBack(shouldFinish = true)
    assertTrue(composeTestRule.activity.isFinishing)
  }
}
