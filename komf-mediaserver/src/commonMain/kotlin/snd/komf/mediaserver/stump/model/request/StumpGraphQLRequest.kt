package snd.komf.mediaserver.stump.model.request

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class StumpGraphQLRequest(
    val query: String,
    val variables: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class StumpGraphQLResponse<T>(
    val data: T? = null,
    val errors: List<StumpGraphQLError>? = null,
)

@Serializable
data class StumpGraphQLError(
    val message: String,
    val path: List<String>? = null,
    val extensions: Map<String, String>? = null,
)