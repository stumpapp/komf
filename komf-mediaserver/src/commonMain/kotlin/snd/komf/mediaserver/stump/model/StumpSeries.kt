package snd.komf.mediaserver.stump.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerSeriesId

@JvmInline
@Serializable
value class StumpSeriesId(val value: String) {
    override fun toString() = value
}

fun MediaServerSeriesId.toStumpSeriesId() = StumpSeriesId(value)

@Serializable
data class StumpSeries(
    val id: StumpSeriesId,
    val name: String,
    val description: String? = null,
    val path: String,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val libraryId: StumpLibraryId,
    val mediaCount: Int = 0,
    val tags: List<StumpTag> = emptyList(),
    val metadata: StumpSeriesMetadata? = null,
)

@Serializable
data class StumpSeriesMetadata(
    val seriesId: String,
    val ageRating: Int? = null,
    val booktype: String? = null,
    val comicid: Int? = null,
    val imprint: String? = null,
    val metaType: String? = null,
    val publisher: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val title: String? = null,
    val volume: Int? = null,
    val characters: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
)

