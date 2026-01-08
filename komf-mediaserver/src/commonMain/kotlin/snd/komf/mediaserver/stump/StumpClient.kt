package snd.komf.mediaserver.stump

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import snd.komf.ktor.rateLimiter
import snd.komf.mediaserver.stump.model.*
import snd.komf.mediaserver.stump.model.request.*
import snd.komf.model.Image
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * Stump media server client implementation.
 */
class StumpClient(
    private val ktor: HttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val authProvider: StumpAuthProvider,
) {
    private val updatesRateLimiter = rateLimiter(eventsPerInterval = 120, interval = 60.seconds)

    suspend fun getLibraries(): List<StumpLibrary> {
        val response = executeGraphQLQuery<GetLibrariesResponse>(
            query = """
                query GetLibraries(${'$'}pagination: Pagination!) {
                    libraries(pagination: ${'$'}pagination) {
                        nodes {
                            id
                            name
                            description
                            path
                            status
                            createdAt
                            updatedAt
                            tags {
                                id
                                name
                            }
                            config {
                                convertRarToZip
                                hardDeleteConversions
                                generateFileHashes
                                processMetadata
                                thumbnailConfig {
                                    format
                                    quality
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "pagination" to mapOf(
                    "none" to mapOf(
                        "unpaginated" to true
                    )
                )
            )
        )
        return response.libraries.nodes
    }

    suspend fun getLibrary(libraryId: StumpLibraryId): StumpLibrary {
        return executeGraphQLQuery<GetLibraryResponse>(
            query = """
                query GetLibrary(${'$'}id: ID!) {
                    libraryById(id: ${'$'}id) {
                        id
                        name
                        description
                        path
                        status
                        createdAt
                        updatedAt
                        tags {
                            id
                            name
                        }
                        config {
                            convertRarToZip
                            hardDeleteConversions
                            generateFileHashes
                            processMetadata
                            thumbnailConfig {
                                format
                                quality
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to libraryId.value)
        ).libraryById ?: throw StumpGraphQLException("Library not found: ${libraryId.value}")
    }

    suspend fun scanLibrary(libraryId: StumpLibraryId) {
        executeGraphQLMutation<Any>(
          mutation = """
              mutation ScanLibrary(${'$'}id: ID!) {
                  scanLibrary(id: ${'$'}id)
              }
          """.trimIndent(),
          variables = mapOf("id" to libraryId.value)
        )
    }

    suspend fun getSeries(libraryId: StumpLibraryId, page: Int = 1, pageSize: Int = 500): StumpPage<StumpSeries> {
        val response = executeGraphQLQuery<GetSeriesResponse>(
            query = """
                query GetSeries(${'$'}filter: SeriesFilterInput!, ${'$'}pagination: Pagination!) {
                    series(filter: ${'$'}filter, pagination: ${'$'}pagination) {
                        nodes {
                            id
                            name
                            description
                            path
                            status
                            createdAt
                            updatedAt
                            libraryId
                            mediaCount
                            tags {
                                id
                                name
                            }
                            metadata {
                                seriesId
                                ageRating
                                booktype
                                comicid
                                comicImage
                                descriptionFormatted
                                imprint
                                metaType
                                publicationRun
                                publisher
                                status
                                summary
                                title
                                totalIssues
                                volume
                                year
                                characters
                                collects {
                                    series
                                    comicid
                                    issueid
                                    issues
                                }
                                genres
                                links
                                writers
                            }
                        }
                        pageInfo {
                            ... on OffsetPaginationInfo {
                                totalPages
                                totalItems
                                currentPage
                                pageSize
                                pageOffset
                                zeroBased
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "filter" to mapOf(
                    "libraryId" to mapOf("eq" to libraryId.value)
                ),
                "pagination" to mapOf(
                    "offset" to mapOf(
                        "page" to page,
                        "pageSize" to pageSize
                    )
                )
            )
        )
        
        return StumpPage(
            content = response.series.nodes,
            totalPages = response.series.pageInfo?.totalPages,
            totalElements = response.series.pageInfo?.totalItems,
            currentPage = response.series.pageInfo?.currentPage ?: page,
            pageSize = response.series.pageInfo?.pageSize ?: pageSize,
            hasNext = response.series.pageInfo?.let { it.currentPage < it.totalPages } ?: false,
            hasPrevious = response.series.pageInfo?.let { it.currentPage > 0 } ?: false
        )
    }

    suspend fun getSeries(seriesId: StumpSeriesId): StumpSeries {
        return executeGraphQLQuery<GetSeriesDetailResponse>(
            query = """
                query GetSeriesDetail(${'$'}id: ID!) {
                    seriesById(id: ${'$'}id) {
                        id
                        name
                        description
                        path
                        status
                        createdAt
                        updatedAt
                        libraryId
                        mediaCount
                        tags {
                            id
                            name
                        }
                        metadata {
                            seriesId
                            title
                            summary
                            publisher
                            status
                            genres
                            ageRating
                            booktype
                            comicid
                            comicImage
                            descriptionFormatted
                            imprint
                            metaType
                            publicationRun
                            totalIssues
                            volume
                            year
                            characters
                            collects {
                                series
                                comicid
                                issueid
                                issues
                            }
                            links
                            writers
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to seriesId.value)
        ).seriesById ?: throw StumpGraphQLException("Series not found: ${seriesId.value}")
    }

    suspend fun updateSeriesMetadata(seriesId: StumpSeriesId, metadata: StumpSeriesMetadataInput) {
        updatesRateLimiter.acquire()
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UpdateSeriesMetadata(${'$'}id: ID!, ${'$'}input: SeriesMetadataInput!) {
                    updateSeriesMetadata(id: ${'$'}id, input: ${'$'}input) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to seriesId.value,
                "input" to json.encodeToJsonElement(metadata)
            )
        )
    }

    suspend fun scanSeries(seriesId: StumpSeriesId): Boolean {
        return executeGraphQLMutation<ScanSeriesResponse>(
            mutation = """
                mutation ScanSeries(${'$'}id: ID!) {
                    scanSeries(id: ${'$'}id)
                }
            """.trimIndent(),
            variables = mapOf("id" to seriesId.value)
        ).scanSeries
    }

    suspend fun getMedia(seriesId: StumpSeriesId, page: Int = 1, pageSize: Int = 500): StumpPage<StumpMedia> {
        val response = executeGraphQLQuery<GetMediaResponse>(
            query = """
                query GetMedia(${'$'}filter: MediaFilterInput!, ${'$'}pagination: Pagination!) {
                    media(filter: ${'$'}filter, pagination: ${'$'}pagination) {
                        nodes {
                            id
                            name
                            path
                            size
                            extension
                            pages
                            status
                            createdAt
                            updatedAt
                            seriesId
                            libraryId
                            hash
                            seriesPosition
                            series {
                                id
                                name
                                resolvedName
                            }
                            metadata {
                                id
                                mediaId
                                ageRating
                                format
                                day
                                language
                                month
                                notes
                                number
                                pageCount
                                publisher
                                series
                                seriesGroup
                                storyArc
                                storyArcNumber
                                summary
                                title
                                titleSort
                                volume
                                year
                                identifierIsbn
                                writers
                                genres
                                characters
                                colorists
                                coverArtists
                                editors
                                inkers
                                letterers
                                links
                                pencillers
                                teams
                            }
                        }
                        pageInfo {
                            ... on OffsetPaginationInfo {
                                totalPages
                                totalItems
                                currentPage
                                pageSize
                                pageOffset
                                zeroBased
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "filter" to mapOf(
                    "seriesId" to mapOf("eq" to seriesId.value)
                ),
                "pagination" to mapOf(
                    "offset" to mapOf(
                        "page" to page,
                        "pageSize" to pageSize
                    )
                )
            )
        )
        
        return StumpPage(
            content = response.media.nodes,
            totalPages = response.media.pageInfo?.totalPages,
            totalElements = response.media.pageInfo?.totalItems,
            currentPage = response.media.pageInfo?.currentPage ?: page,
            pageSize = response.media.pageInfo?.pageSize ?: pageSize,
            hasNext = response.media.pageInfo?.let { it.currentPage < it.totalPages } ?: false,
            hasPrevious = response.media.pageInfo?.let { it.currentPage > 0 } ?: false
        )
    }

    suspend fun getAllMedia(seriesId: StumpSeriesId): List<StumpMedia> {
        val response = executeGraphQLQuery<GetAllMediaResponse>(
            query = """
                query GetAllMedia(${'$'}filter: MediaFilterInput!, ${'$'}pagination: Pagination!) {
                    media(filter: ${'$'}filter, pagination: ${'$'}pagination) {
                        nodes {
                            id
                            name
                            path
                            size
                            extension
                            pages
                            status
                            createdAt
                            updatedAt
                            seriesId
                            libraryId
                            hash
                            seriesPosition
                            series {
                                id
                                name
                                resolvedName
                            }
                            metadata {
                                id
                                mediaId
                                ageRating
                                format
                                day
                                language
                                month
                                notes
                                number
                                pageCount
                                publisher
                                series
                                seriesGroup
                                storyArc
                                storyArcNumber
                                summary
                                title
                                titleSort
                                volume
                                year
                                identifierIsbn
                                writers
                                genres
                                characters
                                colorists
                                coverArtists
                                editors
                                inkers
                                letterers
                                links
                                pencillers
                                teams
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "filter" to mapOf(
                    "seriesId" to mapOf("eq" to seriesId.value)
                ),
                "pagination" to mapOf(
                    "none" to mapOf(
                        "unpaginated" to true
                    )
                )
            )
        )
        
        return response.media.nodes
    }

    suspend fun getMedia(mediaId: StumpMediaId): StumpMedia {
        return executeGraphQLQuery<GetMediaDetailResponse>(
            query = """
                query GetMediaDetail(${'$'}id: ID!) {
                    mediaById(id: ${'$'}id) {
                        id
                        name
                        path
                        size
                        extension
                        pages
                        status
                        createdAt
                        updatedAt
                        seriesId
                        libraryId
                        hash
                        seriesPosition
                        metadata {
                            id
                            mediaId
                            ageRating
                            format
                            day
                            language
                            month
                            notes
                            number
                            pageCount
                            publisher
                            series
                            seriesGroup
                            storyArc
                            storyArcNumber
                            summary
                            title
                            titleSort
                            volume
                            year
                            identifierIsbn
                            writers
                            genres  
                            characters
                            colorists
                            coverArtists
                            editors
                            inkers
                            letterers
                            links
                            pencillers
                            teams
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to mediaId.value)
        ).mediaById ?: throw StumpGraphQLException("Media not found: ${mediaId.value}")
    }

    suspend fun getMediaWithSeries(mediaId: StumpMediaId): StumpMediaWithSeries {
        return executeGraphQLQuery<GetMediaWithSeriesResponse>(
            query = """
                query GetMediaWithSeries(${'$'}id: ID!) {
                    mediaById(id: ${'$'}id) {
                        id
                        name
                        path
                        size
                        extension
                        pages
                        status
                        createdAt
                        updatedAt
                        seriesId
                        libraryId
                        hash
                        seriesPosition
                        metadata {
                            id
                            mediaId
                            ageRating
                            format
                            day
                            language
                            month
                            notes
                            number
                            pageCount
                            publisher
                            series
                            seriesGroup
                            storyArc
                            storyArcNumber
                            summary
                            title
                            titleSort
                            volume
                            year
                            identifierIsbn
                            writers
                            genres  
                            characters
                            colorists
                            coverArtists
                            editors
                            inkers
                            letterers
                            links
                            pencillers
                            teams
                        }
                        series {
                            id
                            name
                            resolvedName
                            metadata {
                                seriesId
                                ageRating
                                booktype
                                comicid
                                comicImage
                                descriptionFormatted
                                imprint
                                metaType
                                publicationRun
                                publisher
                                status
                                summary
                                title
                                totalIssues
                                volume
                                year
                                characters
                                collects {
                                    series
                                    comicid
                                    issueid
                                    issues
                                }
                                genres
                                links
                                writers
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to mediaId.value)
        ).mediaById ?: throw StumpGraphQLException("Media not found: ${mediaId.value}")
    }

    suspend fun updateMediaMetadata(mediaId: StumpMediaId, metadata: StumpMediaMetadataInput) {
        updatesRateLimiter.acquire()
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UpdateMediaMetadata(${'$'}id: ID!, ${'$'}input: MediaMetadataInput!) {
                    updateMediaMetadata(id: ${'$'}id, input: ${'$'}input) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to mediaId.value,
                "input" to json.encodeToJsonElement(metadata)
            )
        )
    }

    suspend fun getSeriesCover(seriesId: StumpSeriesId): Image {
        val response: HttpResponse = ktor.get("api/v2/series/${seriesId.value}/thumbnail") {
            authProvider.addAuth(this)
        }
        
        if (response.status == HttpStatusCode.NotFound) {
            throw StumpResourceNotFoundException("Series cover not found")
        }
        
        val contentType = response.contentType()
        return Image(response.body<ByteArray>(), contentType?.toString())
    }

    suspend fun getMediaCover(mediaId: StumpMediaId): Image {
        val response: HttpResponse = ktor.get("api/v2/media/${mediaId.value}/thumbnail") {
            authProvider.addAuth(this)
        }
        
        if (response.status == HttpStatusCode.NotFound) {
            throw StumpResourceNotFoundException("Media cover not found")
        }
        
        val contentType = response.contentType()
        return Image(response.body<ByteArray>(), contentType?.toString())
    }

    suspend fun uploadSeriesCover(seriesId: StumpSeriesId, cover: Image) {
        updatesRateLimiter.acquire()
        
        val base64Image = Base64.getEncoder().encodeToString(cover.bytes)
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UploadSeriesCover(${'$'}id: ID!, ${'$'}image: String!) {
                    uploadSeriesThumbnailBase64(id: ${'$'}id, image: ${'$'}image) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to seriesId.value,
                "image" to base64Image
            )
        )
    }

    suspend fun uploadMediaCover(mediaId: StumpMediaId, cover: Image) {
        updatesRateLimiter.acquire()
        
        val base64Image = Base64.getEncoder().encodeToString(cover.bytes)
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UploadMediaCover(${'$'}id: ID!, ${'$'}image: String!) {
                    uploadMediaThumbnailBase64(id: ${'$'}id, image: ${'$'}image) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to mediaId.value,
                "image" to base64Image
            )
        )
    }

    private suspend inline fun <reified T> executeGraphQLQuery(
        query: String,
        variables: Map<String, Any?> = emptyMap()
    ): T {
        val request = StumpGraphQLRequest(
            query = query, 
            variables = variables.mapValues { toJsonElement(it.value) }
        )
        val response = ktor.post("api/graphql") {
            contentType(ContentType.Application.Json)
            authProvider.addAuth(this)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            throw StumpGraphQLException("GraphQL request failed with status ${response.status}")
        }

        val responseText = response.bodyAsText()
        val jsonResponse = json.parseToJsonElement(responseText).jsonObject
        
        val errors = jsonResponse["errors"]?.let { 
            json.decodeFromJsonElement<List<StumpGraphQLError>>(it) 
        }
        
        if (errors?.isNotEmpty() == true) {
            val errorMessages = errors.map { it.message }
            throw StumpGraphQLException("GraphQL errors: ${errorMessages.joinToString(", ")}", errorMessages)
        }
        
        val data = jsonResponse["data"] ?: throw StumpGraphQLException("GraphQL response data is null")
        return json.decodeFromJsonElement<T>(data)
    }

    private suspend inline fun <reified T> executeGraphQLMutation(
        mutation: String,
        variables: Map<String, Any?> = emptyMap()
    ): T {
        return executeGraphQLQuery(mutation, variables)
    }

    private fun toJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is JsonElement -> value
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJsonElement(v) })
            is List<*> -> JsonArray(value.map { toJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }
}

/**
 * An interface that all auth providers will implement to inject authentication into requests.
 * 
 * Note: Stump supports multiple authentication methods, but I've only implemented an API key
 * provider which implements this interface. I likely won't add others, but have it as an interface
 * just in case
 */
interface StumpAuthProvider {
    suspend fun addAuth(requestBuilder: HttpRequestBuilder)
}

@Serializable
private data class GetLibrariesResponse(val libraries: StumpPagedResponse<StumpLibrary>)

@Serializable
private data class GetLibraryResponse(val libraryById: StumpLibrary?)

@Serializable
private data class GetSeriesResponse(val series: StumpPagedResponse<StumpSeries>)

@Serializable
private data class GetSeriesDetailResponse(val seriesById: StumpSeries?)

@Serializable
private data class GetMediaResponse(val media: StumpPagedResponse<StumpMedia>)

@Serializable
private data class GetAllMediaResponse(val media: StumpNodesResponse<StumpMedia>)

@Serializable
private data class GetMediaDetailResponse(val mediaById: StumpMedia?)

@Serializable
private data class StumpPagedResponse<T>(
    val nodes: List<T>,
    val pageInfo: StumpPaginationInfo?
)

@Serializable
private data class StumpNodesResponse<T>(
    val nodes: List<T>
)

@Serializable
private data class ScanSeriesResponse(val scanSeries: Boolean)

@Serializable
private data class GetMediaWithSeriesResponse(val mediaById: StumpMediaWithSeries?)