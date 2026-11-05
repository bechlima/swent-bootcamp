// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.overview

import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OverviewViewModelTestB3 {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun loadTodosFailure_exposesErrorWithoutReplacingTodos() = runTest {
    val viewModel =
        OverviewViewModel(
            object : ToDosRepository {
              override fun getNewUid() = "unused"

              override suspend fun getAllTodos(): List<ToDo> {
                throw IllegalStateException("read failed")
              }

              override suspend fun getTodo(todoID: String): ToDo = error("unused")

              override suspend fun addTodo(toDo: ToDo) = Unit

              override suspend fun editTodo(todoID: String, newValue: ToDo) = Unit

              override suspend fun deleteTodo(todoID: String) = Unit
            }
        )

    viewModel.loadTodos()
    advanceUntilIdle()

    assertEquals(emptyList<ToDo>(), viewModel.todos.value)
    assertEquals("Could not load tasks.", viewModel.error.value)
  }
}
