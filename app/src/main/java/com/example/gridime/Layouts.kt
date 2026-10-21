package com.example.gridime

enum class KeyType { CHAR, SHIFT, LAYER, SPACE, ENTER, DELETE, TOOL, CLIP, EMPTY, SUGGESTION }
enum class ShiftState { OFF, ONCE, LOCK }
enum class Tool { SELECT, SELECT_ALL, CUT, COPY, PASTE, HISTORY, SETTINGS }
enum class Layer { LETTERS, SYMBOLS, CLIPS }

/**
 * One key on the grid.
 * weight = width in units (a full row is 10 units)
 * hint   = the controller badge shown in the key's corner
 * index  = which clipboard item a clip card holds, or which suggestion a suggestion key is
 */
data class Key(
    val type: KeyType,
    val label: String = "",
    val weight: Float = 1f,
    val tool: Tool? = null,
    val hint: ControllerButton? = null,
    val index: Int = -1,
    val focusable: Boolean = true       // false for a spacer the highlight should skip
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
    const val DOTCOM_UNITS = 1.5f     // width of the ".com" key (clearly narrower than the space bar)
    const val MAX_CLIP_CARDS = 6      // 3 rows of 2; the 4th row holds the button guide

    private fun chars(s: String) = s.map { Key(KeyType.CHAR, it.toString()) }

    /**
     * The strip above the keys.
     *
     * [selectionTools]: Cut and Copy only show while text is selected, they have nothing to
     * work on otherwise. When they are hidden, their space (and the space Select and Paste
     * no longer need) goes to [suggestions], the word suggestions for what is being typed.
     * The strip is always exactly [UNITS] wide, so the keys never jump around.
     */
    fun toolRow(
        selectAllButton: Boolean,
        selectionTools: Boolean = false,
        suggestions: List<String> = emptyList()
    ): List<Key> {
        val select = Key(KeyType.TOOL, "Select", if (selectAllButton) 0.85f else 0.95f, Tool.SELECT, ControllerButton.SELECT)
        val all = Key(KeyType.TOOL, "All", 1.3f, Tool.SELECT_ALL)
        val paste = Key(KeyType.TOOL, "Paste", if (selectAllButton) 1.5f else 1.9f, Tool.PASTE)
        val history = Key(KeyType.TOOL, "", 0.9f, Tool.HISTORY)
        val settings = Key(KeyType.TOOL, "", 0.9f, Tool.SETTINGS)

        // Keys that are always there, before and after the flexible middle
        val head = if (selectAllButton) listOf(select, all) else listOf(select)
        val tail = listOf(paste, history, settings)
        val spare = UNITS - (head + tail).sumOf { it.weight.toDouble() }.toFloat()

        val middle = if (selectionTools) {
            // Cut and Copy share the spare room, and sit between Select and Paste
            listOf(
                Key(KeyType.TOOL, "Cut", spare / 2f, Tool.CUT),
                Key(KeyType.TOOL, "Copy", spare / 2f, Tool.COPY)
            )
        } else if (suggestions.isEmpty()) {
            listOf(Key(KeyType.EMPTY, "", spare, focusable = false))      // keeps the strip full width
        } else {
            val each = spare / suggestions.size
            suggestions.mapIndexed { i, word -> Key(KeyType.SUGGESTION, word, each, index = i) }
        }
        return head + middle + tail
    }

    /** The space bar gives up [DOTCOM_UNITS] of its width to a ".com" key between "." and Enter. */
    fun letterRows(layout: KeyLayout): List<List<Key>> = when (layout) {
        KeyLayout.GRID -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl'"),
            listOf(shift(1f)) + chars("zxcvbnm") + delete(2f),
            bottomRow("?123", space = 4f, side = 2f)
        )
        KeyLayout.CLASSIC -> listOf(
            chars("qwertyuiop"),
            chars("asdfghjkl"),
            listOf(shift(1.5f)) + chars("zxcvbnm") + delete(1.5f),
            bottomRow("?123", space = 5f, side = 1.5f)
        )
    }

    fun symbolRows(layout: KeyLayout): List<List<Key>> {
        val space = if (layout == KeyLayout.GRID) 4f else 5f
        val side = if (layout == KeyLayout.GRID) 2f else 1.5f
        return listOf(
            chars("1234567890"),
            chars("@#\$%&-+()/"),
            chars("*\"':;!?=") + delete(2f),
            bottomRow("ABC", space, side)
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

    private fun shift(weight: Float) = Key(KeyType.SHIFT, weight = weight, hint = ControllerButton.L2)
    private fun delete(weight: Float) = Key(KeyType.DELETE, weight = weight, hint = ControllerButton.X)

    private fun bottomRow(layerLabel: String, space: Float, side: Float) = listOf(
        Key(KeyType.LAYER, layerLabel, side, hint = ControllerButton.L1),
        Key(KeyType.CHAR, ","),
        Key(KeyType.SPACE, "", space - DOTCOM_UNITS, hint = ControllerButton.Y),
        Key(KeyType.CHAR, "."),
        Key(KeyType.CHAR, ".com", DOTCOM_UNITS, hint = ControllerButton.START),
        Key(KeyType.ENTER, "", side, hint = ControllerButton.R2)
    )
}
