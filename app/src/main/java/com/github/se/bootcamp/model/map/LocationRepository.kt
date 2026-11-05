// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.github.se.bootcamp.model.map

/** Provides location search results. */
interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
