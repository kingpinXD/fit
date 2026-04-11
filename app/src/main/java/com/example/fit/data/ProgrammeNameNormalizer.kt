package com.example.fit.data

object ProgrammeNameNormalizer {

    private val NOISE_WORDS = setOf("the", "edited", "spreadsheet", "sheet", "program")

    /**
     * Normalizes a filename into a clean programme name.
     *
     * Steps:
     * 1. Strip file extension (.xlsx, .json, etc.)
     * 2. Replace underscores with spaces
     * 3. Lowercase everything
     * 4. Remove noise words: "edited", "spreadsheet", "sheet", "program"
     * 5. Replace hyphens surrounded by spaces (" - ") with single space
     * 6. Collapse multiple spaces into one
     * 7. Trim leading/trailing spaces and hyphens
     * 8. Replace remaining spaces with underscores
     * 9. Collapse multiple underscores
     * 10. If result length <= 4, prepend normalized parentFolder name
     */
    fun normalize(filename: String, parentFolder: String = ""): String {
        var name = stripExtension(filename)
        name = name.replace('_', ' ')
        name = name.lowercase()
        name = removeNoiseWords(name)
        name = name.replace(" - ", " ")
        name = name.replace(Regex(" {2,}"), " ")
        name = name.trim(' ', '-')
        name = name.replace(' ', '_')
        name = name.replace(Regex("_{2,}"), "_")

        if (name.length <= 4 && parentFolder.isNotEmpty()) {
            val prefix = cleanFolder(parentFolder)
            name = if (name.isEmpty()) prefix else "${prefix}_${name}"
        }

        return name
    }

    private fun stripExtension(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot > 0) filename.substring(0, dot) else filename
    }

    private fun removeNoiseWords(text: String): String {
        var result = text
        for (word in NOISE_WORDS) {
            // Remove noise word as standalone token (bounded by spaces, hyphens, or string edges)
            result = result.replace(Regex("(?<=^|[\\s-])$word(?=$|[\\s-])", RegexOption.IGNORE_CASE), "")
        }
        // Clean up leftover dangling hyphens (e.g., "powerbuilding-6x-" after removing trailing noise)
        result = result.replace(Regex("-+$"), "")
        result = result.replace(Regex("^-+"), "")
        return result
    }

    /** Normalize a parent folder name using the same cleanup, without the < 4 char fallback. */
    private fun cleanFolder(folder: String): String {
        var name = folder.replace('_', ' ')
        name = name.lowercase()
        name = removeNoiseWords(name)
        name = name.replace(" - ", " ")
        name = name.replace(Regex(" {2,}"), " ")
        name = name.trim(' ', '-')
        name = name.replace(' ', '_')
        name = name.replace(Regex("_{2,}"), "_")
        return name
    }
}
