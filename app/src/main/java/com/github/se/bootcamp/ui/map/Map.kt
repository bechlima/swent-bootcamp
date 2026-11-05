package com.github.se.bootcamp.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.ui.overview.OverviewViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"

  fun getTestTagForTodoMarker(todoId: String): String = "todoMarker_$todoId"
}

@Composable
fun MapScreen(
    overviewViewModel: OverviewViewModel = viewModel(),
    /**
     * You can add parameters with default values here, e.g., modifier: Modifier = Modifier,
     * navController: NavHostController = rememberNavController()
     *
     * Note: Parameters with default values do not mean that you should use the default value in
     * your implementation. They ensure that we can still use your code in our tests.
     */
) {
  LaunchedEffect(Unit) { overviewViewModel.loadTodos() }

  val todos by overviewViewModel.todos.collectAsState()
  SimpleMap(todos)
}

@Composable
fun SimpleMap(todos: List<ToDo> = emptyList()) {
  val todosWithLocations = todosWithLocations(todos)
  val initialLocation = initialMapLocation(todos)
  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(initialLocation.toLatLng(), DEFAULT_ZOOM)
  }
  var hasCenteredOnTodo by remember { mutableStateOf(false) }

  LaunchedEffect(todosWithLocations) {
    if (!hasCenteredOnTodo && todosWithLocations.isNotEmpty()) {
      cameraPositionState.position =
          CameraPosition.fromLatLngZoom(initialLocation.toLatLng(), DEFAULT_ZOOM)
      hasCenteredOnTodo = true
    }
  }

  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
  ) {
    todosWithLocations.forEach { todo ->
      val location = todo.location ?: return@forEach
      val markerState =
          remember(todo.uid, location.latitude, location.longitude) {
            MarkerState(position = LatLng(location.latitude, location.longitude))
          }
      Marker(state = markerState, title = todo.name)
    }
  }
}

private const val DEFAULT_ZOOM = 12f
private val EPFL_LOCATION = com.github.se.bootcamp.model.map.Location(46.5191, 6.5668, "EPFL")

internal fun todosWithLocations(todos: List<ToDo>): List<ToDo> = todos.filter {
  it.location != null
}

internal fun initialMapLocation(todos: List<ToDo>): com.github.se.bootcamp.model.map.Location =
    todosWithLocations(todos).firstOrNull()?.location ?: EPFL_LOCATION

private fun com.github.se.bootcamp.model.map.Location.toLatLng(): LatLng =
    LatLng(latitude, longitude)
