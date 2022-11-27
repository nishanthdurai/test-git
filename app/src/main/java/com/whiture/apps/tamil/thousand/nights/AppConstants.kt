package com.whiture.apps.tamil.thousand.nights

object AppConstants {
    // server url
    const val ServerURL = "https://api.whiture.com"
    const val ContentURL = "https://cdn.kadalpura.com"
    const val Content2URL = "https://cdn1.kadalpura.com"
    const val AudioBookUrl = "$Content2URL/audio-book/tamil"

    // available book
    const val bookId = 1001 // id of the book available in assets, -1 if no book is available

    // calendar data
    val enMonthNamesEn = arrayOf("January", "February", "March", "April", "May", "June", "July",
        "August", "September", "October", "November", "December")
    val enMonthNamesEnShort = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
        "OCT", "NOV", "DEC")
    val enMonthNamesTa = arrayOf("ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", "ஜூலை",
        "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்")
    val enMonthNamesTaForTaMonth = arrayOf("ஏப்ரல் - மே", "மே - ஜூன்", "ஜூன் - ஜூலை",
        "ஜூலை - ஆகஸ்ட்", "ஆகஸ்ட் - செப்டம்பர்", "செப்டம்பர் - அக்டோபர்", "அக்டோபர் - நவம்பர்",
        "நவம்பர் - டிசம்பர்", "டிசம்பர்- ஜனவரி", "ஜனவரி - பிப்ரவரி", "பிப்ரவரி - மார்ச்", "மார்ச் - ஏப்ரல்")
    val taMonthNamesTa = arrayOf("சித்திரை", "வைகாசி", "ஆனி", "ஆடி", "ஆவணி", "புரட்டாசி",
        "ஐப்பசி", "கார்த்திகை", "மார்கழி", "தை", "மாசி", "பங்குனி")
    val taMonthNamesTaForEnMonth = arrayOf("மார்கழி - தை", "தை - மாசி", "மாசி - பங்குனி",
        "பங்குனி - சித்திரை", "சித்திரை - வைகாசி", "வைகாசி - ஆனி", "ஆனி - ஆடி",
        "ஆடி - ஆவணி", "ஆவணி - புரட்டாசி", "புரட்டாசி - ஐப்பசி", "ஐப்பசி - கார்த்திகை",
        "கார்த்திகை - மார்கழி")

    val enDayNamesEn = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday")
    val enDayNamesEnShort = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val taDayNamesTa = arrayOf("ஞாயிற்றுக்கிழமை", "திங்கட்கிழமை", "செவ்வாய்க்கிழமை", "புதன்கிழமை",
        "வியாழக்கிழமை", "வெள்ளிக்கிழமை", "சனிக்கிழமை")
    val taDayNamesTaShort = arrayOf("ஞாயிறு", "திங்கள்", "செவ்வாய்", "புதன்", "வியாழன்", "வெள்ளி", "சனி")

}