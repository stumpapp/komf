package snd.komf.mediaserver.stump

import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.model.MediaServerAlternativeTitle
import snd.komf.mediaserver.model.MediaServerAuthor
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerBookMetadataUpdate
import snd.komf.mediaserver.model.MediaServerBookThumbnail
import snd.komf.mediaserver.model.MediaServerLibrary
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerSeriesMetadata
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate
import snd.komf.mediaserver.model.MediaServerSeriesThumbnail
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.model.Page
import snd.komf.mediaserver.stump.model.*
import snd.komf.mediaserver.stump.model.request.*
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ReadingDirection
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

class StumpMediaServerClientAdapter(
    private val stumpClient: StumpClient
) : MediaServerClient {

    override suspend fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
        val stumpSeriesId = seriesId.toStumpSeriesId()
        val series = stumpClient.getSeries(stumpSeriesId)
        return series.toMediaServerSeries()
    }

    override suspend fun getSeries(libraryId: MediaServerLibraryId, pageNumber: Int): Page<MediaServerSeries> {
        val stumpLibraryId = libraryId.toStumpLibraryId()
        val stumpPage = stumpClient.getSeries(stumpLibraryId, pageNumber)
        
        return Page(
            content = stumpPage.content.map { it.toMediaServerSeries() },
            pageNumber = stumpPage.currentPage,
            totalElements = stumpPage.totalElements,
            totalPages = stumpPage.totalPages
        )
    }

    override suspend fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image? {
        return runCatching { 
            stumpClient.getSeriesCover(seriesId.toStumpSeriesId()) 
        }.getOrNull()
    }

    // Note: Stump doesn't support multiple thumbnails so I just return the single one if it exists in a list
    override suspend fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
        return runCatching { 
            stumpClient.getSeriesCover(seriesId.toStumpSeriesId())
        }.getOrNull()?.let { _ ->
            listOf(
                MediaServerSeriesThumbnail(
                    id = MediaServerThumbnailId("default"),
                    seriesId = seriesId,
                    type = "generated",
                    selected = true
                )
            )
        } ?: emptyList()
    }

    override suspend fun getBook(bookId: MediaServerBookId): MediaServerBook {
        val stumpMediaId = StumpMediaId(bookId.value)
        val mediaWithSeries = stumpClient.getMediaWithSeries(stumpMediaId)
        return mediaWithSeries.toMediaServerBook()
    }

    override suspend fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
        val stumpSeriesId = seriesId.toStumpSeriesId()
        val allMedia = stumpClient.getAllMedia(stumpSeriesId)
        return allMedia.map { it.toMediaServerBook() }
    }

    // Note: Stump doesn't support multiple thumbnails so I just return the single one if it exists in a list
    override suspend fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
        return runCatching { 
            stumpClient.getMediaCover(StumpMediaId(bookId.value))
        }.getOrNull()?.let { _ ->
            listOf(
                MediaServerBookThumbnail(
                    id = MediaServerThumbnailId("default"),
                    bookId = bookId,
                    type = "generated",
                    selected = true
                )
            )
        } ?: emptyList()
    }

    override suspend fun getBookThumbnail(bookId: MediaServerBookId): Image? {
        return runCatching { 
            stumpClient.getMediaCover(StumpMediaId(bookId.value)) 
        }.getOrNull()
    }

    override suspend fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
        val stumpLibraryId = libraryId.toStumpLibraryId()
        val library = stumpClient.getLibrary(stumpLibraryId)
        return library.toMediaServerLibrary()
    }

    override suspend fun getLibraries(): List<MediaServerLibrary> {
        return stumpClient.getLibraries().map { it.toMediaServerLibrary() }
    }

    override suspend fun updateSeriesMetadata(
        seriesId: MediaServerSeriesId,
        metadata: MediaServerSeriesMetadataUpdate
    ) {
        val stumpSeriesId = seriesId.toStumpSeriesId()
        val metadataInput = metadata.toStumpSeriesMetadataInput()
        stumpClient.updateSeriesMetadata(stumpSeriesId, metadataInput)
    }

    override suspend fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
        // TODO(stump): Add `deleteSeriesThumbnail` mutation then implement this
    }

    override suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {
        val stumpMediaId = StumpMediaId(bookId.value)
        val metadataInput = metadata.toStumpMediaMetadataInput()
        stumpClient.updateMediaMetadata(stumpMediaId, metadataInput)
    }

    override suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {
        // TODO(stump): Add `deleteMediaThumbnail` mutation then implement this
    }

    override suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
        // TODO(stump): Add `resetMediaMetadata` mutation then implement this
    }

    override suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        // TODO(stump): Add `resetMediaMetadata` mutation then implement this
    }

    // Note: Stump doesn't return thumbnail metadata, so I return null after upload
    override suspend fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean,
        lock: Boolean
    ): MediaServerSeriesThumbnail? {
        stumpClient.uploadSeriesCover(seriesId.toStumpSeriesId(), thumbnail)
        return null
    }

    // Note: Stump doesn't return thumbnail metadata, so I return null after upload
    override suspend fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean,
        lock: Boolean
    ): MediaServerBookThumbnail? {
        stumpClient.uploadMediaCover(StumpMediaId(bookId.value), thumbnail) 
        return null
    }

    override suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        // val stumpLibraryId = libraryId.toStumpLibraryId()
        // val stumpSeriesId = seriesId.toStumpSeriesId()
        // TODO(stump): Determine what exactly this should do
    }
}

private fun MediaServerSeriesId.toStumpSeriesId() = StumpSeriesId(value)
private fun MediaServerLibraryId.toStumpLibraryId() = StumpLibraryId(value)

private fun StumpSeries.toMediaServerSeries(): MediaServerSeries {
    return MediaServerSeries(
        id = MediaServerSeriesId(id.value),
        libraryId = MediaServerLibraryId(libraryId.value),
        name = name,
        booksCount = mediaCount,
        metadata = metadata?.toMediaServerSeriesMetadata(this) ?: createDefaultSeriesMetadata(this),
        url = path,
        deleted = false,
    )
}

private fun StumpMedia.toMediaServerBook(): MediaServerBook {
    val filePath = Path.of(path)
    val fileName = filePath.fileName?.nameWithoutExtension ?: name
    val bookNumber = metadata?.number?.toInt() ?: seriesPosition

    return MediaServerBook(
        id = MediaServerBookId(id.value),
        seriesId = MediaServerSeriesId(seriesId.value),
        libraryId = libraryId?.let { MediaServerLibraryId(it.value) },
        seriesTitle = series?.resolvedName ?: "Unknown Series",
        name = fileName,
        url = path,
        number = bookNumber,
        oneshot = false, // TODO(stump): Determine if this is a oneshot
        metadata = metadata?.toMediaServerBookMetadata() ?: createDefaultBookMetadata(this),
        deleted = false,
    )
}

private fun StumpMediaWithSeries.toMediaServerBook(): MediaServerBook {
    val filePath = Path.of(path)
    val fileName = filePath.fileName?.nameWithoutExtension ?: name
    val bookNumber = metadata?.number?.toInt() ?: seriesPosition

    return MediaServerBook(
        id = MediaServerBookId(id.value),
        seriesId = MediaServerSeriesId(seriesId.value),
        libraryId = libraryId?.let { MediaServerLibraryId(it.value) },
        seriesTitle = series.resolvedName,
        name = fileName,
        url = path,
        number = bookNumber,
        oneshot = false, // TODO(stump): Determine if this is a oneshot
        metadata = metadata?.toMediaServerBookMetadata() ?: createDefaultBookMetadata(this.toStumpMedia()),
        deleted = false,
    )
}

private fun StumpMediaWithSeries.toStumpMedia(): StumpMedia {
    return StumpMedia(
        id = id,
        name = name,
        description = description,
        path = path,
        size = size,
        extension = extension,
        pages = pages,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        seriesId = seriesId,
        libraryId = libraryId,
        hash = hash,
        seriesPosition = seriesPosition,
        metadata = metadata,
        series = series
    )
}

private fun StumpLibrary.toMediaServerLibrary() = MediaServerLibrary(
    id = MediaServerLibraryId(id.value),
    name = name,
    roots = listOf(path) // Stump libraries have a single root path
)

private fun StumpSeriesMetadata.toMediaServerSeriesMetadata(series: StumpSeries): MediaServerSeriesMetadata {
    val statusEnum = when (status?.lowercase()) {
        "ongoing" -> SeriesStatus.ONGOING
        "completed" -> SeriesStatus.COMPLETED
        "cancelled", "abandoned" -> SeriesStatus.ABANDONED
        "hiatus" -> SeriesStatus.HIATUS
        "ended" -> SeriesStatus.ENDED
        else -> SeriesStatus.ONGOING
    }

    return MediaServerSeriesMetadata(
        status = statusEnum,
        title = title ?: series.name,
        titleSort = series.name, // Stump doesn't have explicit title sort
        alternativeTitles = emptyList(),
        summary = summary ?: "",
        readingDirection = null,
        publisher = publisher,
        alternativePublishers = emptySet(),
        ageRating = ageRating,
        language = null,
        genres = genres,
        tags = emptyList(),
        totalBookCount = null,
        authors = writers.map { MediaServerAuthor(it, "WRITER") },
        releaseYear = null,
        links = links.map { WebLink(it, it) },

        // Lock fields - Stump doesn't have explicit lock support yet
        statusLock = false,
        titleLock = false,
        titleSortLock = false,
        summaryLock = false,
        readingDirectionLock = false,
        publisherLock = false,
        ageRatingLock = false,
        languageLock = false,
        genresLock = false,
        tagsLock = false,
        totalBookCountLock = false,
        authorsLock = false,
        releaseYearLock = false,
        alternativeTitlesLock = false,
        linksLock = false,
    )
}

private fun StumpMediaMetadata.toMediaServerBookMetadata(): MediaServerBookMetadata {
    val allAuthors = mutableListOf<MediaServerAuthor>()
    writers.forEach { allAuthors.add(MediaServerAuthor(it, "WRITER")) }
    pencillers.forEach { allAuthors.add(MediaServerAuthor(it, "PENCILLER")) }
    inkers.forEach { allAuthors.add(MediaServerAuthor(it, "INKER")) }
    colorists.forEach { allAuthors.add(MediaServerAuthor(it, "COLORIST")) }
    letterers.forEach { allAuthors.add(MediaServerAuthor(it, "LETTERER")) }
    editors.forEach { allAuthors.add(MediaServerAuthor(it, "EDITOR")) }

    return MediaServerBookMetadata(
        title = title ?: "",
        summary = summary,
        number = number?.toString() ?: "0",
        numberSort = number?.toString(),
        releaseDate = createReleaseDate(year, month, day),
        authors = allAuthors,
        tags = genres,
        isbn = null, // TODO(stump): Add ISBN support when available in schema
        links = links.map { WebLink(it, it) },

        // Lock fields - Stump doesn't have explicit lock support yet
        titleLock = false,
        summaryLock = false,
        numberLock = false,
        numberSortLock = false,
        releaseDateLock = false,
        authorsLock = false,
        tagsLock = false,
        isbnLock = false,
        linksLock = false,
    )
}

private fun MediaServerSeriesMetadataUpdate.toStumpSeriesMetadataInput(): StumpSeriesMetadataInput {
    val statusString = status?.let { status ->
        when (status) {
            SeriesStatus.ONGOING -> "ongoing"
            SeriesStatus.COMPLETED -> "completed"
            SeriesStatus.ABANDONED -> "cancelled"
            SeriesStatus.HIATUS -> "hiatus"
            SeriesStatus.ENDED -> "ended"
        }
    }

    return StumpSeriesMetadataInput(
        title = title?.name,
        summary = summary,
        publisher = publisher,
        status = statusString,
        ageRating = ageRating,
        genres = genres?.toList() ?: emptyList(),
        writers = authors?.filter { it.role == "WRITER" }?.map { it.name } ?: emptyList()
    )
}

private fun MediaServerBookMetadataUpdate.toStumpMediaMetadataInput(): StumpMediaMetadataInput {
    val releaseYear = releaseDate?.year
    val releaseMonth = releaseDate?.month?.ordinal?.plus(1)  
    val releaseDay = releaseDate?.day

    return StumpMediaMetadataInput(
        title = title,
        summary = summary,
        number = number?.toDoubleOrNull(),
        year = releaseYear,
        month = releaseMonth,
        day = releaseDay,
        colorists = authors?.filter { it.role == "COLORIST" }?.map { it.name } ?: emptyList(),
        editors = authors?.filter { it.role == "EDITOR" }?.map { it.name } ?: emptyList(),
        genres = tags?.toList() ?: emptyList(),
        inkers = authors?.filter { it.role == "INKER" }?.map { it.name } ?: emptyList(),
        letterers = authors?.filter { it.role == "LETTERER" }?.map { it.name } ?: emptyList(),
        pencillers = authors?.filter { it.role == "PENCILLER" }?.map { it.name } ?: emptyList(),
        writers = authors?.filter { it.role == "WRITER" }?.map { it.name } ?: emptyList()
    )
}

// Helper functions
private fun createDefaultSeriesMetadata(series: StumpSeries): MediaServerSeriesMetadata {
    return MediaServerSeriesMetadata(
        status = SeriesStatus.ONGOING,
        title = series.name,
        titleSort = series.name,
        alternativeTitles = emptyList(),
        summary = series.description ?: "",
        readingDirection = null,
        publisher = null,
        alternativePublishers = emptySet(),
        ageRating = null,
        language = null,
        genres = emptyList(),
        tags = emptyList(),
        totalBookCount = series.mediaCount.takeIf { it > 0 },
        authors = emptyList(),
        releaseYear = null,
        links = emptyList(),

        statusLock = false,
        titleLock = false,
        titleSortLock = false,
        summaryLock = false,
        readingDirectionLock = false,
        publisherLock = false,
        ageRatingLock = false,
        languageLock = false,
        genresLock = false,
        tagsLock = false,
        totalBookCountLock = false,
        authorsLock = false,
        releaseYearLock = false,
        alternativeTitlesLock = false,
        linksLock = false,
    )
}

private fun createDefaultBookMetadata(media: StumpMedia): MediaServerBookMetadata {
    return MediaServerBookMetadata(
        title = media.name,
        summary = media.description,
        number = media.seriesPosition.toString(),
        numberSort = media.seriesPosition.toString(),
        releaseDate = null,
        authors = emptyList(),
        tags = emptyList(),
        isbn = null,
        links = emptyList(),

        titleLock = false,
        summaryLock = false,
        numberLock = false,
        numberSortLock = false,
        releaseDateLock = false,
        authorsLock = false,
        tagsLock = false,
        isbnLock = false,
        linksLock = false,
    )
}

private fun parseAgeRating(ageRating: String?): Int? {
    return ageRating?.toIntOrNull()
}

private fun createReleaseDate(year: Int?, month: Int?, day: Int?): kotlinx.datetime.LocalDate? {
    return if (year != null) {
        try {
            kotlinx.datetime.LocalDate(year, month ?: 1, day ?: 1)
        } catch (e: Exception) {
            null
        }
    } else null
}



private fun parseReadingDirection(direction: String?): ReadingDirection? {
    return when (direction?.lowercase()) {
        "left_to_right", "ltr", "left-to-right" -> ReadingDirection.LEFT_TO_RIGHT
        "right_to_left", "rtl", "right-to-left" -> ReadingDirection.RIGHT_TO_LEFT
        "vertical" -> ReadingDirection.VERTICAL
        "webtoon" -> ReadingDirection.WEBTOON
        else -> null
    }
}