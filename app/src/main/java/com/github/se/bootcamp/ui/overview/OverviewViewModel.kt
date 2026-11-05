package com.github.se.bootcamp.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.github.se.bootcamp.model.todo.ToDosRepositoryProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Overview screen.
 *
 * Responsible for managing the UI state, by fetching and providing ToDo items via the
 * [ToDosRepository].
 *
 * @property todoRepository The repository used to fetch and manage ToDo items.
 */
class OverviewViewModel(
    private val todoRepository: ToDosRepository = ToDosRepositoryProvider.repository
) : ViewModel() {

  private val _todos = MutableStateFlow<List<ToDo>>(emptyList())
  private val _error = MutableStateFlow<String?>(null)

  val todos: StateFlow<List<ToDo>> = _todos
  val error: StateFlow<String?> = _error

  fun loadTodos() {
    viewModelScope.launch {
      _error.value = null
      try {
        _todos.value = todoRepository.getAllTodos()
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _error.value = "Could not load tasks."
      }
    }
  }
}
