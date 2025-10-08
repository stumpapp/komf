package snd.komf.mediaserver.stump.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerLibraryId

@JvmInline
@Serializable
value class StumpLibraryId(val value: String) {
    override fun toString() = value
}

fun MediaServerLibraryId.toStumpLibraryId() = StumpLibraryId(value)

@Serializable
data class StumpLibrary(
    val id: StumpLibraryId,
    val name: String,
    val description: String? = null,
    val path: String,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val tags: List<StumpTag> = emptyList(),
    val config: StumpLibraryConfig? = null,
)

@Serializable
data class StumpLibraryConfig(
    val convertRarToZip: Boolean = false,
    val hardDeleteFile: Boolean = false,
    val createWebpThumbnails: Boolean = true,
    val processEpubMetadata: Boolean = true,
    val thumbnailConfig: StumpThumbnailConfig? = null,
)

@Serializable
data class StumpThumbnailConfig(
    val format: String = "WEBP",
    val quality: Int = 85,
)

@Serializable
data class StumpTag(
    val id: String,
    val name: String,
)