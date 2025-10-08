package snd.komf.mediaserver.stump

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import snd.komf.mediaserver.BookEvent
import snd.komf.mediaserver.MediaServerEventListener
import snd.komf.mediaserver.SeriesEvent
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.stump.model.StumpLibraryId
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

/**
 * Event handler for Stump server events.
 * 
 * Note: Stump currently uses GraphQL subscriptions for real-time events, but this
 * implementation uses polling as a fallback until proper subscription support is added.
 * 
 * TODO: Implement GraphQL subscription support when available:
 * - Connect to GraphQL subscription endpoint
 * - Subscribe to relevant events (media added, series removed, etc.)
 * - Process events in real-time instead of polling
 */
class StumpEventHandler(
    private val stumpClient: StumpClient,
    private val eventListeners: List<MediaServerEventListener>,
    private val pollingInterval: kotlin.time.Duration = 5.minutes // Poll every 5 minutes
) {
    private val eventHandlerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isActive: Boolean = false

    @Synchronized
    fun start() {
        if (isActive) return
        
        isActive = true
        logger.info { "Starting Stump event handler with polling (${pollingInterval})" }
        
        eventHandlerScope.launch {
            pollForChanges()
                .catch { error ->
                    logger.error(error) { "Error in Stump event polling" }
                }
                .collect()
        }
    }

    @Synchronized
    fun stop() {
        isActive = false
        logger.info { "Stopping Stump event handler" }
    }

    private fun pollForChanges() = flow {
        while (isActive) {
            try {
                checkForNewMedia()
                emit(Unit)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to poll for changes" }
            }
            
            delay(pollingInterval)
        }
    }

    private suspend fun checkForNewMedia() {
        try {
            // Get all libraries
            val libraries = stumpClient.getLibraries()
            
            for (library in libraries) {
                checkLibraryForChanges(library.id)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to check for new media" }
        }
    }

    private suspend fun checkLibraryForChanges(libraryId: StumpLibraryId) {
        try {
            // For now, just report that we're polling - actual change detection
            // would require storing state about what we've seen before
            logger.debug { "Polling library ${libraryId.value} for changes" }
            
            // TODO: Implement actual change detection by storing previous state
            // and comparing with current state to find new books/series
            
        } catch (e: Exception) {
            logger.warn(e) { "Failed to check library ${libraryId.value} for changes" }
        }
    }

    // TODO: Implement series deletion detection
    // This would require tracking known series and detecting when they're no longer returned
    private suspend fun checkForDeletedSeries() {
        // Implementation needed when series deletion events are required
        // For now, we only handle new content
    }
}