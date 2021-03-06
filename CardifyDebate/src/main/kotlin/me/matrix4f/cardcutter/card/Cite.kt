package me.matrix4f.cardcutter.card

import me.matrix4f.cardcutter.prefs.Prefs
import me.matrix4f.cardcutter.util.currentDate

data class Cite(val authors: Array<Author>,
                val date: Timestamp,
                val title: String,
                val publication: String,
                val url: String) {

    fun getAuthorName(useShortName: Boolean): String {
        if (authors.size > 1) {
            if (Prefs.get().useEtAl && useShortName) {
                return "${authors[0].toString(useShortName)} et al."
            }

            // Multiple authors - create a list (e.g. "Brooks, Wolfsworth, and Ikenberry")

            val builder = StringBuilder(authors[0].toString(useShortName))
            for (i in 1 until (authors.size - 1))
                builder.append(", ").append(authors[i].toString(useShortName))
            if (authors.size > 2)
                builder.append(',')
            builder.append(" and ").append(authors.last().toString(useShortName))

            return builder.toString()
        } else if (authors.size == 1) {
            return authors[0].toString(useShortName)
        } else {
            return "No Author"
        }
    }

    fun getAuthorQualifications(): String {
        val sb = StringBuilder()

        for (author in authors)
            if (author.qualifications.get().isNotEmpty())
                sb.append(author.qualifications.get()).append(", ")
        return sb.toString()
    }
}