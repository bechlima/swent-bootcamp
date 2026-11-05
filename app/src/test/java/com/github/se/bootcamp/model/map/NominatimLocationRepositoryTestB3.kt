// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.model.map

import java.io.IOException
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.junit.Test

class NominatimLocationRepositoryTestB3 {

  @Test
  fun search_withBlankQuery_returnsEmptyListWithoutCallingHttpClient() = runTest {
    var requestCount = 0
    val client =
        OkHttpClient.Builder()
            .addInterceptor {
              requestCount += 1
              error("HTTP client should not be called for a blank query")
            }
            .build()

    assertEquals(emptyList<Location>(), NominatimLocationRepository(client).search("  "))
    assertEquals(0, requestCount)
  }

  @Test
  fun search_mapsAllResultsAndBuildsNominatimRequest() = runTest {
    var request: Request? = null
    val responseBody =
        """
        [
          {"display_name":"EPFL, Lausanne, Switzerland","lat":"46.5191","lon":"6.5668"},
          {"display_name":"Lausanne, Switzerland","lat":"46.5197","lon":"6.6323"}
        ]
        """
            .trimIndent()
            .toResponseBody("application/json".toMediaType())
    val client =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
              request = chain.request()
              responseFor(200, responseBody, chain.request())
            }
            .build()

    val locations = NominatimLocationRepository(client).search("Lausanne")

    assertEquals(
        listOf(
            Location(
                name = "EPFL, Lausanne, Switzerland",
                latitude = 46.5191,
                longitude = 6.5668,
            ),
            Location(
                name = "Lausanne, Switzerland",
                latitude = 46.5197,
                longitude = 6.6323,
            ),
        ),
        locations,
    )
    val capturedRequest = checkNotNull(request)
    assertEquals("Lausanne", capturedRequest.url.queryParameter("q"))
    assertEquals("json", capturedRequest.url.queryParameter("format"))
    assertEquals(
        "EPFL Bootcamp ToDo App/1.0 (https://github.com/swent-epfl/bootcamp-26-bechlima)",
        capturedRequest.header("User-Agent"),
    )
  }

  @Test
  fun search_withEmptyJsonArray_returnsEmptyList() = runTest {
    val client = clientFor(200, "[]".toResponseBody())

    val locations = NominatimLocationRepository(client).search("nowhere")

    assertEquals(emptyList<Location>(), locations)
  }

  @Test
  fun search_withNonSuccessfulResponse_throwsIOException() = runTest {
    val client = clientFor(503, "Service unavailable".toResponseBody())

    val exception =
        try {
          NominatimLocationRepository(client).search("Lausanne")
          null
        } catch (error: IOException) {
          error
        }

    assertTrue(exception != null)
    assertEquals("Nominatim request failed with HTTP 503", checkNotNull(exception).message)
  }

  @Test
  fun search_usesNameWhenDisplayNameIsMissing() = runTest {
    val client =
        clientFor(
            200,
            """[{"name":"EPFL","lat":"46.5191","lon":"6.5668"}]"""
                .toResponseBody("application/json".toMediaType()),
        )

    assertEquals(
        listOf(Location(46.5191, 6.5668, "EPFL")),
        NominatimLocationRepository(client).search("EPFL"),
    )
  }

  @Test
  fun search_withMalformedJson_throwsJsonException() = runTest {
    val client = clientFor(200, """{"not":"an array"}""".toResponseBody())

    var exception: JSONException? = null
    try {
      NominatimLocationRepository(client).search("EPFL")
    } catch (error: JSONException) {
      exception = error
    }

    assertTrue(exception != null)
  }

  @Test
  fun search_withMalformedCoordinates_throwsNumberFormatException() = runTest {
    val client =
        clientFor(
            200,
            """[{"display_name":"EPFL","lat":"not-a-number","lon":"6.5668"}]"""
                .toResponseBody("application/json".toMediaType()),
        )

    var exception: NumberFormatException? = null
    try {
      NominatimLocationRepository(client).search("EPFL")
    } catch (error: NumberFormatException) {
      exception = error
    }

    assertTrue(exception != null)
  }

  @Test
  fun search_withTransportFailure_throwsIOException() = runTest {
    val client =
        OkHttpClient.Builder().addInterceptor { throw IOException("network unavailable") }.build()

    var exception: IOException? = null
    try {
      NominatimLocationRepository(client).search("EPFL")
    } catch (error: IOException) {
      exception = error
    }

    assertEquals("network unavailable", checkNotNull(exception).message)
  }

  @Test
  fun search_waitsBetweenRequestsThroughRepositoryRateLimiter() = runTest {
    var currentTime = 0L
    val waits = mutableListOf<Long>()
    val limiter =
        RequestRateLimiter(
            now = { currentTime },
            wait = { duration ->
              waits += duration
              currentTime += duration
            },
        )

    val client =
        OkHttpClient.Builder()
            .addInterceptor { chain -> responseFor(200, "[]".toResponseBody(), chain.request()) }
            .build()
    val repository = NominatimLocationRepository(client, limiter)

    repository.search("first")
    currentTime = 100L
    repository.search("second")

    assertEquals(listOf(900L), waits)
  }

  private fun clientFor(code: Int, body: okhttp3.ResponseBody): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor { chain -> responseFor(code, body, chain.request()) }
        .build()
  }

  private fun responseFor(
      code: Int,
      body: okhttp3.ResponseBody,
      request: Request,
  ): Response {
    return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code in 200..299) "OK" else "Error")
        .body(body)
        .build()
  }
}
