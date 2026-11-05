package com.github.se.bootcamp.ui.authentication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.se.bootcamp.BootcampApp
import com.github.se.bootcamp.ui.overview.OverviewScreenTestTags
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.FirebaseEmulatedTest
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.github.se.bootcamp.utils.UI_WAIT_TIMEOUT
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationNavigationB3Test : FirebaseEmulatedTest(BootcampMilestone.B3) {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.auth.signOut()
  }

  @Test
  fun alreadyAuthenticated_startsOnOverview() {
    runTest { FirebaseEmulator.auth.signInAnonymously().await() }

    composeTestRule.setContent { BootcampApp() }

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.EMPTY_TODO_LIST_MSG)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.checkOverviewScreenIsDisplayed()
  }

  @Test
  fun logout_returnsToSignInScreen() {
    runTest { FirebaseEmulator.auth.signInAnonymously().await() }
    composeTestRule.setContent { BootcampApp() }

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(OverviewScreenTestTags.LOGOUT_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.LOGOUT_BUTTON).performClick()

    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }
}
