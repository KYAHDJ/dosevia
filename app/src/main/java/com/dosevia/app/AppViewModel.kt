package com.dosevia.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppState(
    val pillType: PillType = PillType.TYPE_21_7,
    val startDate: Date = Date(),
    val days: List<DayData> = emptyList(),
    val settings: ReminderSettings = ReminderSettings(),
    val customPillConfig: CustomPillConfig = CustomPillConfig()
)

class AppViewModel(private val context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("dosevia_prefs", Context.MODE_PRIVATE)
    private val gson: Gson

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, JsonDeserializer { json, _, _ ->
                try { dateFormat.parse(json.asString) } catch (e: Exception) { Date(json.asLong) }
            })
            .registerTypeAdapter(Date::class.java, JsonSerializer<Date> { src, _, _ ->
                com.google.gson.JsonPrimitive(dateFormat.format(src))
            })
            .create()
        loadState()
    }

    private fun loadState() {
        viewModelScope.launch {
            val pillTypeStr = prefs.getString("pillType", PillType.TYPE_21_7.name) ?: PillType.TYPE_21_7.name
            val pillType = try { PillType.valueOf(pillTypeStr) } catch (e: Exception) { PillType.TYPE_21_7 }
            val startDateMs = prefs.getLong("startDate", Date().time)
            val startDate = Date(startDateMs)
            val customJson = prefs.getString("customPillConfig", null)
            val customConfig = if (customJson != null) {
                try { gson.fromJson(customJson, CustomPillConfig::class.java) } catch (e: Exception) { CustomPillConfig() }
            } else CustomPillConfig()

            val daysJson = prefs.getString("days", null)
            val savedDays: List<DayData>? = if (daysJson != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<DayData>>() {}.type
                    gson.fromJson(daysJson, type)
                } catch (e: Exception) { null }
            } else null

            val days = generateDays(pillType, startDate, customConfig, savedDays)

            _state.value = AppState(
                pillType = pillType,
                startDate = startDate,
                days = days,
                customPillConfig = customConfig
            )
        }
    }

    private fun saveState() {
        val state = _state.value
        prefs.edit()
            .putString("pillType", state.pillType.name)
            .putLong("startDate", state.startDate.time)
            .putString("customPillConfig", gson.toJson(state.customPillConfig))
            .putString("days", gson.toJson(state.days))
            .apply()
    }

    private fun generateDays(
        pillType: PillType,
        startDate: Date,
        customConfig: CustomPillConfig,
        savedDays: List<DayData>?
    ): List<DayData> {
        val config = getPillConfiguration(pillType, customConfig)
        val days = mutableListOf<DayData>()
        val cal = Calendar.getInstance()
        cal.time = startDate
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        for (i in 0 until config.total) {
            val isPlacebo = i >= config.active && config.lowDose == 0 && config.placebo > 0
            val isLowDose = i >= config.active && config.lowDose > 0 && i < (config.active + config.lowDose)
            val date = cal.time

            // Restore status from saved data
            val savedStatus = savedDays?.find { sd ->
                val sdCal = Calendar.getInstance()
                sdCal.time = sd.date
                sdCal.set(Calendar.HOUR_OF_DAY, 0); sdCal.set(Calendar.MINUTE, 0)
                sdCal.set(Calendar.SECOND, 0); sdCal.set(Calendar.MILLISECOND, 0)
                val thisCal = Calendar.getInstance()
                thisCal.time = date
                thisCal.set(Calendar.HOUR_OF_DAY, 0); thisCal.set(Calendar.MINUTE, 0)
                thisCal.set(Calendar.SECOND, 0); thisCal.set(Calendar.MILLISECOND, 0)
                sdCal.timeInMillis == thisCal.timeInMillis
            }

            days.add(DayData(
                day = i + 1,
                status = savedStatus?.status ?: PillStatus.NOT_TAKEN,
                isPlacebo = isPlacebo,
                isLowDose = isLowDose,
                date = date,
                takenAt = savedStatus?.takenAt
            ))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }

    fun changePillType(pillType: PillType) {
        val newDays = generateDays(pillType, _state.value.startDate, _state.value.customPillConfig, _state.value.days)
        _state.value = _state.value.copy(pillType = pillType, days = newDays)
        saveState()
    }

    fun changeStartDate(date: Date) {
        val newDays = generateDays(_state.value.pillType, date, _state.value.customPillConfig, _state.value.days)
        _state.value = _state.value.copy(startDate = date, days = newDays)
        saveState()
    }

    fun changeCustomPillConfig(active: Int, placebo: Int, lowDose: Int) {
        val customConfig = CustomPillConfig(active, placebo, lowDose)
        val newDays = generateDays(PillType.CUSTOM, _state.value.startDate, customConfig, _state.value.days)
        _state.value = _state.value.copy(
            pillType = PillType.CUSTOM,
            customPillConfig = customConfig,
            days = newDays
        )
        saveState()
    }

    fun updateDayStatus(day: Int, status: PillStatus) {
        val updatedDays = _state.value.days.map { d ->
            if (d.day == day) d.copy(
                status = status,
                takenAt = if (status == PillStatus.TAKEN) System.currentTimeMillis() else null
            )
            else d
        }
        _state.value = _state.value.copy(days = updatedDays)
        saveState()
    }
}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AppViewModel(context) as T
    }
}
