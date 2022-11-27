package com.whiture.apps.tamil.thousand.nights.models

// data class to represent the chapters in 1001.json file
data class MetaChapter(val id: Int, val title: String, val duration: Int,
                       var isPlaying: Boolean = false) {

    fun gotAudio() = (duration > 0)
}


