package com.whiture.apps.tamil.thousand.nights

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ta month index - 1..12, en month index - 1..12
// based on starting date
fun enMonthIndexForTaMonth(taMonthIndex: Int): Int =
    if (taMonthIndex + 3 > 12) (taMonthIndex + 3) - 12 else taMonthIndex + 3

// ta month index - 1..12, en month index - 1..12
// based on starting date
fun taMonthIndexForEnMonth(enMonthIndex: Int): Int =
    if (enMonthIndex - 4 < 1) enMonthIndex + 8 else enMonthIndex - 4

// function to get an instance of calendar set to the given date
fun calendar(date: Date): Calendar {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return calendar
}

// function to get an instance of calendar set to the given date
// year: 2019, 2020 etc, month: 1-12, date: 1-31
fun calendar(year: Int, month: Int, date: Int): Calendar {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    calendar.set(Calendar.DATE, date)
    return calendar
}

// function to get an instance of calendar set to the given date
fun calendar(year: Int, month: Int): Calendar {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    calendar.set(Calendar.MONTH, month - 1)
    return calendar
}

// function to get an instance of calendar set to the given date
fun calendar(year: Int): Calendar {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, year)
    return calendar
}

// month = 1..12, year = 2021, 2022 etc
fun totalDaysInMonth(year: Int, month: Int): Int = when (month) {
    in arrayOf(1, 3, 5, 7, 8, 10, 12) -> 31
    2 -> if (year % 4 == 0) 29 else 28
    else -> 30
}

// year, month (1..12) and date
fun Calendar.get() = Triple(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DATE))

// get the total days since the beginning of year 2020
fun Date.totalDaysSince2020(): Int {
    dateFromString("01/01/2020", "dd/MM/yyyy")?.let {
        return this.daysBetween(it)
    }
    return 0
}

fun Date.totalDays(dateString: String): Int {
    dateFromString(dateString, "dd/MM/yyyy")?.let {
        return this.daysBetween(it)
    }
    return 1
}

/**
 * method to find the number of days between the current date and the next occurring date given in
 * current month and next month
 * returns:
 *  null: if data not found
 *  first: 0 - today, 1 - tomorrow, 2 - day after tomorrow
 *  second: the possible date which is in future
 */
fun Date.diff(thisMonthDates: Array<Int>, nextMonthDates: Array<Int>): Pair<Int, Date?>? {
    val calendar = calendar(this)
    thisMonthDates.sorted().firstOrNull { it >= calendar.get().third }?.let { nextDate ->
        val (year, month, _) = calendar.get()
        calendar(year, month, nextDate).let {
            return Pair(it.time.diffInDays(this), it.time)
        }
    }
    if (nextMonthDates.isNotEmpty()) {
        calendar(this.nextMonth).let {
            it.set(Calendar.DATE, nextMonthDates.sorted()[0])
            return Pair(it.time.diffInDays(this), it.time)
        }
    }
    return null
}

// method to find out if the date is in between the given start and end dates
fun Date.isBetween(start: Date, end: Date) = (this.time >= start.time && this.time <= end.time)

// method to find out if the date is in between the given start and end dates
fun Date.isBetween(start: Triple<Int, Int, Int>, end: Triple<Int, Int, Int>): Boolean {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, start.third)
    calendar.set(Calendar.MONTH, start.second - 1)
    calendar.set(Calendar.DATE, start.first)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    val startTime = calendar.time
    calendar.set(Calendar.YEAR, end.third)
    calendar.set(Calendar.MONTH, end.second - 1)
    calendar.set(Calendar.DATE, end.first)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    val endTime = calendar.time
    return (this > startTime && this < endTime)
}

// Date Class Extension methods
val Date.displayStringWithSlash: String
    get() = SimpleDateFormat("dd/MM/yyyy").format(this)

val Date.displayStringWithHyphen: String
    get() = SimpleDateFormat("dd-MM-yyyy").format(this)

val Date.displayStringYYYYMMDD: String
    get() = SimpleDateFormat("yyyy-MM-dd").format(this)

val Date.displayStringNoSeparator: String
    get() = SimpleDateFormat("ddMMyyyy").format(this)

val Date.displayString: String
    get() = SimpleDateFormat("dd.MM.yyyy").format(this)

val Date.monthEn: String get() = getEnMonthName(
    month = calendar(this).get(Calendar.MONTH), isEn = true)

val Date.monthTa: String get() = getEnMonthName(month = calendar(this).get(Calendar.MONTH))

// Sunday to Saturday = 0..6
val Date.dayIndex: Int
    get() = calendar(this).get(Calendar.DAY_OF_WEEK) - 1 // starts from Sunday

val Date.dayTa: String get() = getWeekDay(
    day = calendar(this).get(Calendar.DAY_OF_WEEK), isEn = false, isShorter = true)

val Date.dayTaLengthier: String get() = getWeekDay(
    day = calendar(this).get(Calendar.DAY_OF_WEEK), isEn = false, isShorter = false)

val Date.dayEn: String get() = getWeekDay(
    day = calendar(this).get(Calendar.DAY_OF_WEEK), isEn = true, isShorter = false)

val Date.dayEnShort: String get() = getWeekDay(
    day = calendar(this).get(Calendar.DAY_OF_WEEK), isEn = true, isShorter = true)

val Date.nextDay: Date
    get() {
        val calendar = calendar(this)
        calendar.add(Calendar.DATE, 1)
        return calendar.time
    }

val Date.prevDay: Date
    get() {
        val calendar = calendar(this)
        calendar.add(Calendar.DATE, -1)
        return calendar.time
    }

val Date.nextMonth: Date
    get() {
        val calendar = calendar(this)
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DATE, 1)
        return calendar.time
    }

val Date.prevMonth: Date
    get() {
        val calendar = calendar(this)
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DATE, 1)
        return calendar.time
    }

val Date.currentMonth: Int get() = calendar(this).get(Calendar.MONTH)

val Date.currentYear: Int get() = calendar(this).get(Calendar.YEAR)

// starts from 1 to 54
val Date.week: Int get() = calendar(this).get(Calendar.WEEK_OF_YEAR)

// the starting day of the week - sunday
val Date.startOfWeek: Date
    get() {
        val calendar = calendar(this)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            calendar.add(Calendar.DATE, -1)
        }
        return calendar.time
    }

// the ending day of the week - saturday
val Date.endOfWeek: Date
    get() {
        val calendar = calendar(this)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE, 1)
        }
        return calendar.time
    }

// check if we have the data available with us
val Date.hasData: Boolean
    get() {
        val calendar = calendar(this)
        val year = calendar.get(Calendar.YEAR)
        return (year in 2021..2025)
    }

// check if we have the data available with us
val Date.hasDataForDownload: Boolean
    get() {
        val calendar = calendar(this)
        val year = calendar.get(Calendar.YEAR)
        return (year in 2021..2025)
    }

// get the day of the year 1-356
val Date.dayOfYear: Int get() = calendar(this).get(Calendar.DAY_OF_YEAR)

// get the day of the year 1-31
val Date.dateOfMonth: Int get() = calendar(this).get(Calendar.DATE)

// get time in the hh:mm am/pm format
val Date.timeHHMM: String
    get() = SimpleDateFormat("h:mm a").format(this)

// total mins since the midnight 12 AM
val Date.minsSinceMidnight: Int
    get() = calendar(this).let { return (it.get(Calendar.HOUR_OF_DAY) * 60) +
            it.get(Calendar.MINUTE) }

// total mins since morning 6AM or evening 6PM
var Date.minsSinceSunMoonPhase: Int
    get() {
        return when (val mins = minsSinceMidnight) {
            in 0..360 -> mins + 360 // midnight to morning 6AM
            in 361..1080 -> mins - 360 // morning 6AM to evening 6PM
            in 1081..1440 -> mins - 1080 // evening 6PM to midnight
            else -> 0
        }
    }
    private set(_) {}

// if the sun or moon phase is happening
val Date.isSunPhase: Boolean
    get() = (minsSinceMidnight in 360..1080) // between morning 6AM to evening 6PM in mins

// method to convert Room date into string for notes
val Date.roomDateToString: String
    get() = SimpleDateFormat("EEE, MMM d yyyy").format(this)

// find out if both dates are same or not
fun Date.isSameDate(to: Date?): Boolean = if (to != null) {
    val calendar = calendar(this)
    val current = Triple(
        calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH),
        calendar.get(Calendar.YEAR)
    )
    calendar.time = to
    val target = Triple(
        calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH),
        calendar.get(Calendar.YEAR)
    )
    (current.first == target.first && current.second == target.second
            && current.third == target.third)
}
else false

// convert calendar to date
fun convertToDate(calendar: Calendar): Date {
    return calendar.time
}
// find out the difference between two dates in days (in absolute values)
fun Date.diffInDays(target: Date): Int = TimeUnit.MILLISECONDS.toDays(
    kotlin.math.abs(this.time - target.time)
).toInt()

// find out the difference between two dates in hours (in absolute values)
fun Date.diffInHours(target: Date): Int = TimeUnit.MILLISECONDS.toHours(
    kotlin.math.abs(this.time - target.time)
).toInt()

// find out the difference between two dates in minutes (in absolute values)
fun Date.diffInMins(target: Date): Int = TimeUnit.MILLISECONDS.toMinutes(
    kotlin.math.abs(this.time - target.time)
).toInt()

// find out the difference between two dates in seconds (in absolute values)
fun Date.diffInSecs(target: Date): Int = TimeUnit.MILLISECONDS.toSeconds(
    kotlin.math.abs(this.time - target.time)
).toInt()

fun getWeekDay(day: Int, isEn: Boolean = false, isShorter: Boolean = false) = if (isEn) (
        if (isShorter) AC.enDayNamesEnShort[day - 1] else AC.enDayNamesEn[day - 1]
        ) else (if (isShorter) AC.taDayNamesTaShort[day - 1] else AC.taDayNamesTa[day - 1])

fun getAllWeekDay(isEn: Boolean = false, isShorter: Boolean = false) : Array<String> {
    val weekDayList = mutableListOf<String>()
    repeat(8) { day ->
        if(day != 0) { weekDayList.add(getWeekDay(day, isEn, isShorter)) }
    }
    return weekDayList.toTypedArray()
}

fun getEnMonthName(month: Int, isEn: Boolean = false) = if (isEn) AC.enMonthNamesEn[
        month] else AC.enMonthNamesTa[month]

// year: 2018, 2019 etc, month: 1-12, date: 1-31
fun getWeekDayInTamil(year: Int, month: Int, date: Int): String = Calendar.getInstance().let {
    it.set(Calendar.YEAR, year)
    it.set(Calendar.MONTH, month - 1)
    it.set(Calendar.DATE, date)
    it.time.dayTa
}

// converts milliseconds to minute and second
fun milliSecondsToMinutesAndSeconds(duration: Int): String {
    val minutes = (duration / 1000) / 60
    val seconds = (duration / 1000) % 60
    return "${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"
}


