package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.CommandType

// A set of commands for page rendering
data class PageVO(val commands: Array<CommandVO>) {
    // set of graphics commands for the page to be rendered
    val graphicElements = mutableListOf<GraphicsTag>()
    fun add(tag: GraphicsTag) { graphicElements.add(tag) }
    fun content(): String {
        val content = commands.filter { it.type == CommandType.Paragraph }.joinToString(
            " ") { it.script }
        return if (content.length > 128) content.substring(0, 127) else content
    }
}

