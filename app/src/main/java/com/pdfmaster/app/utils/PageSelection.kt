package com.pdfmaster.app.utils

/**
 * Parse user input like "1,3,5-7" into list of page numbers (1-based).
 * Returns null if invalid.
 */
object PageSelection {

    fun parse(input: String, maxPage: Int): List<Int>? {
        if (input.isBlank()) return null
        val result = mutableListOf<Int>()
        try {
            input.split(",").forEach { part ->
                val p = part.trim()
                if (p.contains("-")) {
                    val (a, b) = p.split("-").map { it.trim().toInt() }
                    if (a > b || a < 1 || b > maxPage) return null
                    for (i in a..b) result.add(i)
                } else {
                    val n = p.toInt()
                    if (n < 1 || n > maxPage) return null
                    result.add(n)
                }
            }
        } catch (_: Exception) {
            return null
        }
        return result.distinct().sorted()
    }
}
