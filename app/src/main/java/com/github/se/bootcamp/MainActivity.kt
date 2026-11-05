package com.github.se.bootcamp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.se.bootcamp.model.map.LocationRepository
import com.github.se.bootcamp.model.map.NominatimLocationRepository
import com.github.se.bootcamp.ui.authentication.SignInScreen
import com.github.se.bootcamp.ui.authentication.SignInViewModel
import com.github.se.bootcamp.ui.map.MapScreen
import com.github.se.bootcamp.ui.navigation.NavigationTestTags
import com.github.se.bootcamp.ui.overview.AddTodoScreen
import com.github.se.bootcamp.ui.overview.EditToDoScreen
import com.github.se.bootcamp.ui.overview.OverviewScreen
import com.github.se.bootcamp.ui.theme.BootcampTheme
import okhttp3.OkHttpClient

enum class BootCampScreen {
  SignIn,
  Overview,
  Map,
  AddTodo,
  EditTodo,
}

/**
 * *B3 only*:
 *
 * Provide an OkHttpClient client for network requests.
 *
 * Property `client` is mutable for testing purposes.
 */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

object LocationRepositoryProvider {
  fun create(): LocationRepository = NominatimLocationRepository(HttpClientProvider.client)
}

/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { BootcampTheme { Surface(modifier = Modifier.fillMaxSize()) { BootcampApp() } } }
  }
}

/**
 * `BootcampApp` is the main composable function that sets up the whole app UI. It initializes the
 * navigation controller and defines the navigation graph. You can add your app implementation
 * inside this function.
 *
 * For B3:
 *
 * @param context The context of the application, used for accessing resources and services.
 * @param credentialManager The CredentialManager instance for handling authentication credentials.
 */
@Composable
fun BootcampApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
) {
  val navController = rememberNavController()
  val signInViewModel: SignInViewModel = viewModel()
  val authState by signInViewModel.uiState.collectAsState()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val editTodoRoute = "${BootCampScreen.EditTodo.name}/{todoUid}"
  val isEditTodoRoute = currentRoute == editTodoRoute
  val isAddTodoRoute = currentRoute == BootCampScreen.AddTodo.name
  val isSignInRoute = currentRoute == BootCampScreen.SignIn.name

  LaunchedEffect(authState.user, authState.signedOut) {
    if (authState.user != null && currentRoute == BootCampScreen.SignIn.name) {
      navController.navigate(BootCampScreen.Overview.name) {
        popUpTo(BootCampScreen.SignIn.name) { inclusive = true }
      }
    } else if (authState.signedOut && currentRoute != BootCampScreen.SignIn.name) {
      navController.navigate(BootCampScreen.SignIn.name) {
        popUpTo(BootCampScreen.Overview.name) { inclusive = true }
      }
    }
  }

  BackHandler(enabled = currentRoute == BootCampScreen.Map.name) { navController.popBackStack() }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        if (!isSignInRoute && !isAddTodoRoute && !isEditTodoRoute) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(16.dp),
          ) {
            Text(
                when (currentRoute) {
                  BootCampScreen.Overview.name -> "Overview"
                  BootCampScreen.Map.name -> "Map"
                  else -> ""
                },
                modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
            )
          }
        }
      },
      bottomBar = {
        if (!isSignInRoute && !isAddTodoRoute && !isEditTodoRoute) {
          NavigationBar(modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU)) {
            NavigationBarItem(
                selected = currentRoute == BootCampScreen.Overview.name,
                onClick = {
                  if (currentRoute != BootCampScreen.Overview.name) {
                    navController.navigate(BootCampScreen.Overview.name) {
                      popUpTo(BootCampScreen.Overview.name) { inclusive = true }
                      launchSingleTop = true
                    }
                  }
                },
                icon = { Icon(Icons.Default.Menu, contentDescription = "Overview") },
                label = { Text("Overview") },
                modifier = Modifier.testTag(NavigationTestTags.OVERVIEW_TAB),
            )

            NavigationBarItem(
                selected = currentRoute == BootCampScreen.Map.name,
                onClick = {
                  if (currentRoute != BootCampScreen.Map.name) {
                    navController.navigate(BootCampScreen.Map.name) { launchSingleTop = true }
                  }
                },
                icon = { Icon(Icons.Default.Public, contentDescription = "Map") },
                label = { Text("Map") },
                modifier = Modifier.testTag(NavigationTestTags.MAP_TAB),
            )
          }
        }
      },
  ) { innerPadding ->
    NavHost(
        navController = navController,
        startDestination =
            if (authState.user == null) BootCampScreen.SignIn.name
            else BootCampScreen.Overview.name,
        modifier = Modifier.padding(innerPadding),
    ) {
      composable(BootCampScreen.SignIn.name) {
        SignInScreen(
            credentialManager = credentialManager,
            signInViewModel = signInViewModel,
        )
      }

      composable(BootCampScreen.Overview.name) {
        OverviewScreen(
            onButtonClick = { navController.navigate(BootCampScreen.AddTodo.name) },
            onTodoClick = { todo ->
              navController.navigate("${BootCampScreen.EditTodo.name}/${todo.uid}")
            },
            onLogoutClick = { signInViewModel.signOut(credentialManager) },
        )
      }

      composable(BootCampScreen.Map.name) { MapScreen() }

      composable(BootCampScreen.AddTodo.name) {
        AddTodoScreen(
            onSaveSuccess = {
              navController.navigate(BootCampScreen.Overview.name) {
                popUpTo(BootCampScreen.Overview.name)
                launchSingleTop = true
              }
            },
            onBack = { navController.popBackStack() },
        )
      }

      composable(editTodoRoute) { backStackEntry ->
        EditToDoScreen(
            todoUid = backStackEntry.arguments?.getString("todoUid") ?: "",
            onSuccess = {
              navController.navigate(BootCampScreen.Overview.name) {
                popUpTo(BootCampScreen.Overview.name)
                launchSingleTop = true
              }
            },
            onBack = { navController.popBackStack() },
        )
      }
    }
  }
}
