// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.overview

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LocationSearchViewModelTestB3 {
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun addViewModel_exposesLocationSearchFailure() = runTest {
    val viewModel =
        AddToDoViewModel(
            todoRepository = EmptyToDosRepository,
            locationRepository =
                object : com.github.se.bootcamp.model.map.LocationRepository {
                  override suspend fun search(
                      query: String
                  ): List<com.github.se.bootcamp.model.map.Location> {
                    throw IOException("offline")
                  }
                },
        )

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()

    assertEquals("Could not search locations.", viewModel.uiState.value.locationSearchError)
  }

  @Test
  fun addViewModel_exposesSuccessfulLocationSearchResults() = runTest {
    val suggestions = locations(6)
    val viewModel = AddToDoViewModel(EmptyToDosRepository, repositoryReturning(suggestions))

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()

    assertEquals(suggestions.take(5), viewModel.uiState.value.locationSuggestions)
    assertEquals(null, viewModel.uiState.value.locationSearchError)
  }

  @Test
  fun addViewModel_blankLocation_clearsSuggestionsWithoutSearching() = runTest {
    var searchCount = 0
    val viewModel =
        AddToDoViewModel(
            EmptyToDosRepository,
            object : com.github.se.bootcamp.model.map.LocationRepository {
              override suspend fun search(
                  query: String
              ): List<com.github.se.bootcamp.model.map.Location> {
                searchCount += 1
                return locations(1)
              }
            },
        )

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()
    viewModel.updateLocation(" ")
    advanceUntilIdle()

    assertEquals(1, searchCount)
    assertEquals(
        emptyList<com.github.se.bootcamp.model.map.Location>(),
        viewModel.uiState.value.locationSuggestions,
    )
  }

  @Test
  fun addViewModel_exposesMalformedResponseFailure() = runTest {
    val viewModel =
        AddToDoViewModel(
            EmptyToDosRepository,
            repositoryThrowing(JSONException("malformed")),
        )

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()

    assertEquals("Could not search locations.", viewModel.uiState.value.locationSearchError)
  }

  @Test
  fun editViewModel_exposesMalformedCoordinateFailure() = runTest {
    val viewModel =
        EditToDoViewModel(
            EmptyToDosRepository,
            repositoryThrowing(NumberFormatException("invalid coordinate")),
        )

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()

    assertEquals("Could not search locations.", viewModel.uiState.value.locationSearchError)
  }

  @Test
  fun editViewModel_exposesSuccessfulLocationSearchResults() = runTest {
    val suggestions =
        (1..6).map { index ->
          com.github.se.bootcamp.model.map.Location(
              index.toDouble(),
              index.toDouble(),
              "Place $index",
          )
        }
    val viewModel =
        EditToDoViewModel(
            todoRepository = EmptyToDosRepository,
            locationRepository =
                object : com.github.se.bootcamp.model.map.LocationRepository {
                  override suspend fun search(
                      query: String
                  ): List<com.github.se.bootcamp.model.map.Location> = suggestions
                },
        )

    viewModel.updateLocation("EPFL")
    advanceUntilIdle()

    assertEquals(suggestions.take(5), viewModel.uiState.value.locationSuggestions)
    assertEquals(null, viewModel.uiState.value.locationSearchError)
  }

  @Test
  fun editViewModel_rethrowsCancellationWithoutSettingSearchError() = runTest {
    val viewModel =
        EditToDoViewModel(
            todoRepository = EmptyToDosRepository,
            locationRepository =
                object : com.github.se.bootcamp.model.map.LocationRepository {
                  override suspend fun search(
                      query: String
                  ): List<com.github.se.bootcamp.model.map.Location> {
                    throw CancellationException("search cancelled")
                  }
                },
        )

    var cancellation: CancellationException? = null
    try {
      viewModel.searchLocation("EPFL")
    } catch (exception: CancellationException) {
      cancellation = exception
    }

    assertTrue(cancellation != null)
    assertEquals(null, viewModel.uiState.value.locationSearchError)
  }

  private object EmptyToDosRepository : com.github.se.bootcamp.model.todo.ToDosRepository {
    override fun getNewUid(): String = "unused"

    override suspend fun getAllTodos() = emptyList<com.github.se.bootcamp.model.todo.ToDo>()

    override suspend fun getTodo(todoID: String) =
        error("getTodo should not be called in this test")

    override suspend fun addTodo(toDo: com.github.se.bootcamp.model.todo.ToDo) = Unit

    override suspend fun editTodo(
        todoID: String,
        newValue: com.github.se.bootcamp.model.todo.ToDo,
    ) = Unit

    override suspend fun deleteTodo(todoID: String) = Unit
  }

  private fun locations(count: Int) =
      (1..count).map {
        com.github.se.bootcamp.model.map.Location(it.toDouble(), it.toDouble(), "Place $it")
      }

  private fun repositoryReturning(suggestions: List<com.github.se.bootcamp.model.map.Location>) =
      object : com.github.se.bootcamp.model.map.LocationRepository {
        override suspend fun search(
            query: String
        ): List<com.github.se.bootcamp.model.map.Location> = suggestions
      }

  private fun repositoryThrowing(error: Exception) =
      object : com.github.se.bootcamp.model.map.LocationRepository {
        override suspend fun search(
            query: String
        ): List<com.github.se.bootcamp.model.map.Location> {
          throw error
        }
      }
}
