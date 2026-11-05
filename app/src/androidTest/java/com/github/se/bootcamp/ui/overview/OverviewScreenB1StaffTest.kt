package com.github.se.bootcamp.ui.overview

// ***************************************************************************** //
// ***                                                                       *** //
// *** THIS FILE CONTAIN STAFF TESTS. IT SHOULD NOT BE PUSHED ON THE         *** //
// *** STUDENT'S REPOSITORY.                                                 *** //
// ***                                                                       *** //
// ***************************************************************************** //

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.github.se.bootcamp.model.todo.ToDosRepository
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlin.collections.last
import org.junit.Rule
import org.junit.Test

class OverviewScreenB1StaffTest {
  @get:Rule val composeTestRule = createComposeRule()

  private class ToDosRepositoryImpl(val todoList: List<ToDo> = listOf<ToDo>()) : ToDosRepository {
    override suspend fun addTodo(toDo: ToDo) {
      return
    }

    override suspend fun editTodo(todoID: String, newValue: ToDo) {
      TODO("Not yet implemented")
    }

    override suspend fun deleteTodo(todoID: String) {
      TODO("Not yet implemented")
    }

    override fun getNewUid(): String {
      return "${todoList.size}"
    }

    override suspend fun getAllTodos(): List<ToDo> {
      return todoList
    }

    override suspend fun getTodo(todoID: String): ToDo {
      TODO("Not yet implemented")
    }
  }

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

  fun setContent(withRepository: ToDosRepository) {
    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = OverviewViewModel(todoRepository = withRepository))
    }
  }

  /**
   * Test that the list of todos is lazy loaded. We create 100 todos and check that the last one is
   * not displayed without scrolling.
   */
  @Test
  fun todoListIsLazy() {
    val todos = (1..100).map { i -> todo1.copy(uid = i.toString(), name = "${todo1.name} #$i") }
    setContent(withRepository = ToDosRepositoryImpl(todos))

    val listTag = OverviewScreenTestTags.TODO_LIST
    val lastTag = OverviewScreenTestTags.getTestTagForTodoItem(todos.last())

    // Before any scrolling, the last item should NOT be composed at all.
    composeTestRule.onNodeWithTag(lastTag, useUnmergedTree = true).assertDoesNotExist()

    // Scroll until the last item comes into view.
    composeTestRule.onNodeWithTag(listTag).performScrollToNode(hasTestTag(lastTag))

    // After scrolling, it should now exist and be visible.
    composeTestRule
        .onNodeWithTag(lastTag, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  /**
   * Test that the due date is correctly formatted as "dd/MM/yyyy" for days, months, and years. In
   * particular, this checks that days and months are displayed with 2 digits (e.g., "02") and that
   * years are displayed with 4 digits (e.g., "0601" instead of "601").
   */
  @Test
  fun dueDateIsCorrectlyFormattedForDay() {
    val todo1 = todo1.copy(uid = "1", dueDate = getTimestamp(2023, Calendar.DECEMBER, 2))
    val todoList = listOf(todo1)
    val dueDate1 = "02/12/2023"
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo1, hasText(dueDate1, substring = false, ignoreCase = false))
  }

  /**
   * Test that the due date is correctly formatted as "dd/MM/yyyy" for days, months, and years. In
   * particular, this checks that days and months are displayed with 2 digits (e.g., "02") and that
   * years are displayed with 4 digits (e.g., "0601" instead of "601").
   */
  @Test
  fun dueDateIsCorrectlyFormattedForMonth() {
    val todo1 = todo1.copy(uid = "1", dueDate = getTimestamp(2023, Calendar.JANUARY, 25))
    val todoList = listOf(todo1)
    val dueDate1 = "25/01/2023"
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo1, hasText(dueDate1, substring = false, ignoreCase = false))
  }

  /**
   * Test that the due date is correctly formatted as "dd/MM/yyyy" for days, months, and years. In
   * particular, this checks that days and months are displayed with 2 digits (e.g., "02") and that
   * years are displayed with 4 digits (e.g., "0601" instead of "601").
   */
  @Test
  fun dueDateIsCorrectlyFormattedForYear() {
    val todo1 = todo1.copy(uid = "1", dueDate = getTimestamp(601, Calendar.JANUARY, 25))
    val todoList = listOf(todo1)
    val dueDate1 = "25/01/0601"
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo1, hasText(dueDate1, substring = false, ignoreCase = false))
  }

  /**
   * Asserts that [todo]'s item contains a descendant matching [matcher], looking at the unmerged
   * tree: the tagged container is a plain layout, so it does not merge its children's semantics and
   * carries no text of its own.
   */
  private fun onTodoItem(todo: ToDo, matcher: SemanticsMatcher) {
    composeTestRule
        .onNode(
            hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(todo))
                .and(hasAnyDescendant(matcher)),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
  }
}
