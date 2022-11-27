package com.whiture.apps.tamil.thousand.nights.models

import android.graphics.Bitmap

// graphics tag for rendering on the screen
// used by the custom view (PaperView) to render an image or a text on the given co-ordinates
data class GraphicsTag(val color: Int, val pointX: Float, val pointY: Float,
                       val img: Bitmap? = null, val text: String? = null, val textSize: Float = 0f) {
    val isImg: Boolean get() = (img != null)

    companion object {
        fun text(color: Int, pointX: Float, pointY: Float, text: String, textSize: Float) =
            GraphicsTag(color = color, pointX = pointX, pointY = pointY, text = text, textSize = textSize)
        fun img(color: Int, pointX: Float, pointY: Float, img: Bitmap) = GraphicsTag(color = color,
            pointX = pointX, pointY = pointY, img = img)
    }

}

