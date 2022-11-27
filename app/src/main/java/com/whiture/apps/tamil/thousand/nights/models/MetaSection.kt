package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.intArray
import com.whiture.apps.tamil.thousand.nights.stringArray
import org.json.JSONObject

// data class to represent the sections in 1001.json file
data class MetaSection(val id: Int, val title: String, val chapters: Array<MetaChapter>) {
    companion object {
        fun parse(json: JSONObject): MetaSection {
            val chapters = mutableListOf<MetaChapter>()
            val titles = json.stringArray("chapters")
            if (json.has("chapterTiming")) {
                json.intArray("chapterTiming").forEachIndexed { i, t ->
                    chapters.add(MetaChapter(i + 1, titles[i], t))
                }
            }
            else {
                titles.forEachIndexed { i, title ->
                    chapters.add(MetaChapter(i + 1, titles[i], 0))
                }
            }
            return MetaSection(json.getInt("id"), json.getString("title"),
                chapters.toTypedArray())
        }
    }

    fun gotAudio() = chapters.any { it.gotAudio() }

    fun stopPlaying() {
        chapters.forEach { it.isPlaying = false }
    }
}
