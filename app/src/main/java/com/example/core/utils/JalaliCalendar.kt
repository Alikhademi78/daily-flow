package com.example.core.utils

import android.icu.util.Calendar
import android.icu.util.ULocale

object JalaliCalendar {
    data class JalaliDate(val year: Int, val month: Int, val day: Int) {
        override fun toString(): String {
            return "$year/${String.format("%02d", month)}/${String.format("%02d", day)}"
        }
    }

    private fun getPersianCalendar(timeMs: Long): Calendar {
        val locale = ULocale("fa_IR@calendar=persian")
        val cal = Calendar.getInstance(locale)
        cal.timeInMillis = timeMs
        return cal
    }

    fun getJalaliDate(timeMs: Long): JalaliDate {
        val cal = getPersianCalendar(timeMs)
        return JalaliDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getPersianDayName(timeMs: Long): String {
        val cal = getPersianCalendar(timeMs)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یک‌شنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
    }

    fun getPersianMonthName(month: Int): String {
        return when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }
    }

    fun formatIranDateTime(timeMs: Long): String {
        val jDate = getJalaliDate(timeMs)
        val cal = getPersianCalendar(timeMs)
        val timeStr = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        return "$jDate ساعت $timeStr"
    }
}
