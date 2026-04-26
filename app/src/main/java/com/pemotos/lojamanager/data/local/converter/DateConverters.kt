package com.pemotos.lojamanager.data.local.converter

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

class DateConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}
