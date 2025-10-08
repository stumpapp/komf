package snd.komf.mediaserver.stump.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class StumpMediaWithSeries(
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
    val title: String? = null
)