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

object AddToDoScreenTestTags {
  const val INPUT_TODO_TITLE = "inputTodoTitle"
  const val INPUT_TODO_DESCRIPTION = "inputTodoDescription"
  const val INPUT_TODO_ASSIGNEE = "inputTodoAssignee"
  const val INPUT_TODO_LOCATION = "inputTodoLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val INPUT_TODO_DATE = "inputTodoDate"
  const val TODO_SAVE = "todoSave"
  const val ERROR_MESSAGE = "errorMessage"
}

data class AddToDoUiState(
    val title: String = "",
    val description: String = "",
    val assignee: String = "",
    val locationQuery: String = "",
    val selectedLocation: Location? = null,
    val locationSuggestions: List<Location> = emptyList(),
    val locationSearchError: String? = null,
    val dueDate: String = "",
    val error: String? = null,
    val saveSucceeded: Boolean = false,
)

class AddToDoViewModel(
    private val todoRepository: ToDosRepository = ToDosRepositoryProvider.repository,
    private val locationRepository: LocationRepository = LocationRepositoryProvider.create(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddToDoUiState())
  val uiState: StateFlow<AddToDoUiState> = _uiState.asStateFlow()

  private val touchedFields = mutableSetOf<String>()
  private var locationSearchJob: Job? = null

  fun updateTitle(value: String) = update("title") { it.copy(title = value) }

  fun updateDescription(value: String) = update("description") { it.copy(description = value) }

  fun updateAssignee(value: String) = update("assignee") { it.copy(assignee = value) }

  fun updateDate(value: String) = update("dueDate") { it.copy(dueDate = value) }

  fun updateLocation(value: String) {
    update("location") {
      it.copy(
          locationQuery = value,
          selectedLocation = null,
          locationSearchError = null,
      )
    }
    locationSearchJob?.cancel()
    if (value.isBlank()) {
      _uiState.value =
          _uiState.value.copy(locationSuggestions = emptyList(), locationSearchError = null)
      return
    }
    locationSearchJob = viewModelScope.launch {
      try {
        val suggestions = locationRepository.search(value)
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
  }

  fun selectLocation(location: Location) {
    _uiState.value =
        _uiState.value.copy(
            locationQuery = location.name,
            selectedLocation = location,
            locationSuggestions = emptyList(),
        )
  }

  fun save() {
    val state = _uiState.value
    val error = saveValidationError(state)
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
      val todo =
          ToDo(
              uid = todoRepository.getNewUid(),
              name = state.title.trim(),
              description = state.description.trim(),
              assigneeName = state.assignee.trim(),
              dueDate = parseDate(state.dueDate),
              location = state.selectedLocation,
              status = ToDoStatus.CREATED,
              ownerId = ownerId,
          )
      try {
        todoRepository.addTodo(todo)
        _uiState.value = _uiState.value.copy(saveSucceeded = true, error = null)
      } catch (exception: CancellationException) {
        throw exception
      } catch (_: Exception) {
        _uiState.value = _uiState.value.copy(error = "Could not save the task.")
      }
    }
  }

  private fun update(field: String, change: (AddToDoUiState) -> AddToDoUiState) {
    touchedFields += field
    val next = change(_uiState.value)
    _uiState.value = next.copy(error = validationError(next))
  }

  private fun validationError(state: AddToDoUiState): String? =
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

  private fun saveValidationError(state: AddToDoUiState): String? =
      when {
        state.title.isBlank() -> "Title is required."
        state.description.isBlank() -> "Description is required."
        state.assignee.isBlank() -> "Assignee is required."
        !isValidDate(state.dueDate) -> "Enter a date as dd/MM/yyyy."
        else -> null
      }

  private fun isValidDate(value: String): Boolean = Regex("""\d{2}/\d{2}/\d{4}""").matches(value)

  private fun parseDate(value: String): Timestamp {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    formatter.isLenient = true
    return Timestamp(formatter.parse(value)!!)
  }
}

@Composable
fun AddTodoScreen(
    addToDoViewModel: AddToDoViewModel = viewModel(),
    onSaveSuccess: () -> Unit = {},
    onBack: () -> Unit = {},
) {
  val state by addToDoViewModel.uiState.collectAsState()

  LaunchedEffect(state.saveSucceeded) { if (state.saveSucceeded) onSaveSuccess() }

  Scaffold(topBar = { TodoTopBar("Create a new task", onBack) }) { innerPadding ->
    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
      AddToDoTextField(
          "Title",
          state.title,
          addToDoViewModel::updateTitle,
          AddToDoScreenTestTags.INPUT_TODO_TITLE,
      )
      OutlinedTextField(
          value = state.description,
          onValueChange = addToDoViewModel::updateDescription,
          label = { Text("Description") },
          modifier =
              Modifier.fillMaxWidth()
                  .heightIn(min = 100.dp)
                  .testTag(AddToDoScreenTestTags.INPUT_TODO_DESCRIPTION),
      )
      AddToDoTextField(
          "Assignee",
          state.assignee,
          addToDoViewModel::updateAssignee,
          AddToDoScreenTestTags.INPUT_TODO_ASSIGNEE,
      )
      LocationSelection(
          query = state.locationQuery,
          suggestions = state.locationSuggestions,
          onQueryChange = addToDoViewModel::updateLocation,
          onLocationSelected = addToDoViewModel::selectLocation,
          testTag = AddToDoScreenTestTags.INPUT_TODO_LOCATION,
          suggestionTag = AddToDoScreenTestTags.LOCATION_SUGGESTION,
      )
      state.locationSearchError?.let {
        Text(it, modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
      }
      AddToDoTextField(
          "Due Date",
          state.dueDate,
          addToDoViewModel::updateDate,
          AddToDoScreenTestTags.INPUT_TODO_DATE,
      )
      state.error?.let {
        Text(it, modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
      }
      Button(
          onClick = addToDoViewModel::save,
          modifier = Modifier.testTag(AddToDoScreenTestTags.TODO_SAVE),
      ) {
        Text("Save")
      }
    }
  }
}

@Composable
private fun AddToDoTextField(
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
