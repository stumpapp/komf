package snd.komf.mediaserver.stump.model

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
    val createdAt: String,
    val updatedAt: String? = null,
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
    val comicImage: String? = null,
    val descriptionFormatted: String? = null,
    val imprint: String? = null,
    val metaType: String? = null,
    val publicationRun: String? = null,
    val publisher: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val title: String? = null,
    val totalIssues: Int? = null,
    val volume: Int? = null,
    val year: Int? = null,
    val characters: List<String> = emptyList(),
    val collects: List<StumpCollectedItem> = emptyList(),
    val genres: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
)

@Serializable
data class StumpCollectedItem(
    val series: String? = null,
    val comicid: String? = null,
    val issueid: String? = null,
    val issues: String? = null,
)

