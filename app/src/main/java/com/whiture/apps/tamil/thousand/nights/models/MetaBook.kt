package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.objectArray
import org.json.JSONObject

// data class to represent the entire 1001.json file
data class MetaBook(val title: String, val id: Int, val gotImages: Boolean,
                    val sections: Array<MetaSection>) {

    companion object {
        fun parse(root: JSONObject?): MetaBook {
            root?.let { root ->
                return MetaBook(title = root.getString("title"),
                    id = root.getInt("book_id"), gotImages = root.getBoolean("has_images"),
                    sections = root.objectArray("sections").map { MetaSection.parse(it) }.toTypedArray())
            }
            return MetaBook(title = "", id = -1, gotImages = false, sections = emptyArray())
        }
    }

    fun gotAudio() = sections.any { it.gotAudio() }

    fun getPlaylist(): Array<AlbumData> {
        val albums = mutableListOf<AlbumData>()
        sections.forEach { section ->
            section.chapters.forEach { chapter ->
                albums.add(AlbumData.prepare(bookId = id, bookTitle = title,
                    sectionId = section.id, data = chapter, chapterId = chapter.id))
            }
        }
        return albums.toTypedArray()
    }

    fun getAlbums(): Array<MetaAlbum> {
        val albums = mutableListOf<MetaAlbum>()
        sections.forEach { section ->
            albums.add(MetaAlbum.prepare(section))
            section.chapters.forEach { chapter ->
                albums.add(MetaAlbum.prepare(chapter))
            }
        }
        return albums.toTypedArray()
    }

    fun getTotalDuration(): Int = sections.sumOf { it.chapters.sumOf { it.duration } }

    fun getTotalDuration(albumIndex: Int): Int {
        var index = -1
        return sections.sumOf { it.chapters.sumOf { chapter ->
            index += 1
            return@sumOf if (index >= albumIndex) chapter.duration else 0
        } }
    }

}

