package snd.komf.mediaserver.stump

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
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
                        data {
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
        return response.libraries.data
    }

    suspend fun getLibrary(libraryId: StumpLibraryId): StumpLibrary {
        return executeGraphQLQuery<GetLibraryResponse>(
            query = """
                query GetLibrary(${'$'}id: ID!) {
                    library(id: ${'$'}id) {
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
        ).library
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
                query GetSeries(${'$'}libraryId: ID!, ${'$'}pagination: Pagination!) {
                    series(libraryId: ${'$'}libraryId, pagination: ${'$'}pagination) {
                        data {
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
                                imprint
                                metaType
                                publisher
                                status
                                summary
                                title
                                volume
                                characters
                                genres
                                links
                                writers
                            }
                        }
                        pageInfo {
                            ... on OffsetPaginationInfo {
                                totalPages
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
                "libraryId" to libraryId.value,
                "pagination" to mapOf(
                    "offset" to mapOf(
                        "page" to page,
                        "pageSize" to pageSize
                    )
                )
            )
        )
        
        return StumpPage(
            content = response.series.data,
            totalPages = response.series.pageInfo?.totalPages,
            totalElements = null, // TODO(stump): Not available in OffsetPaginationInfo
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
                    series(id: ${'$'}id) {
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
                            id
                            title
                            summary
                            publisher
                            totalIssues
                            publicationYear
                            language
                            status
                            genre
                            tags
                            ageRating
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to seriesId.value)
        ).series
    }

    suspend fun updateSeriesMetadata(seriesId: StumpSeriesId, metadata: StumpSeriesMetadataInput) {
        updatesRateLimiter.acquire()
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UpdateSeriesMetadata(${'$'}id: ID!, ${'$'}metadata: SeriesMetadataInput!) {
                    updateSeriesMetadata(id: ${'$'}id, metadata: ${'$'}metadata) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to seriesId.value,
                "metadata" to metadata
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
                query GetMedia(${'$'}seriesId: ID!, ${'$'}pagination: Pagination!) {
                    media(seriesId: ${'$'}seriesId, pagination: ${'$'}pagination) {
                        data {
                            id
                            name
                            description
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
                                seriesId
                                mediaId
                                ageRating
                                day
                                language
                                month
                                notes
                                number
                                pageCount
                                publisher
                                series
                                summary
                                title
                                titleSort
                                volume
                                year
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
                "seriesId" to seriesId.value,
                "pagination" to mapOf(
                    "offset" to mapOf(
                        "page" to page,
                        "pageSize" to pageSize
                    )
                )
            )
        )
        
        return StumpPage(
            content = response.media.data,
            totalPages = response.media.pageInfo?.totalPages,
            totalElements = null,  // TODO(stump): Not available in OffsetPaginationInfo
            currentPage = response.media.pageInfo?.currentPage ?: page,
            pageSize = response.media.pageInfo?.pageSize ?: pageSize,
            hasNext = response.media.pageInfo?.let { it.currentPage < it.totalPages } ?: false,
            hasPrevious = response.media.pageInfo?.let { it.currentPage > 0 } ?: false
        )
    }

    suspend fun getAllMedia(seriesId: StumpSeriesId): List<StumpMedia> {
        val response = executeGraphQLQuery<GetAllMediaResponse>(
            query = """
                query GetAllMedia(${'$'}seriesId: ID!, ${'$'}pagination: Pagination!) {
                    media(seriesId: ${'$'}seriesId, pagination: ${'$'}pagination) {
                        data {
                            id
                            name
                            description
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
                                seriesId
                                mediaId
                                ageRating
                                day
                                language
                                month
                                notes
                                number
                                pageCount
                                publisher
                                series
                                summary
                                title
                                titleSort
                                volume
                                year
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
                "seriesId" to seriesId.value,
                "pagination" to mapOf(
                    "none" to mapOf(
                        "unpaginated" to true
                    )
                )
            )
        )
        
        return response.media.data
    }

    suspend fun getMedia(mediaId: StumpMediaId): StumpMedia {
        return executeGraphQLQuery<GetMediaDetailResponse>(
            query = """
                query GetMediaDetail(${'$'}id: ID!) {
                    media(id: ${'$'}id) {
                        id
                        name
                        description
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
                            seriesId
                            mediaId
                            ageRating
                            day
                            language
                            month
                            notes
                            number
                            pageCount
                            publisher
                            series
                            summary
                            title
                            titleSort
                            volume
                            year
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
        ).media
    }

    suspend fun getMediaWithSeries(mediaId: StumpMediaId): StumpMediaWithSeries {
        return executeGraphQLQuery<GetMediaWithSeriesResponse>(
            query = """
                query GetMediaWithSeries(${'$'}id: ID!) {
                    media(id: ${'$'}id) {
                        id
                        name
                        description
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
                            seriesId
                            mediaId
                            ageRating
                            day
                            language
                            month
                            notes
                            number
                            pageCount
                            publisher
                            series
                            summary
                            title
                            titleSort
                            volume
                            year
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
                                imprint
                                metaType
                                publisher
                                status
                                summary
                                title
                                volume
                                characters
                                genres
                                links
                                writers
                            }
                        }
                    }
                }
            """.trimIndent(),
            variables = mapOf("id" to mediaId.value)
        ).media
    }

    suspend fun updateMediaMetadata(mediaId: StumpMediaId, metadata: StumpMediaMetadataInput) {
        updatesRateLimiter.acquire()
        
        executeGraphQLMutation<Any>(
            mutation = """
                mutation UpdateMediaMetadata(${'$'}id: ID!, ${'$'}metadata: MediaMetadataInput!) {
                    updateMediaMetadata(id: ${'$'}id, metadata: ${'$'}metadata) {
                        id
                    }
                }
            """.trimIndent(),
            variables = mapOf(
                "id" to mediaId.value,
                "metadata" to metadata
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
                    uploadSeriesCover(id: ${'$'}id, image: ${'$'}image) {
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
                    uploadMediaCover(id: ${'$'}id, image: ${'$'}image) {
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
            variables = variables.mapValues { it.value.toString() }
        )
        val response = ktor.post("api/graphql") {
            contentType(ContentType.Application.Json)
            authProvider.addAuth(this)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            throw StumpGraphQLException("GraphQL request failed with status ${response.status}")
        }

        val graphqlResponse = response.body<StumpGraphQLResponse<T>>()
        
        if (graphqlResponse.errors?.isNotEmpty() == true) {
            val errorMessages = graphqlResponse.errors.map { it.message }
            throw StumpGraphQLException("GraphQL errors: ${errorMessages.joinToString(", ")}", errorMessages)
        }
        
        return graphqlResponse.data ?: throw StumpGraphQLException("GraphQL response data is null")
    }

    private suspend inline fun <reified T> executeGraphQLMutation(
        mutation: String,
        variables: Map<String, Any?> = emptyMap()
    ): T {
        return executeGraphQLQuery(mutation, variables)
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
private data class GetLibraryResponse(val library: StumpLibrary)

@Serializable
private data class GetSeriesResponse(val series: StumpPagedResponse<StumpSeries>)

@Serializable
private data class GetSeriesDetailResponse(val series: StumpSeries)

@Serializable
private data class GetMediaResponse(val media: StumpPagedResponse<StumpMedia>)

@Serializable
private data class GetAllMediaResponse(val media: StumpUnpagedResponse<StumpMedia>)

@Serializable
private data class GetMediaDetailResponse(val media: StumpMedia)

@Serializable
private data class StumpPagedResponse<T>(
    val data: List<T>,
    val pageInfo: StumpPaginationInfo?
)

@Serializable
private data class StumpUnpagedResponse<T>(
    val data: List<T>
)

@Serializable
private data class ScanSeriesResponse(val scanSeries: Boolean)

@Serializable
private data class GetMediaWithSeriesResponse(val media: StumpMediaWithSeries)