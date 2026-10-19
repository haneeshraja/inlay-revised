package com.example.gridime

enum class KeyType { CHAR, SHIFT, LAYER, SPACE, ENTER, DELETE, TOOL, CLIP, EMPTY }
enum class ShiftState { OFF, ONCE, LOCK }
enum class Tool { SELECT, SELECT_ALL, CUT, COPY, PASTE, HISTORY, SETTINGS }
enum class Layer { LETTERS, SYMBOLS, CLIPS }

/**
 * One key on the grid.
 * weight = width in units (a full row is 10 units)
 * hint   = the controller badge shown in the key's corner
 * index  = which clipboard item a clip card holds
 */
data class Key(
    val type: KeyType,
    val label: String = "",
    val weight: Float = 1f,
    val tool: Tool? = null,
    val hint: ControllerButton? = null,
    val index: Int = -1
)

/**
 * All keyboard layouts. Change a row here to change the keyboard.
 *
 * The GRID layout lines every key up in 10 columns, the way Microsoft's
 * controller keyboard does, so Up and Down always land on the key directly above
 * or below. CLASSIC keeps the familiar phone stagger.
 */
object Layouts {
    const val UNITS = 10f
    const val URL_KEY_UNITS = 2f      // width of the ".com" key in browser address bars
    const val MAX_CLIP_CARDS = 6      // 3 rows of 2; the 4th row holds the button guide

    private fun chars(s: String) = s.map { Key(KeyType.CHAR, it.toString()) }

    fun toolRow(selectAllButton: Boolean): List<Key> {
        val select = Key(KeyType.TOOL, "Select", if (selectAllButton) 1.7f else 1.9f, Tool.SELECT, ControllerButton.SELECT)
        val tail = listOf(
            Key(KeyType.TOOL, "", 0.9f, Tool.HISTORY),
            Key(KeyType.TOOL, "", 0.9f, Tool.SETTINGS)
        )
        return if (selectAllButton) {
            listOf(
                select,
                Key(KeyType.TOOL, "All", 1.3f, Tool.SELECT_ALL),
                Key(KeyType.TOOL, "Cut", 1.0f, Tool.CUT),
                Key(KeyType.TOOL, "Copy", 1.2f, Tool.COPY),
                Key(KeyType.TOOL, "Paste", 3.0f, Tool.PASTE)
            ) + tail
        } else {
            listOf(
                select,
                Key(KeyType.TOOL, "Cut", 1.2f, Tool.CUT),
                Key(KeyType.TOOL, "Copy", 1.3f, Tool.COPY),
                Key(KeyType.TOOL, "Paste", 3.8f, Tool.PASTE)
            ) + tail
        }
    }

    /** In a browser address bar the space bar gives up [URL_KEY_UNITS] of its width to a ".com" key. */
    fun letterRows(layout: KeyLayout, urlMode: Boolean = false): List<List<Key>> = when (layout) {
        KeyLayout.GRID -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl'"),
            listOf(shift(1f)) + chars("zxcvbnm") + delete(2f),
            bottomRow("?123", space = 4f, side = 2f, urlMode = urlMode)
        )
        KeyLayout.CLASSIC -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl"),
            listOf(shift(1.5f)) + chars("zxcvbnm") + delete(1.5f),
            bottomRow("?123", space = 5f, side = 1.5f, urlMode = urlMode)
        )
    }

    fun symbolRows(layout: KeyLayout, urlMode: Boolean = false): List<List<Key>> {
        val space = if (layout == KeyLayout.GRID) 4f else 5f
        val side = if (layout == KeyLayout.GRID) 2f else 1.5f
        return listOf(
            chars("1234567890"),
            chars("@#\$%&-+()/"),
            chars("*\"':;!?=") + delete(2f),
            bottomRow("ABC", space, side, urlMode)
        )
    }

    fun clipRows(clips: List<String>): List<List<Key>> {
        if (clips.isEmpty()) {
            return listOf(listOf(Key(KeyType.EMPTY, "Copied text will show up here", UNITS)))
        }
        return clips.take(MAX_CLIP_CARDS).chunked(2).mapIndexed { r, pair ->
            pair.mapIndexed { c, text -> Key(KeyType.CLIP, text, UNITS / 2, index = r * 2 + c) }
        }
    }

    private fun shift(weight: Float) = Key(KeyType.SHIFT, weight = weight, hint = ControllerButton.L1)
    private fun delete(weight: Float) = Key(KeyType.DELETE, weight = weight, hint = ControllerButton.X)

    private fun bottomRow(layerLabel: String, space: Float, side: Float, urlMode: Boolean = false) = listOf(
        Key(KeyType.LAYER, layerLabel, side, hint = ControllerButton.L2),
        Key(KeyType.CHAR, ","),
        Key(KeyType.SPACE, "", if (urlMode) space - URL_KEY_UNITS else space, hint = ControllerButton.Y)
    ) + (if (urlMode) listOf(Key(KeyType.CHAR, ".com", URL_KEY_UNITS)) else emptyList()) + listOf(
        Key(KeyType.CHAR, "."),
        Key(KeyType.ENTER, "", side, hint = ControllerButton.R2)
    )
}
