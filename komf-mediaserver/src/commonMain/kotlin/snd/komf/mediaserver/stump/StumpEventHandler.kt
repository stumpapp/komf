package snd.komf.mediaserver.stump

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.encodeToString
import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId


private val logger = KotlinLogging.logger {}

/**
 * Event handler for Stump server events using GraphQL subscriptions.
 */
class StumpEventHandler(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authProvider: StumpAuthProvider,
    private val eventListeners: List<MediaServerEventListener>,
    private val json: Json
) {
    private val eventHandlerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isActive: Boolean = false

    @Synchronized
    fun start() {
        if (isActive) return
        
        isActive = true
        logger.info { "Starting Stump event handler with GraphQL subscriptions" }
        
        eventHandlerScope.launch {
            try {
                subscribeToEvents()
            } catch (e: Exception) {
                logger.error(e) { "Error in Stump event subscription" }
            }
        }
    }

    @Synchronized
    fun stop() {
        isActive = false
        logger.info { "Stopping Stump event handler" }
    }

    private suspend fun subscribeToEvents() {
        // Note: There are more events, but none I think that are useful for Komf. There isn't
        // currently an event for "deleted" books or series, but I think if people want to use this
        // with Stump I'll have to add it.
        val subscriptionQuery = """
            subscription ReadEvents {
                readEvents {
                    __typename
                    ... on CreatedMedia {
                        id
                        seriesId
                        libraryId
                    }
                }
            }
        """.trimIndent()

        try {
            val reqBuilder = HttpRequestBuilder()
            authProvider.addAuth(reqBuilder)
            val authHeaders = reqBuilder.headers.build()
            
            httpClient.webSocket(
                method = HttpMethod.Get,
                host = extractHost(baseUrl),
                port = extractPort(baseUrl),
                path = "/api/graphql",
                request = {
                    authHeaders.forEach { name, values ->
                        values.forEach { value ->
                            header(name, value)
                        }
                    }
                }
            ) {

                send(Frame.Text(json.encodeToString(mapOf(
                    "type" to "connection_init"
                ))))

                send(Frame.Text(json.encodeToString(mapOf(
                    "id" to "1",
                    "type" to "start",
                    "payload" to mapOf(
                        "query" to subscriptionQuery
                    )
                ))))

                while (isActive) {
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        handleWebSocketMessage(frame.readText())
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "WebSocket connection failed" }
            if (isActive) {
                // Retry after delay
                kotlinx.coroutines.delay(5000)
                subscribeToEvents()
            }
        }
    }

    private suspend fun handleWebSocketMessage(message: String) {
        try {
            val messageJson = json.parseToJsonElement(message).jsonObject
            val type = messageJson["type"]?.jsonPrimitive?.content
            
            if (type == "data") {
                val data = messageJson["payload"]?.jsonObject?.get("data")?.jsonObject
                val readEvents = data?.get("readEvents")?.jsonObject
                
                readEvents?.let { event ->
                    processEvent(event)
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to handle WebSocket message: $message" }
        }
    }

    private suspend fun processEvent(event: kotlinx.serialization.json.JsonObject) {
        val typeName = event["__typename"]?.jsonPrimitive?.content

        // Note: This is kinda the only event which Stump emits that Komf wants.
        // See MediaServerEventListener.kt
        when (typeName) {
            "CreatedMedia" -> {
                val mediaId = event["id"]?.jsonPrimitive?.content
                val seriesId = event["seriesId"]?.jsonPrimitive?.content
                val libraryId = event["libraryId"]?.jsonPrimitive?.content
                logger.debug { "Created media: $mediaId in series: $seriesId, library: $libraryId" }
                
                if (mediaId != null && seriesId != null && libraryId != null) {
                    notifyBookAdded(
                        MediaServerBookId(mediaId),
                        MediaServerSeriesId(seriesId),
                        MediaServerLibraryId(libraryId)
                    )
                }
            }
        }
    }



    private suspend fun notifyBookAdded(bookId: MediaServerBookId, seriesId: MediaServerSeriesId, libraryId: MediaServerLibraryId) {
        val event = BookEvent(
            libraryId = libraryId,
            seriesId = seriesId,
            bookId = bookId
        )
        
        eventListeners.forEach { listener ->
            try {
                listener.onBooksAdded(listOf(event))
            } catch (e: Exception) {
                logger.warn(e) { "Error notifying listener of book added: $bookId" }
            }
        }
    }



    private fun extractHost(url: String): String {
        return url.substringAfter("://").substringBefore(":").substringBefore("/")
    }

    private fun extractPort(url: String): Int {
        val afterProtocol = url.substringAfter("://")
        return if (":" in afterProtocol && "/" in afterProtocol) {
            val portSection = afterProtocol.substringAfter(":").substringBefore("/")
            portSection.toIntOrNull() ?: if (url.startsWith("https")) 443 else 80
        } else {
            if (url.startsWith("https")) 443 else 80
        }
    }
}