// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.ui.map

import com.github.se.bootcamp.model.map.Location
import com.github.se.bootcamp.model.todo.ToDo
import com.github.se.bootcamp.model.todo.ToDoStatus
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test

class MapB3Test {

  @Test
  fun todosWithLocations_excludesTodosWithoutLocations() {
    val locatedTodo = todo("located", Location(1.0, 2.0, "Located"))
    val unlocatedTodo = todo("unlocated", null)

    assertEquals(listOf(locatedTodo), todosWithLocations(listOf(unlocatedTodo, locatedTodo)))
  }

  @Test
  fun todosWithLocations_retainsEveryLocatedTodo() {
    val locatedTodos =
        listOf(
            todo("first", Location(1.0, 2.0, "First")),
            todo("second", Location(3.0, 4.0, "Second")),
            todo("third", Location(5.0, 6.0, "Third")),
        )

    assertEquals(locatedTodos, todosWithLocations(locatedTodos))
  }

  @Test
  fun initialMapLocation_usesFirstLocatedTodo() {
    val firstLocated = Location(1.0, 2.0, "First")
    val secondLocated = Location(3.0, 4.0, "Second")

    assertEquals(
        firstLocated,
        initialMapLocation(
            listOf(
                todo("unlocated", null),
                todo("first", firstLocated),
                todo("second", secondLocated),
            )
        ),
    )
  }

  @Test
  fun initialMapLocation_usesEpflWhenNoTodoHasLocation() {
    assertEquals(
        Location(46.5191, 6.5668, "EPFL"),
        initialMapLocation(listOf(todo("unlocated", null))),
    )
  }

  private fun todo(uid: String, location: Location?): ToDo =
      ToDo(
          uid = uid,
          name = uid,
          description = "",
          assigneeName = "",
          dueDate = Timestamp.now(),
          location = location,
          status = ToDoStatus.CREATED,
          ownerId = "",
      )
}
