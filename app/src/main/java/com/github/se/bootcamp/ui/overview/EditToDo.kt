package com.github.se.bootcamp.ui.overview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.bootcamp.LocationRepositoryProvider
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.map.LocationRepository
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.github.se.bootcamp.model.todo.ToDosRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException

object EditToDoScreenTestTags {
  const val INPUT_TODO_TITLE = "inputTodoTitle"
  const val INPUT_TODO_DESCRIPTION = "inputTodoDescription"
  const val INPUT_TODO_ASSIGNEE = "inputTodoAssignee"
  const val INPUT_TODO_LOCATION = "inputTodoLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val INPUT_TODO_DATE = "inputTodoDate"
  const val INPUT_TODO_STATUS = "inputTodoStatus"
  const val TODO_SAVE = "todoSave"
  const val TODO_DELETE = "todoDelete"
  const val ERROR_MESSAGE = "errorMessage"
}

data class EditToDoUiState(
    val todo: ToDo? = null,
    val title: String = "",
    val description: String = "",
    val assignee: String = "",
    val locationQuery: String = "",
    val selectedLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val locationSearchError: String? = null,
    val dueDate: String = "",
    val status: ToDoStatus = ToDoStatus.CREATED,
    val error: String? = null,
    val operationSucceeded: Boolean = false,
)

class EditToDoViewModel(
    private val todoRepository: ToDosRepository = ToDosRepositoryProvider.repository,
    private val locationRepository: LocationRepository = LocationRepositoryProvider.create(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditToDoUiState())
  val uiState: StateFlow<EditToDoUiState> = _uiState.asStateFlow()

  private val touchedFields = mutableSetOf<String>()
  private var locationSearchJob: Job? = null

  fun loadTodo(todoUid: String) {
    viewModelScope.launch {
      try {
        val todo = todoRepository.getTodo(todoUid)
        _uiState.value =
            EditToDoUiState(
                todo = todo,
                title = todo.name,
                description = todo.description,
                assignee = todo.assigneeName,
                locationQuery = "",
                selectedLocation = todo.location,
                dueDate = formatDate(todo.dueDate),
                status = todo.status,
            )
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.value = _uiState.value.copy(error = "Could not load the task.")
      }
    }
  }

  fun updateTitle(value: String) = update("title") { it.copy(title = value) }

  fun updateDescription(value: String) = update("description") { it.copy(description = value) }

  fun updateAssignee(value: String) = update("assignee") { it.copy(assignee = value) }

  fun updateLocation(value: String) {
    update("location") { it.copy(locationQuery = value, locationSearchError = null) }
    locationSearchJob?.cancel()
    if (value.isBlank()) {
      _uiState.value =
          _uiState.value.copy(locationSuggestions = emptyList(), locationSearchError = null)
      return
    }
    locationSearchJob = viewModelScope.launch { searchLocation(value) }
  }

  internal suspend fun searchLocation(query: String) {
    try {
      val suggestions = locationRepository.search(query)
      _uiState.value =
          _uiState.value.copy(
              locationSuggestions = suggestions.take(5),
              locationSearchError = null,
          )
    } catch (exception: CancellationException) {
      throw exception
    } catch (_: IOException) {
      _uiState.value =
          _uiState.value.copy(
              locationSuggestions = emptyList(),
              locationSearchError = "Could not search locations.",
          )
    } catch (_: JSONException) {
      _uiState.value =
          _uiState.value.copy(
              locationSuggestions = emptyList(),
              locationSearchError = "Could not search locations.",
          )
    } catch (_: NumberFormatException) {
      _uiState.value =
          _uiState.value.copy(
              locationSuggestions = emptyList(),
              locationSearchError = "Could not search locations.",
          )
    }
  }

  fun selectLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(
            locationQuery = location.name,
            selectedLocation = location,
            locationSuggestions = emptyList(),
        )
  }

  fun updateDate(value: String) = update("dueDate") { it.copy(dueDate = value) }

  fun cycleStatus() {
    update("status") {
      it.copy(status = ToDoStatus.entries[(it.status.ordinal + 1) % ToDoStatus.entries.size])
    }
  }

  fun save() {
    val state = _uiState.value
    val todo = state.todo ?: return
    val error = validationError(state)
    if (error != null) {
      _uiState.value = state.copy(error = error)
      return
    }
    viewModelScope.launch {
      val ownerId = Firebase.auth.currentUser?.uid
      if (ownerId == null) {
        _uiState.value = _uiState.value.copy(error = "Could not save the task.")
        return@launch
      }
      val editedTodo =
          todo.copy(
              name = state.title.trim(),
              description = state.description.trim(),
              assigneeName = state.assignee.trim(),
              dueDate = parseDate(state.dueDate),
              location = state.selectedLocation,
              status = state.status,
              ownerId = ownerId,
          )
      try {
        todoRepository.editTodo(todo.uid, editedTodo)
        _uiState.value = _uiState.value.copy(operationSucceeded = true, error = null)
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.value = _uiState.value.copy(error = "Could not save the task.")
      }
    }
  }

  fun delete() {
    val todo = _uiState.value.todo ?: return
    viewModelScope.launch {
      try {
        todoRepository.deleteTodo(todo.uid)
        _uiState.value = _uiState.value.copy(operationSucceeded = true, error = null)
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.value = _uiState.value.copy(error = "Could not delete the task.")
      }
    }
  }

  private fun update(field: String, change: (EditToDoUiState) -> EditToDoUiState) {
    touchedFields += field
    val next = change(_uiState.value)
    _uiState.value = next.copy(error = validationError(next))
  }

  private fun validationError(state: EditToDoUiState): String? =
      when {
        state.title.isNotBlank() &&
            state.description.isNotBlank() &&
            state.assignee.isNotBlank() &&
            isValidDate(state.dueDate) -> null
        "title" in touchedFields && state.title.isBlank() -> "Title is required."
        "description" in touchedFields && state.description.isBlank() -> "Description is required."
        "assignee" in touchedFields && state.assignee.isBlank() -> "Assignee is required."
        "dueDate" in touchedFields && !isValidDate(state.dueDate) -> "Enter a date as dd/MM/yyyy."
        else -> null
      }

  private fun isValidDate(value: String): Boolean = Regex("""\d{2}/\d{2}/\d{4}""").matches(value)

  private fun parseDate(value: String): Timestamp {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    formatter.isLenient = true
    return Timestamp(formatter.parse(value)!!)
  }

  private fun formatDate(timestamp: Timestamp): String =
      SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(timestamp.toDate())
}

@Composable
fun EditToDoScreen(
    todoUid: String,
    editToDoViewModel: EditToDoViewModel = viewModel(),
    onSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
) {
  val state by editToDoViewModel.uiState.collectAsState()

  LaunchedEffect(todoUid) { editToDoViewModel.loadTodo(todoUid) }
  LaunchedEffect(state.operationSucceeded) { if (state.operationSucceeded) onSuccess() }

  Scaffold(topBar = { TodoTopBar("Edit Todo", onBack) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
      EditToDoTextField(
          "Title",
          state.title,
          editToDoViewModel::updateTitle,
          EditToDoScreenTestTags.INPUT_TODO_TITLE,
      )
      OutlinedTextField(
          value = state.description,
          onValueChange = editToDoViewModel::updateDescription,
          label = { Text("Description") },
          modifier =
              Modifier.fillMaxWidth()
                  .heightIn(min = 100.dp)
                  .testTag(EditToDoScreenTestTags.INPUT_TODO_DESCRIPTION),
      )
      EditToDoTextField(
          "Assignee",
          state.assignee,
          editToDoViewModel::updateAssignee,
          EditToDoScreenTestTags.INPUT_TODO_ASSIGNEE,
      )
      LocationSelection(
          query = state.locationQuery,
          suggestions = state.locationSuggestions,
          onQueryChange = editToDoViewModel::updateLocation,
          onLocationSelected = editToDoViewModel::selectLocation,
          testTag = EditToDoScreenTestTags.INPUT_TODO_LOCATION,
          suggestionTag = EditToDoScreenTestTags.LOCATION_SUGGESTION,
      )
      state.locationSearchError?.let {
        Text(it, modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
      }
      EditToDoTextField(
          "Due Date",
          state.dueDate,
          editToDoViewModel::updateDate,
          EditToDoScreenTestTags.INPUT_TODO_DATE,
      )
      Button(
          onClick = editToDoViewModel::cycleStatus,
          modifier = Modifier.testTag(EditToDoScreenTestTags.INPUT_TODO_STATUS),
      ) {
        Text(state.status.toString())
      }
      state.error?.let {
        Text(it, modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
      }
      Button(
          onClick = editToDoViewModel::save,
          modifier = Modifier.testTag(EditToDoScreenTestTags.TODO_SAVE),
      ) {
        Text("Save")
      }
      Button(
          onClick = editToDoViewModel::delete,
          modifier = Modifier.testTag(EditToDoScreenTestTags.TODO_DELETE),
      ) {
        Text("Delete")
      }
    }
  }
}

@Composable
private fun EditToDoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth().testTag(tag),
  )
}
