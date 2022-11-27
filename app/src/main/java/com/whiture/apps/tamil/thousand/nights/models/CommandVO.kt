package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.CommandType

// BML classes for parsing the BML files, VO classes for rendering the book content
// Each command points to the specific type - header, para, image, image credits or new line
data class CommandVO(val type: CommandType, val script: String) {

    companion object {
        fun newLine() = CommandVO(type = CommandType.NewLine, script = "")
        fun header(text: String) = CommandVO(type = CommandType.Header1, script = text)
        fun para(text: String) = CommandVO(type = CommandType.Paragraph, script = text)
        fun image(url: String) = CommandVO(type = CommandType.Image, script = url)
        fun imageCr(url: String) = CommandVO(type = CommandType.ImageCR, script = url)
    }
}

