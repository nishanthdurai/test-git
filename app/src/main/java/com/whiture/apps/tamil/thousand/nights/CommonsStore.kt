package com.whiture.apps.tamil.thousand.nights

import android.graphics.Paint
import com.whiture.apps.tamil.thousand.nights.models.ChapterVO
import com.whiture.apps.tamil.thousand.nights.models.CommandVO
import com.whiture.apps.tamil.thousand.nights.models.PageVO
import kotlin.math.ceil

// All related to book store goes here

// Marker tags for BML files
const val BMLMarker = "~~"
const val BMLTagBook = "book"
const val BMLTagChapter = "chapter"
const val BMLTagTitle = "title"
const val BMLTagDesc = "desc"
const val BMLTagPageHeading = "page_heading"
const val BMLTagImage = "img"
const val BMLTagImageCr = "imgcr"
const val BMLTagHeader1 = "header1"
const val BMLTagHeader2 = "header2"
const val BMLTagHeader3 = "header3"
const val BMLTagPara = "para"

enum class TagType {
    Chapter, Header, Img, Para, ParaNewLine,
}

enum class CommandType {
    ChapterTitle, // title of the chapter displayed on the first page
    ChapterPageHeading, // page heading displayed on every page
    ChapterDesc, // not displayed, used for reference purposes
    Paragraph, // para tag, give it a new line and space
    Header1, // type of header displayed
    Header2, // type of header displayed
    Header3, // type of header displayed
    Image, // image to be displayed
    ImageCR, // image credit section if any
    NewLine // new line to be displayed
}

/**
 * the section1.bml file will be parsed and represented as a set of BMLTag data classes
 */
data class BMLTag(
    val type: TagType,
    val script: String, // para text, image url, header text, chapter title
    val copyRightRef: String? = null, // for img
    val chapterId: Int = -1, // for chapter
    val desc: String? = null, // for chapter
    val pageHeading: String? = null, // for chapter
    val contents: Array<BMLTag>? = null, // for chapter
) {

    fun headerCommand(): CommandVO? = if (type == TagType.Chapter && pageHeading != null)
        CommandVO(type = CommandType.ChapterPageHeading, if (pageHeading.count() > 32)
            "${pageHeading.substring(0, 29)}.." else pageHeading) else null

}

// function to return only the specified chapter content
fun getBookChapter(id: Int, content: Array<String>): BMLTag? {
    var isChapterStarted = false
    var title = ""
    var desc = ""
    var heading = ""
    var inPara = false
    var particles: List<String>
    var tags: MutableList<BMLTag>? = null
    for (line in content) {
        if (line.startsWith(BMLMarker)) {
            particles = line.split(BMLMarker)
            when (particles[1]) {
                BMLTagBook -> { } // just ignore
                BMLTagChapter -> particles[2].toIntOrNull()?.let {
                    if (it == id) {
                        isChapterStarted = true
                        tags = mutableListOf()
                    }
                    else {
                        if (isChapterStarted) {
                            return BMLTag(type = TagType.Chapter, script = title, desc = desc, chapterId = id,
                                pageHeading = heading, contents = tags?.toTypedArray())
                        }
                    }
                }
                BMLTagHeader1, BMLTagHeader2, BMLTagHeader3 -> tags?.add(
                    BMLTag(type = TagType.Header, script = particles[2]))
                BMLTagTitle -> title = particles[2]
                BMLTagDesc -> desc = particles[2]
                BMLTagPageHeading -> heading = particles[2]
                BMLTagImage -> tags?.add(BMLTag(type = TagType.Img, script = particles[2]))
                BMLTagImageCr -> tags?.lastOrNull { it.type == TagType.Img }?.let { img ->
                    tags?.remove(img)
                    tags?.add(BMLTag(type = TagType.Img, script = img.script,
                        copyRightRef = particles[2] ))
                }
                BMLTagPara -> inPara = true
            }
        }
        else {
            if (isChapterStarted) {
                if (inPara) {
                    tags?.add(BMLTag(type = TagType.Para, script = line))
                    inPara = false // added, set it false
                }
                else {
                    tags?.add(BMLTag(type = TagType.ParaNewLine, script = line))
                }
            }
        }
    }
    if (isChapterStarted) {
        return BMLTag(type = TagType.Chapter, script = title, desc = desc, chapterId = id,
            pageHeading = heading, contents = tags?.toTypedArray())
    }
    return null
}

// process the book chapter and return the chapterVO which will have an array of pageVO for each page
fun processBookChapter(chapter: BMLTag, paint: Paint, contentWidth: Float, contentHeight: Float,
                       pageHeaderFontSize: Float, titleFontSize: Float, headerFontSize: Float,
                       paraFontSize: Float, lineSpace: Float, rootPath: String): ChapterVO? {
    // method to add the given number of spaces to the sentence
    fun addSpaces(para: String, spaces: Int): String {
        val words = para.split(" ").toTypedArray()
        if (words.size == 1) return para
        val total = spaces + words.size - 1
        var index = 0
        repeat(total) {
            words[index] += " "
            index += 1
            if (index >= words.size - 1) {
                index = 0
            }
        }
        return words.joinToString(separator = "")
    }

    // method to break the given texts into lines by manipulating the size required to render by
    // the paint object
    fun processText(text: String, spaceMeasureWidth: Float): Array<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        val buffer = StringBuilder()
        var sentenceWidth = 0f
        words.forEach { word ->
            val wordWidth = paint.measureText(word)
            if (sentenceWidth + wordWidth + spaceMeasureWidth > contentWidth) {
                // we need to break the sentence here, more than line space, so bring the word
                // to next line, check if sentence measure size is more than 1/2 (5/6 earlier)
                // of line width to add spaces
                lines.add(if (sentenceWidth > 0.5f * contentWidth) {
                    addSpaces(buffer.toString().trim(),
                        ceil((contentWidth - sentenceWidth) / spaceMeasureWidth).toInt())
                }
                else {
                    buffer.toString().trim()
                })
                // clear the current line
                buffer.clear()
                sentenceWidth = 0f
            }
            buffer.append("$word ") // append the word
            sentenceWidth += wordWidth + spaceMeasureWidth
        }
        lines.add(buffer.toString().trim())
        return lines.toTypedArray()
    }

    // method to swap the para new line and para tags as this will help us in rendering properly
    fun processParaNewLines(contents: Array<BMLTag>?) {
        contents?.forEachIndexed { index, tag ->
            if (index > 0) {
                if (tag.type == TagType.ParaNewLine) {
                    contents[index - 1].let { prevTag ->
                        if (prevTag.type == TagType.Para) { // swap
                            contents[index - 1] = BMLTag(type = TagType.ParaNewLine, script = prevTag.script)
                            contents[index] = BMLTag(type = TagType.Para, script = tag.script)
                        }
                    }
                }
            }
        }
    }

    // current page's set of commands
    val commands = mutableListOf<CommandVO>()
    // current chapter's set of pages
    val pages = mutableListOf<PageVO>()

    // initialize space size for different fonts
    paint.textSize = titleFontSize
    val titleFontSpaceWidth = paint.measureText(" ")
    paint.textSize = headerFontSize
    val headerFontSpaceWidth = paint.measureText(" ")
    paint.textSize = paraFontSize
    val paraFontSpaceWidth = paint.measureText(" ")

    var yAxis = 0f // current y axis pointer

    // method to add new line command to the page VO
    fun addNewLine(fontSize: Float) {
        yAxis += fontSize + lineSpace
        commands.add(CommandVO.newLine())
    }

    // method to add the command for the given text
    fun addCommand(text: String, type: CommandType, fontSize: Float, spaceWidth: Float) {
        paint.textSize = fontSize
        processText(text, spaceWidth).forEach { line ->
            yAxis += fontSize + lineSpace
            commands.add(CommandVO(type = type, script = line))
        }
    }

    // method to add new page and reset the y pointer
    fun addNewPage(pointer: Float) {
        chapter.headerCommand()?.let {
            pages.add(PageVO(commands = commands.toTypedArray()))
            commands.clear()
            commands.add(it)
        }
        yAxis = pointer
    }

    // add chapter title and new line
    addCommand(chapter.script, CommandType.ChapterTitle, titleFontSize, titleFontSpaceWidth)
    addNewLine(titleFontSize)

    // add chapter desc and new line
    if (chapter.desc?.isNotBlank() == true) {
        addCommand(chapter.desc, CommandType.ChapterDesc, paraFontSize, paraFontSpaceWidth)
        addNewLine(paraFontSize)
    }

    // add chapter content, create new page VO as we fill the page
    paint.textSize = paraFontSize
    processParaNewLines(chapter?.contents)
    chapter.contents?.forEach { tag ->
        when(tag.type) {
            TagType.Chapter -> { } // do nothing, we are already dealing this
            TagType.Header -> {
                paint.textSize = headerFontSize
                processText(tag.script, headerFontSpaceWidth).forEach { line ->
                    yAxis += headerFontSize
                    if (yAxis > contentHeight) { // add new page & reset y pointer
                        addNewPage(pageHeaderFontSize + lineSpace + headerFontSize)
                    }
                    yAxis += lineSpace
                    commands.add(CommandVO.header(line))
                }
                addNewLine(paraFontSize)
                paint.textSize = paraFontSize
            }
            TagType.Img -> {
                yAxis += contentWidth * 0.5f
                if (yAxis > contentHeight) { // add new page & reset y pointer
                    addNewPage(pageHeaderFontSize + lineSpace + contentWidth * 0.5f)
                }
                // add image
                yAxis += lineSpace
                commands.add(CommandVO.image("$rootPath${tag.script}"))
                // add image copyright text
                yAxis += paraFontSize + lineSpace
                commands.add(CommandVO.imageCr(tag.copyRightRef ?: ""))
            }
            TagType.Para, TagType.ParaNewLine -> {
                processText(tag.script, paraFontSpaceWidth).forEach { line ->
                    yAxis += paraFontSize
                    if (yAxis > contentHeight) { // add new page & reset y pointer
                        addNewPage(pageHeaderFontSize + lineSpace + paraFontSize)
                    }
                    yAxis += lineSpace
                    commands.add(CommandVO.para(line))
                }
                if (tag.type == TagType.ParaNewLine) {
                    // don't add new line spacing for para
                    // yAxis += paraFontSize;
                }
                else {
                    addNewLine(paraFontSize)
                }
            }
        }
    }
    if (commands.isNotEmpty()) addNewPage(0f) // for the remaining commands
    return ChapterVO(pages = pages.toTypedArray())
}

