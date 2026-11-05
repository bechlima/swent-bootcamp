package com.github.se.bootcamp.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.displayString
import java.text.SimpleDateFormat

object OverviewScreenTestTags {
  const val CREATE_TODO_BUTTON = "createTodoFab"
  const val LOGOUT_BUTTON = "logoutButton"
  const val EMPTY_TODO_LIST_MSG = "emptyTodoList"
  const val TODO_LIST = "todoList"

  fun getTestTagForTodoItem(todo: ToDo): String = "todoItem${todo.uid}"
}

@Composable
fun OverviewScreen(
    overviewViewModel: OverviewViewModel = viewModel(),
    onButtonClick: () -> Unit = {},
    onTodoClick: (ToDo) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    /**
     * You can add parameters with default values here, e.g., onClick: () -> Unit = {}, modifier:
     * Modifier = Modifier, navController: NavHostController = rememberNavController(),
     *
     * You should not modify the `overviewViewModel` parameter.
     *
     * Note: Parameters with default values do not mean that you should use the default value in
     * your implementation. They ensure that we can still use your code in our tests.
     */
) {
  LaunchedEffect(Unit) { overviewViewModel.loadTodos() }

  val todos by overviewViewModel.todos.collectAsState()
  val error by overviewViewModel.error.collectAsState()
  Column(modifier = Modifier.fillMaxSize()) {
    error?.let { Text(it) }
    if (todos.isEmpty()) {
      Text(
          "Todo list is empty",
          modifier = Modifier.testTag(OverviewScreenTestTags.EMPTY_TODO_LIST_MSG),
      )
    }
    Card {
      Button(
          onClick = onButtonClick,
          modifier = Modifier.testTag(OverviewScreenTestTags.CREATE_TODO_BUTTON),
      ) {}
    }
    Button(
        onClick = onLogoutClick,
        modifier = Modifier.testTag(OverviewScreenTestTags.LOGOUT_BUTTON),
    ) {
      Text("Log out")
    }
    if (todos.isNotEmpty()) {
      LazyColumn(modifier = Modifier.testTag(OverviewScreenTestTags.TODO_LIST)) {
        items(todos) { todo -> TodoListItem(todo, onTodoClick) }
      }
    }
  }
}

@Composable
fun TodoListItem(todo: ToDo, onTodoClick: (ToDo) -> Unit = {}) {
  Card(
      modifier =
          Modifier.testTag(OverviewScreenTestTags.getTestTagForTodoItem(todo)).clickable {
            onTodoClick(todo)
          }
  ) {
    Column {
      Text(todo.name)
      Text(todo.status.displayString())
      Text(todo.assigneeName)

      val formatter = SimpleDateFormat("dd/MM/yyyy", LocalLocale.current.platformLocale)
      val formattedDate = formatter.format(todo.dueDate.toDate())
      Text(formattedDate)
    }
  }
}
