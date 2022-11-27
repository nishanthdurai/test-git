package com.whiture.apps.tamil.thousand.nights.models

import com.whiture.apps.tamil.thousand.nights.AC

// album data class for Media Player Service to process
data class AlbumData(
    val title: String, // title for the notification
    val desc: String, // description to be shown on the notification
    val url: String, // the url of the mp3 file
    val length: Long, // length of the audio file in milli-seconds
    var repeat: Int = 0, // total times to be repeated by the media player, default 0, useful for Mandirams
    var sectionId: Int, // book section id
    var chapterId: Int, // book section's chapter id
) {
    companion object {
        fun prepare(bookId: Int, bookTitle: String, sectionId: Int, chapterId: Int, data: MetaChapter)
        = AlbumData(title = bookTitle, desc = "$sectionId.${data.id} ${data.title}",
            url = "${AC.AudioBookUrl}/$bookId/${bookId}_${sectionId}_${data.id}.mp3",
            length = data.duration.toLong(), sectionId = sectionId, chapterId = chapterId)
    }

}
