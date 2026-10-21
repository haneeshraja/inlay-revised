package com.example.gridime

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.Locale

/**
 * Finds words that start with what has been typed so far, the most common ones first.
 *
 * The words come from a plain list in res/raw (see licenses/WORD_LIST.md), already in order
 * of how common they are, so the first matches are the best ones. It loads in the
 * background; until it is ready there are simply no suggestions. [onReady] runs on the
 * main thread once it is.
 */
class WordSuggester(context: Context, onReady: () -> Unit) {

    @Volatile
    private var words: Array<String> = emptyArray()

    init {
        val resources = context.applicationContext.resources
        Thread {
            val loaded = runCatching {
                resources.openRawResource(R.raw.words_en).bufferedReader().useLines { lines ->
                    lines.map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("#") }
                        .toList()
                        .toTypedArray()
                }
            }.getOrDefault(emptyArray())
            words = loaded
            Handler(Looper.getMainLooper()).post(onReady)
        }.apply {
            isDaemon = true
            start()
        }
    }

    /** Up to [limit] words that continue [typed], written in the same style (Capital, CAPS or lower case). */
    fun suggest(typed: String, limit: Int): List<String> {
        val list = words
        if (list.isEmpty() || typed.isEmpty()) return emptyList()
        val prefix = typed.replace('’', '\'').lowercase(Locale.ROOT)
        val found = ArrayList<String>(limit)
        for (word in list) {
            if (word.length > prefix.length && word.startsWith(prefix)) {
                found += matchCase(word, typed)
                if (found.size == limit) break
            }
        }
        return found
    }

    private fun matchCase(word: String, typed: String): String = when {
        // Two or more capital letters and no small ones: someone typing in capitals
        typed.count { it.isLetter() } > 1 && typed.none { it.isLowerCase() } -> word.uppercase(Locale.ROOT)
        typed.first().isUpperCase() -> word.replaceFirstChar { it.uppercase() }
        // "I" is always a capital, also in I'm, I'll ...
        word.startsWith("i'") -> "I" + word.drop(1)
        else -> word
    }
}
