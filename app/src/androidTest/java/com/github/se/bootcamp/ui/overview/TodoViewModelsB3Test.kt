// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.overview

import androidx.test.platform.app.InstrumentationRegistry
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.github.se.bootcamp.utils.BootcampMilestone
import com.github.se.bootcamp.utils.FirebaseEmulator
import com.github.se.bootcamp.utils.InMemoryTest
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TodoViewModelsB3Test : InMemoryTest(BootcampMilestone.B3) {
  private lateinit var fakeRepository: RecordingToDosRepository

  @Before
  override fun setUp() {
    super.setUp()
    fakeRepository = RecordingToDosRepository()
    runTest { FirebaseEmulator.auth.signInAnonymously().await() }
  }

  @Test
  fun addInvalidRequiredInput_doesNotSave() = runTest {
    val viewModel = AddToDoViewModel(fakeRepository, EmptyLocationRepository)

    viewModel.save()
    waitForMainThread()

    assertEquals("Title is required.", viewModel.uiState.value.error)
    assertNull(fakeRepository.addedTodo)
  }

  @Test
  fun addWithoutLocation_savesTodoAndMarksSuccess() = runTest {
    val viewModel = AddToDoViewModel(fakeRepository, EmptyLocationRepository)
    fillAddFields(viewModel)

    viewModel.save()
    waitForMainThread()

    assertEquals(true, viewModel.uiState.value.saveSucceeded)
    assertEquals(FirebaseEmulator.auth.currentUser!!.uid, fakeRepository.addedTodo!!.ownerId)
    assertEquals(null, fakeRepository.addedTodo!!.location)
  }

  @Test
  fun addWithLocation_preservesSelectedLocation() = runTest {
    val viewModel = AddToDoViewModel(fakeRepository, EmptyLocationRepository)
    val location = Location(46.5191, 6.5668, "EPFL")
    fillAddFields(viewModel)
    viewModel.selectLocation(location)

    viewModel.save()
    waitForMainThread()

    assertEquals(location, fakeRepository.addedTodo!!.location)
  }

  @Test
  fun addRepositoryFailure_exposesSaveError() = runTest {
    fakeRepository.addFailure = IllegalStateException("write failed")
    val viewModel = AddToDoViewModel(fakeRepository, EmptyLocationRepository)
    fillAddFields(viewModel)

    viewModel.save()
    waitForMainThread()

    assertEquals("Could not save the task.", viewModel.uiState.value.error)
    assertEquals(false, viewModel.uiState.value.saveSucceeded)
  }

  @Test
  fun editInvalidRequiredInput_doesNotUpdate() = runTest {
    val original = sampleTodo()
    fakeRepository.todos += original
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)
    viewModel.loadTodo(original.uid)
    waitForMainThread()
    viewModel.updateTitle("")

    viewModel.save()
    waitForMainThread()

    assertEquals("Title is required.", viewModel.uiState.value.error)
    assertNull(fakeRepository.editedTodo)
  }

  @Test
  fun editPreservesSelectedLocationAndChangedFields() = runTest {
    val original = sampleTodo()
    fakeRepository.todos += original
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)
    viewModel.loadTodo(original.uid)
    waitForMainThread()
    val location = Location(46.52, 6.57, "Lausanne")
    viewModel.updateTitle("Updated")
    viewModel.selectLocation(location)

    viewModel.save()
    waitForMainThread()

    assertEquals("Updated", fakeRepository.editedTodo!!.name)
    assertEquals(FirebaseEmulator.auth.currentUser!!.uid, fakeRepository.editedTodo!!.ownerId)
    assertEquals(location, fakeRepository.editedTodo!!.location)
    assertEquals(true, viewModel.uiState.value.operationSucceeded)
  }

  @Test
  fun editRepositoryFailure_exposesSaveError() = runTest {
    val original = sampleTodo()
    fakeRepository.todos += original
    fakeRepository.editFailure = IllegalStateException("write failed")
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)
    viewModel.loadTodo(original.uid)
    waitForMainThread()

    viewModel.save()
    waitForMainThread()

    assertEquals("Could not save the task.", viewModel.uiState.value.error)
    assertEquals(false, viewModel.uiState.value.operationSucceeded)
  }

  @Test
  fun deleteSuccess_invokesRepositoryAndMarksOperationSucceeded() = runTest {
    val original = sampleTodo()
    fakeRepository.todos += original
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)
    viewModel.loadTodo(original.uid)
    waitForMainThread()

    viewModel.delete()
    waitForMainThread()

    assertEquals(original.uid, fakeRepository.deletedTodoId)
    assertEquals(true, viewModel.uiState.value.operationSucceeded)
  }

  @Test
  fun loadFailure_exposesLoadError() = runTest {
    fakeRepository.getFailure = IllegalStateException("read failed")
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)

    viewModel.loadTodo("missing")
    waitForMainThread()

    assertEquals("Could not load the task.", viewModel.uiState.value.error)
  }

  @Test
  fun deleteFailure_exposesDeleteError() = runTest {
    val original = sampleTodo()
    fakeRepository.todos += original
    fakeRepository.deleteFailure = IllegalStateException("delete failed")
    val viewModel = EditToDoViewModel(fakeRepository, EmptyLocationRepository)
    viewModel.loadTodo(original.uid)
    waitForMainThread()

    viewModel.delete()
    waitForMainThread()

    assertEquals("Could not delete the task.", viewModel.uiState.value.error)
    assertEquals(false, viewModel.uiState.value.operationSucceeded)
  }

  private fun fillAddFields(viewModel: AddToDoViewModel) {
    viewModel.updateTitle("Task")
    viewModel.updateDescription("Description")
    viewModel.updateAssignee("Owner")
    viewModel.updateDate("23/09/2026")
  }

  private fun waitForMainThread() {
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
  }

  private fun sampleTodo() =
      ToDo(
          "todo",
          "Task",
          "Description",
          "Owner",
          Timestamp(calendar()),
          null,
          ToDoStatus.CREATED,
          "old",
      )

  private fun calendar(): java.util.Date {
    val calendar = Calendar.getInstance()
    calendar.set(2026, Calendar.SEPTEMBER, 23, 0, 0, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
  }

  private object EmptyLocationRepository : com.github.se.bootcamp.model.map.LocationRepository {
    override suspend fun search(query: String): List<Location> = emptyList()
  }

  private class RecordingToDosRepository : ToDosRepository {
    val todos = mutableListOf<ToDo>()
    var addedTodo: ToDo? = null
    var editedTodo: ToDo? = null
    var deletedTodoId: String? = null
    var addFailure: Exception? = null
    var editFailure: Exception? = null
    var getFailure: Exception? = null
    var deleteFailure: Exception? = null

    override fun getNewUid(): String = "new-todo"

    override suspend fun getAllTodos(): List<ToDo> = todos

    override suspend fun getTodo(todoID: String): ToDo {
      getFailure?.let { throw it }
      return todos.first { it.uid == todoID }
    }

    override suspend fun addTodo(toDo: ToDo) {
      addFailure?.let { throw it }
      addedTodo = toDo
    }

    override suspend fun editTodo(todoID: String, newValue: ToDo) {
      editFailure?.let { throw it }
      editedTodo = newValue
    }

    override suspend fun deleteTodo(todoID: String) {
      deleteFailure?.let { throw it }
      deletedTodoId = todoID
    }
  }
}
