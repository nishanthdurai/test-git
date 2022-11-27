package com.whiture.apps.tamil.thousand.nights.models

// data class to represent each line of item in play list (both read and hear)
// this gets converted from MetaSection and MetaChapter
data class MetaAlbum(val title: String, val length: Int, val sectionId: Int,
                     val chapterId: Int, val isSection: Boolean) {

    companion object {
        fun prepare(chapter: MetaChapter) = MetaAlbum(title = chapter.title,
            length = chapter.duration, sectionId = -1, chapterId = chapter.id,
            isSection = false)

        fun prepare(section: MetaSection) = MetaAlbum(title = section.title,
            length = -1, sectionId = section.id, chapterId = -1,
            isSection = true)
    }

}
