package snd.komf.mediaserver.stump.model.request

import kotlinx.serialization.Serializable

@Serializable
data class StumpMediaMetadataInput(
    val title: String? = null,
    val titleSort: String? = null,
    val series: String? = null,
    val number: Double? = null,
    val volume: Int? = null,
    val summary: String? = null,
    val notes: String? = null,
    val genres: List<String>? = null,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val writers: List<String>? = null,
    val pencillers: List<String>? = null,
    val inkers: List<String>? = null,
    val colorists: List<String>? = null,
    val letterers: List<String>? = null,
    val coverArtists: List<String>? = null,
    val editors: List<String>? = null,
    val publisher: String? = null,
    val links: List<String>? = null,
    val characters: List<String>? = null,
    val teams: List<String>? = null,
    val pageCount: Int? = null,
    val ageRating: Int? = null,
    val language: String? = null,
    val identifierIsbn: String? = null,
)