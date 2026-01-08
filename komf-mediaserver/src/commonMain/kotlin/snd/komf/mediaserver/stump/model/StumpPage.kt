package snd.komf.mediaserver.stump.model

import kotlinx.serialization.Serializable

@Serializable
data class StumpPage<T>(
    val content: List<T>,
    val totalPages: Int? = null,
    val totalElements: Int? = null,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)

@Serializable
data class StumpPaginationInfo(
    val totalPages: Int,
    val totalItems: Int,
    val currentPage: Int,
    val pageSize: Int,
    val pageOffset: Int,
    val zeroBased: Boolean,
)