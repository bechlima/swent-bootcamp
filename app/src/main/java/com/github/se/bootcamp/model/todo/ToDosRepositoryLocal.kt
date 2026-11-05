package com.github.se.bootcamp.model.todo

import java.util.UUID

/** Represents a repository that manages a local list of todos. */
class ToDosRepositoryLocal : ToDosRepository {
  private val todos = mutableListOf<ToDo>()

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllTodos(): List<ToDo> {
    return todos.toList()
  }

  override suspend fun getTodo(todoID: String): ToDo {
    return todos.firstOrNull { it.uid == todoID }
        ?: throw NoSuchElementException("Todo with id $todoID not found")
  }

  override suspend fun addTodo(toDo: ToDo) {
    todos.add(toDo)
  }

  override suspend fun editTodo(todoID: String, newValue: ToDo) {
    val index = todos.indexOfFirst { it.uid == todoID }

    if (index == -1) {
      throw NoSuchElementException("Todo with id $todoID not found")
    }

    todos[index] = newValue
  }

  override suspend fun deleteTodo(todoID: String) {
    val removed = todos.removeIf { it.uid == todoID }

    if (!removed) {
      throw NoSuchElementException("Todo with id $todoID not found")
    }
  }
}
