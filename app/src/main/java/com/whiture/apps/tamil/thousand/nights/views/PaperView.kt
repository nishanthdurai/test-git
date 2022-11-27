package com.whiture.apps.tamil.thousand.nights.views

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.whiture.apps.tamil.thousand.nights.models.ChapterVO
import com.whiture.apps.tamil.thousand.nights.models.PageVO
import com.whiture.apps.tamil.thousand.nights.utils.RenderingEngine

class PaperView(context: Context, attrs: AttributeSet): View(context, attrs) {

    var engine: RenderingEngine? = null
    var page: PageVO? = null
    var totalPages = 0
    var pageIndex = 0

    fun setPage(chapter: ChapterVO, pageId: Int) {
        page = chapter.pages[pageId]
        totalPages = chapter.pages.size
        this.pageIndex = pageId + 1 // just to show 'Page 1 of 20'
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        engine?.renderPage(canvas, context, page, pageIndex, totalPages)
    }

}

