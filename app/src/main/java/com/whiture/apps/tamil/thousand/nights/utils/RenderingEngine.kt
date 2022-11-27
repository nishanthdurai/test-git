package com.whiture.apps.tamil.thousand.nights.utils

import android.content.Context
import android.graphics.*
import com.whiture.apps.tamil.thousand.nights.CommandType.*
import com.whiture.apps.tamil.thousand.nights.logE
import com.whiture.apps.tamil.thousand.nights.models.GraphicsTag
import com.whiture.apps.tamil.thousand.nights.models.PageVO

// Class to render the given ChapterVO object (rendering commands) into the given view
class RenderingEngine(val paint: Paint, var lineSpacing: Float, var titleFontSize: Float,
                      var headerFontSize: Float, var paraFontSize: Float,
                      var pageHeaderFontSize: Float, val pageWidth: Float, var fontColor: Int,
                      var crFontColor: Int, val pointX: Float, var pointY: Float,
                      val hasImagesDownloaded: Boolean) {

    // Origin Pointer Y - this is required as resume operation in activity will push
    // the content down due to pointY incrementing
    private var originY: Float = pointY

    init {
        paint.isAntiAlias = true
        paint.textSize = paraFontSize
        paint.color = fontColor
    }

    // method to render the given set of commands in Page VO to the given canvas object
    fun renderPage(canvas: Canvas?, context: Context, pageVO: PageVO?, pageIndex: Int,
                   totalPages: Int) {
        // this methods gets called repeatedly (10+ times), better to capture values to
        // avoid repeated calculation
        if (pageVO?.graphicElements?.isEmpty() == true) {
            // set current pointer Y to origin pointer Y, as every time, the onResume called on
            // activity, this rendering takes place again
            pointY = originY
            // reset paint dynamics
            paint.textSize = paraFontSize
            paint.color = fontColor
            // now render each command
            pageVO.commands.forEach { command ->
                when (command.type) {
                    ChapterTitle -> {
                        pointY += lineSpacing + titleFontSize
                        pageVO.add(GraphicsTag.text(fontColor, pointX, pointY, command.script,
                            titleFontSize))
                    }
                    ChapterDesc -> {
                        pointY += lineSpacing + paraFontSize
                        renderText(pageVO, command.script, paraFontSize,
                            crFontColor, pageWidth, pointX, pointY)
                    }
                    ChapterPageHeading -> {
                        pointY += lineSpacing + pageHeaderFontSize
                        // heading - to right
                        paint.textSize = pageHeaderFontSize
                        pageVO.add(GraphicsTag.text(crFontColor,
                            pointX + (pageWidth - paint.measureText(command.script)),
                            pointY, command.script, pageHeaderFontSize))
                        // page no - to left
                        pageVO.add(GraphicsTag.text(crFontColor, pointX, pointY,
                            "Page $pageIndex of $totalPages", pageHeaderFontSize))
                    }
                    Paragraph -> {
                        pointY += lineSpacing + paraFontSize
                        pageVO.add(GraphicsTag.text(fontColor, pointX, pointY, command.script,
                            paraFontSize))
                    }
                    Header1, Header2, Header3 -> {
                        pointY += lineSpacing + headerFontSize
                        pageVO.add(GraphicsTag.text(fontColor, pointX, pointY, command.script,
                            headerFontSize))
                    }
                    Image -> {
                        var image: Bitmap? = null
                        try {
                            image = if (hasImagesDownloaded) {
                                BitmapFactory.decodeFile(command.script)
                            }
                            else {
                                BitmapFactory.decodeStream(context.assets.open("noimg.png"))
                            }
                        }
                        catch (e: Exception) {
                            logE(e, "Exception while rendering image")
                        }
                        if (image == null) {
                            image = BitmapFactory.decodeStream(context.assets.open("noimg.png"))
                        }
                        image?.let { pageVO.add(GraphicsTag.img(fontColor, pointX, pointY, it)) }
                        pointY += (pageWidth * 0.5f)
                    }
                    ImageCR -> {
                        pointY += lineSpacing + paraFontSize
                        // align it to right
                        paint.textSize = paraFontSize
                        pageVO.add(GraphicsTag.text(crFontColor,
                            pointX + (pageWidth - paint.measureText(command.script)),
                            pointY, command.script, paraFontSize))
                    }
                    NewLine -> {
                        pointY += lineSpacing + paraFontSize
                    }
                }
            }
        }

        // draw it on the canvas
        pageVO?.graphicElements?.forEach { element ->
            paint.color = element.color
            if (element.isImg) {
                element.img?.let{ canvas?.drawBitmap(it, null, Rect(element.pointX.toInt(),
                    element.pointY.toInt(), (element.pointX + pageWidth).toInt(),
                    (element.pointY + (pageWidth * 0.5f)).toInt()), paint) }
            }
            else { // text
                paint.textSize = element.textSize
                element.text?.let { canvas?.drawText(it, element.pointX, element.pointY, paint) }
            }
        }
    }

    // method to render the text on the given specified area
    private fun renderText(pageVO: PageVO, text: String, textSize: Float, color: Int, width: Float,
                           pointX: Float, pointY: Float) {
        paint.textSize = textSize
        val textWidth = paint.measureText(text)
        if (textWidth > 0.95f * width) { // overflowing, in some cases
            // we are going to render the last word on the same line at end
            // however, this approach needs to be fixed
            val lastSpaceIndex = text.lastIndexOf(' ')
            if (lastSpaceIndex > 0) {
                val lastWord = text.substring(lastSpaceIndex)
                // draw first part of text until last word in the line
                pageVO.add(GraphicsTag.text(color, pointX, pointY,
                    text.substring(0, lastSpaceIndex), textSize))
                // draw the last word in the same line at the end, overlapping will happen here
                pageVO.add(GraphicsTag.text(color, pointX + width - paint.measureText(lastWord),
                    pointY, lastWord, textSize))
            }
            else { // single word, just draw it anyway
                pageVO.add(GraphicsTag.text(color, pointX, pointY, text, textSize))
            }
        }
        else { // just draw the text
            pageVO.add(GraphicsTag.text(color, pointX, pointY, text, textSize))
        }
    }

    fun setFontColor(fontColor: Int, crFontColor: Int) {
        this.fontColor = fontColor
        this.crFontColor = crFontColor
    }

    fun setFontSizes(titleFontSize: Float, headerFontSize: Float,
                     paraFontSize: Float, pageHeaderFontSize: Float) {
        this.titleFontSize = titleFontSize
        this.headerFontSize = headerFontSize
        this.paraFontSize = paraFontSize
        this.pageHeaderFontSize = pageHeaderFontSize
    }

}

