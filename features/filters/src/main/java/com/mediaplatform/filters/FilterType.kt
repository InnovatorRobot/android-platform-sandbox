package com.mediaplatform.filters

/**
 * All available image filters.
 *
 * [id] must match the C++ image_filter::FilterType enum values exactly.
 */
enum class FilterType(val id: Int, val displayName: String) {
    NONE       (0, "Original"),
    GRAYSCALE  (1, "Grayscale"),
    BLUR       (2, "Blur"),
    SHARPEN    (3, "Sharpen"),
    EDGE_DETECT(4, "Edges");

    companion object {
        fun fromId(id: Int): FilterType = values().firstOrNull { it.id == id } ?: NONE
    }
}
