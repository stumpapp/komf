package snd.komf.mediaserver.stump.model.request

import kotlinx.serialization.Serializable

@Serializable
data class StumpSeriesMetadataInput(
    val ageRating: Int? = null,
    val booktype: String? = null,
    val characters: List<String>? = null,
    val comicid: Int? = null,
    val genres: List<String>? = null,
    val imprint: String? = null,
    val links: List<String>? = null,
    val metaType: String? = null,
    val publisher: String? = null,
    val status: String? = null,
    val summary: String? = null,
    val title: String? = null,
    val volume: Int? = null,
    val writers: List<String>? = null,
)