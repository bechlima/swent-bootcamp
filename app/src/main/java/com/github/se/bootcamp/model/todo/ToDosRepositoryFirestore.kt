package com.github.se.bootcamp.model.todo

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlinx.coroutines.tasks.await

const val TODOS_COLLECTION_PATH = "todos"

class ToDosRepositoryFirestore(private val db: FirebaseFirestore) : ToDosRepository {
  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun getAllTodos(): List<ToDo> {
    val uid =
        Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("Cannot fetch todos without an authenticated user")
    return db.collection(TODOS_COLLECTION_PATH)
        .whereEqualTo("ownerId", uid)
        .get()
        .await()
        .documents
        .map(::toTodo)
  }

  override suspend fun getTodo(todoID: String): ToDo {
    val snapshot = db.collection(TODOS_COLLECTION_PATH).document(todoID).get().await()
    if (!snapshot.exists()) {
      throw NoSuchElementException("Todo with id $todoID not found")
    }
    return toTodo(snapshot)
  }

  override suspend fun addTodo(toDo: ToDo) {
    db.collection(TODOS_COLLECTION_PATH).document(toDo.uid).set(toMap(toDo)).await()
  }

  override suspend fun editTodo(todoID: String, newValue: ToDo) {
    getTodo(todoID)
    db.collection(TODOS_COLLECTION_PATH).document(todoID).set(toMap(newValue)).await()
  }

  override suspend fun deleteTodo(todoID: String) {
    getTodo(todoID)
    db.collection(TODOS_COLLECTION_PATH).document(todoID).delete().await()
  }

  private fun toMap(toDo: ToDo): Map<String, Any?> =
      mapOf(
          "name" to toDo.name,
          "description" to toDo.description,
          "assigneeName" to toDo.assigneeName,
          "dueDate" to toDo.dueDate,
          "location" to
              toDo.location?.let {
                mapOf(
                    "latitude" to it.latitude,
                    "longitude" to it.longitude,
                    "name" to it.name,
                )
              },
          "status" to toDo.status.name,
          "ownerId" to toDo.ownerId,
      )

  private fun toTodo(document: DocumentSnapshot): ToDo {
    val location = document.get("location") as? Map<*, *>
    return ToDo(
        uid = document.id,
        name = document.getString("name")!!,
        description = document.getString("description")!!,
        assigneeName = document.getString("assigneeName")!!,
        dueDate = document.getTimestamp("dueDate")!!,
        location =
            location?.let {
              com.github.se.bootcamp.model.map.Location(
                  latitude = (it["latitude"] as Number).toDouble(),
                  longitude = (it["longitude"] as Number).toDouble(),
                  name = it["name"] as String,
              )
            },
        status = ToDoStatus.valueOf(document.getString("status")!!),
        ownerId = document.getString("ownerId")!!,
    )
  }
}
