package snd.komf.mediaserver.stump.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class StumpMediaId(val value: String) {
    override fun toString() = value
}

@Serializable
data class StumpMedia(
    val id: StumpMediaId,
    val name: String,
    val description: String? = null,
    val path: String,
    val size: Long,
    val extension: String,
    val pages: Int,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val seriesId: StumpSeriesId,
    val libraryId: StumpLibraryId? = null,
    val hash: String? = null,
    val seriesPosition: Int,
    val metadata: StumpMediaMetadata? = null,
    val series: StumpMediaSeriesSelection? = null,
)

@Serializable
data class StumpMediaMetadata(
    val id: Int,
    val mediaId: String? = null,
    val ageRating: Int? = null,
    val day: Int? = null,
    val language: String? = null,
    val month: Int? = null,
    val notes: String? = null,
    val number: Double? = null,
    val pageCount: Int? = null,
    val publisher: String? = null,
    val series: String? = null,
    val summary: String? = null,
    val title: String? = null,
    val titleSort: String? = null,
    val volume: Int? = null,
    val year: Int? = null,
    val writers: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val colorists: List<String> = emptyList(),
    val coverArtists: List<String> = emptyList(),
    val editors: List<String> = emptyList(),
    val inkers: List<String> = emptyList(),
    val letterers: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val pencillers: List<String> = emptyList(),
    val teams: List<String> = emptyList(),
)