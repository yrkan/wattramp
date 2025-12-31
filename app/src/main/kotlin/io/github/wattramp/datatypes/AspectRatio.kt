package io.github.wattramp.datatypes

/**
 * Aspect ratio categories for layout orientation decisions.
 * Used to determine whether to arrange content horizontally or vertically.
 *
 * Common Karoo 3 configurations:
 * - 4 fields vertical: 470x195 → WIDE (ratio ~2.4)
 * - 5 fields vertical: 470x156 → WIDE (ratio ~3.0)
 * - 6 fields vertical: 470x130 → WIDE (ratio ~3.6)
 * - 2x2 grid cell:     235x390 → TALL (ratio ~0.6)
 * - Narrow strip:      235x195 → SQUARE (ratio ~1.2)
 */
enum class AspectRatio {
    /** width > 1.5 * height - prefer horizontal layout */
    WIDE,
    /** roughly equal width/height - flexible layout */
    SQUARE,
    /** height > 1.5 * width - prefer vertical layout */
    TALL
}
