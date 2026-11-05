// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.model.map

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class NominatimLocationRepository(val client: OkHttpClient) : LocationRepository {
  private var rateLimiter = sharedRateLimiter

  internal constructor(client: OkHttpClient, testRateLimiter: RequestRateLimiter) : this(client) {
    rateLimiter = testRateLimiter
  }

  override suspend fun search(query: String): List<Location> {
    if (query.isBlank()) return emptyList()

    rateLimiter.awaitPermit()
    val url =
        NOMINATIM_SEARCH_URL.toHttpUrl()
            .newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("format", "json")
            .build()
    val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()

    return withContext(Dispatchers.IO) {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          throw IOException("Nominatim request failed with HTTP ${response.code}")
        }
        parseLocations(response.body.string())
      }
    }
  }

  private fun parseLocations(response: String): List<Location> {
    val locations = JSONArray(response)
    return buildList(locations.length()) {
      for (index in 0 until locations.length()) {
        val location = locations.getJSONObject(index)
        add(
            Location(
                latitude = location.getString("lat").toDouble(),
                longitude = location.getString("lon").toDouble(),
                name = location.optString("display_name", location.optString("name")),
            )
        )
      }
    }
  }

  private companion object {
    const val NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
    const val USER_AGENT =
        "EPFL Bootcamp ToDo App/1.0 (https://github.com/swent-epfl/bootcamp-26-bechlima)"

    val sharedRateLimiter = RequestRateLimiter()
  }
}

internal class RequestRateLimiter(
    private val now: () -> Long = System::currentTimeMillis,
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
  private val mutex = Mutex()
  private var lastRequestAt: Long? = null

  suspend fun awaitPermit() {
    mutex.withLock {
      val previousRequestAt = lastRequestAt
      if (previousRequestAt != null) {
        val waitTime = 1_000L - (now() - previousRequestAt)
        if (waitTime > 0) wait(waitTime)
      }
      lastRequestAt = now()
    }
  }
}
