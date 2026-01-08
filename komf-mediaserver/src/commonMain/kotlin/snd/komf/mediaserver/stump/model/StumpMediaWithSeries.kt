package snd.komf.mediaserver.stump.model

import kotlinx.serialization.Serializable

@Serializable
data class StumpMediaWithSeries(
    val id: StumpMediaId,
    val name: String,
    val path: String,
    val size: Long,
    val extension: String,
    val pages: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val seriesId: StumpSeriesId,
    val libraryId: StumpLibraryId? = null,
    val hash: String? = null,
    val seriesPosition: Int,
    val metadata: StumpMediaMetadata? = null,
    val series: StumpMediaSeriesSelection
)

@Serializable
data class StumpMediaSeriesSelection(
    val id: StumpSeriesId,
    val name: String,
    val resolvedName: String,
    val metadata: StumpSeriesMetadataBasic? = null
)

@Serializable
data class StumpSeriesMetadataBasic(
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