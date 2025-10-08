package snd.komf.mediaserver.stump

/**
 * An authentication provider that uses an API key for authentication
 */
class StumpApiKeyAuthProvider(private val apiKey: String) : StumpAuthProvider {
    override suspend fun addAuth(requestBuilder: io.ktor.client.request.HttpRequestBuilder) {
        requestBuilder.headers.append("Authorization", "Bearer $apiKey")
    }
}