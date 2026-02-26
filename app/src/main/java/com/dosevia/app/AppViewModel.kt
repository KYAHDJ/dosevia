package com.dosevia.app

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "DoseviaVM"

data class AppState(
    val pillType: PillType = PillType.TYPE_21_7,
    val startDate: Date = Date(),
    val days: List<DayData> = emptyList(),
    val settings: ReminderSettings = ReminderSettings(),
    val customPillConfig: CustomPillConfig = CustomPillConfig(),
    val notes: List<Note> = emptyList(),
    val widgetThemes: WidgetThemeSettings = defaultWidgetThemeSettings()
)

class AppViewModel(private val context: Context) : ViewModel() {

    // Always use applicationContext — never Activity context
    private val appContext = context.applicationContext

    // Two separate prefs files:
    // 1. dosevia_prefs  — config (pillType, startDate, settings, notes, customConfig)
    // 2. dosevia_status — per-day status stored as "status_YYYY-MM-DD" keys
    //    This is bulletproof: no JSON parsing, no date serialization issues.
    private val configPrefs = appContext.getSharedPreferences("dosevia_prefs",  Context.MODE_PRIVATE)
    private val statusPrefs = appContext.getSharedPreferences("dosevia_status", Context.MODE_PRIVATE)

    private val gson: Gson
    private val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        gson = GsonBuilder()
            .registerTypeAdapter(Date::class.java, JsonDeserializer { json, _, _ ->
                try { dateFormat.parse(json.asString) } catch (e: Exception) {
                    try { Date(json.asLong) } catch (e2: Exception) { Date() }
                }
            })
            .registerTypeAdapter(Date::class.java, JsonSerializer<Date> { src, _, _ ->
                com.google.gson.JsonPrimitive(dateFormat.format(src))
            })
            .create()

        loadState()
        ensureNotificationChannel(appContext)
        checkAlarmTakenFlag()
        // Push correct data to widgets immediately on startup (catches NOT_TAKEN→MISSED corrections)
        PillWidget.requestUpdate(appContext)
        PillWidgetMedium.requestUpdate(appContext)
        PillWidgetCalendar.requestUpdate(appContext)
    }

    // ── Date key helpers ──────────────────────────────────────────────────────

    /** Normalise a Date to midnight local time */
    private fun normDate(d: Date): Long {
        val c = Calendar.getInstance()
        c.time = d
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun todayMs() = normDate(Date())

    /** "2025-02-23" key for a given date — used as statusPrefs key */
    private fun dateKey(date: Date) = keyFmt.format(date)

    // ── Status storage — per-day key/value (bulletproof) ─────────────────────

    /** Save one day's status directly to statusPrefs. */
    private fun saveStatus(date: Date, status: PillStatus, takenAt: Long?) {
        val k = dateKey(date)
        statusPrefs.edit()
            .putString("status_$k", status.name)
            .also { ed ->
                if (takenAt != null) ed.putLong("takenAt_$k", takenAt)
                else ed.remove("takenAt_$k")
            }
            .commit()   // synchronous — survives process death
        Log.d(TAG, "saveStatus: $k → ${status.name}")
    }

    /** Read one day's status from statusPrefs. Returns null if never written. */
    private fun loadStatus(date: Date): Pair<PillStatus, Long?>? {
        val k   = dateKey(date)
        val raw = statusPrefs.getString("status_$k", null) ?: return null
        val st  = try { PillStatus.valueOf(raw) } catch (_: Exception) { return null }
        val at  = if (statusPrefs.contains("takenAt_$k")) statusPrefs.getLong("takenAt_$k", 0) else null
        return Pair(st, at)
    }

    // ── Day list builder ──────────────────────────────────────────────────────

    /**
     * Builds the full day list for the current pillType + startDate.
     * For each day:
     *   1. Check statusPrefs for a user-saved status → use it exactly as-is.
     *   2. If no saved status, auto-assign: past → MISSED, today/future → NOT_TAKEN.
     * TAKEN is NEVER overwritten. MISSED set by user is NEVER overwritten.
     */
    private fun buildDays(
        pillType: PillType,
        startDate: Date,
        customConfig: CustomPillConfig
    ): List<DayData> {
        val config  = getPillConfiguration(pillType, customConfig)
        val today   = todayMs()
        val days    = mutableListOf<DayData>()
        val cal     = Calendar.getInstance()
        cal.time    = startDate
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)

        for (i in 0 until config.total) {
            val isPlacebo = config.lowDose == 0 && config.placebo > 0 && i >= config.active
            val isLowDose = config.lowDose > 0 && i >= config.active && i < (config.active + config.lowDose)
            val date      = cal.time
            val dateMs    = cal.timeInMillis

            val saved = loadStatus(date)
            val (status, takenAt) = if (saved != null) {
                // User explicitly set this status — respect it, EXCEPT:
                // if it's a past day still sitting at NOT_TAKEN, correct it to MISSED.
                if (dateMs < today && saved.first == PillStatus.NOT_TAKEN) {
                    saveStatus(date, PillStatus.MISSED, null)
                    Pair(PillStatus.MISSED, null)
                } else {
                    saved
                }
            } else {
                // No saved status — auto-assign based on date
                val autoStatus = if (dateMs < today) PillStatus.MISSED else PillStatus.NOT_TAKEN
                autoStatus.also { if (dateMs < today) saveStatus(date, it, null) }
                Pair(autoStatus, null)
            }

            days.add(DayData(
                day       = i + 1,
                status    = status,
                isPlacebo = isPlacebo,
                isLowDose = isLowDose,
                date      = date,
                takenAt   = takenAt
            ))
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }

    // ── Alarm helper ──────────────────────────────────────────────────────────

    private fun applyAlarm(settings: ReminderSettings, days: List<DayData>) {
        if (!settings.appActive) { cancelAlarm(appContext); return }

        val today     = normDate(Date())
        val todayPill = days.firstOrNull { normDate(it.date) == today }
        val isPlacebo = todayPill?.isPlacebo == true || todayPill?.isLowDose == true
        if (isPlacebo && !settings.placeboReminder) { cancelAlarm(appContext); return }
        if (todayPill?.status == PillStatus.TAKEN)  { cancelAlarm(appContext); return }

        scheduleAlarm(
            context           = appContext,
            hour              = settings.dailyReminderHour,
            minute            = settings.dailyReminderMinute,
            title             = settings.notificationTitle,
            subtitle          = settings.notificationSubtitle,
            vibrationEnabled  = settings.vibrationEnabled,
            notificationIcon  = settings.notificationIcon,
            notificationSound = settings.notificationSound
        )
    }

    // ── Alarm-screen "Taken" flag ─────────────────────────────────────────────

    fun recheckAlarmTakenFlag() = checkAlarmTakenFlag()

    /**
     * Called whenever the app comes to the foreground (RESUMED).
     * Re-runs buildDays so any NOT_TAKEN days that crossed midnight are
     * corrected to MISSED, then pushes fresh data to both widgets.
     */
    fun refreshWidgets() {
        val s    = _state.value
        val days = buildDays(s.pillType, s.startDate, s.customPillConfig)
        _state.value = s.copy(days = days)
        PillWidget.requestUpdate(appContext)
        PillWidgetMedium.requestUpdate(appContext)
        PillWidgetCalendar.requestUpdate(appContext)
    }

    private fun checkAlarmTakenFlag() {
        val takenDate = configPrefs.getLong("alarm_taken_date", -1L)
        if (takenDate == -1L) return

        val todayMs = todayMs()
        if (takenDate == todayMs) {
            val todayPill = _state.value.days.firstOrNull { normDate(it.date) == todayMs }
            if (todayPill != null && todayPill.status != PillStatus.TAKEN) {
                updateDayStatus(todayPill.day, PillStatus.TAKEN)
            }
        }
        configPrefs.edit().remove("alarm_taken_date").commit()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadState() {
        val pillType = try {
            PillType.valueOf(configPrefs.getString("pillType", PillType.TYPE_21_7.name) ?: PillType.TYPE_21_7.name)
        } catch (_: Exception) { PillType.TYPE_21_7 }

        // Use 0L as default — if never saved, we'll save it now after building days
        val savedStartMs  = configPrefs.getLong("startDate", 0L)
        val startDate     = if (savedStartMs != 0L) Date(savedStartMs) else {
            // First ever launch — use today as start date and immediately save it
            val d = normDate(Date()).let { Date(it) }
            configPrefs.edit().putLong("startDate", d.time).commit()
            d
        }

        val customConfig = configPrefs.getString("customPillConfig", null)?.let {
            try { gson.fromJson(it, CustomPillConfig::class.java) } catch (_: Exception) { null }
        } ?: CustomPillConfig()

        val settings: ReminderSettings = configPrefs.getString("settings", null)?.let {
            try { gson.fromJson(it, ReminderSettings::class.java) } catch (_: Exception) { null }
        } ?: ReminderSettings()

        val notes: List<Note> = configPrefs.getString("notes", null)?.let {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<Note>>() {}.type
                gson.fromJson(it, type)
            } catch (_: Exception) { null }
        } ?: emptyList()

        val widgetThemes: WidgetThemeSettings = configPrefs.getString("widgetThemes", null)?.let {
            try { gson.fromJson(it, WidgetThemeSettings::class.java) } catch (_: Exception) { null }
        } ?: defaultWidgetThemeSettings()

        // Build days — status comes from per-day statusPrefs, never from JSON parsing
        val days = buildDays(pillType, startDate, customConfig)

        Log.d(TAG, "loadState: pillType=$pillType startDate=$startDate days=${days.size} " +
                   "taken=${days.count { it.status == PillStatus.TAKEN }} " +
                   "missed=${days.count { it.status == PillStatus.MISSED }}")

        _state.value = AppState(
            pillType         = pillType,
            startDate        = startDate,
            days             = days,
            settings         = settings,
            customPillConfig = customConfig,
            notes            = notes,
            widgetThemes     = widgetThemes
        )

        applyAlarm(settings, days)
    }

    // ── Save config (non-status fields) ───────────────────────────────────────

    private fun saveConfig() {
        val s = _state.value
        configPrefs.edit()
            .putString("pillType",         s.pillType.name)
            .putLong("startDate",          s.startDate.time)
            .putString("customPillConfig", gson.toJson(s.customPillConfig))
            .putString("settings",         gson.toJson(s.settings))
            .putString("notes",            gson.toJson(s.notes))
            .putString("widgetThemes",     gson.toJson(s.widgetThemes))
            .commit()
        PillWidget.requestUpdate(appContext)
        PillWidgetMedium.requestUpdate(appContext)
        PillWidgetCalendar.requestUpdate(appContext)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun changePillType(pillType: PillType) {
        val newDays = buildDays(pillType, _state.value.startDate, _state.value.customPillConfig)
        _state.value = _state.value.copy(pillType = pillType, days = newDays)
        applyAlarm(_state.value.settings, newDays)
        saveConfig()
    }

    fun changeStartDate(date: Date) {
        val newDays = buildDays(_state.value.pillType, date, _state.value.customPillConfig)
        _state.value = _state.value.copy(startDate = date, days = newDays)
        applyAlarm(_state.value.settings, newDays)
        saveConfig()
    }

    fun changeCustomPillConfig(active: Int, placebo: Int, lowDose: Int) {
        val config  = CustomPillConfig(active, placebo, lowDose)
        val newDays = buildDays(PillType.CUSTOM, _state.value.startDate, config)
        _state.value = _state.value.copy(pillType = PillType.CUSTOM, customPillConfig = config, days = newDays)
        applyAlarm(_state.value.settings, newDays)
        saveConfig()
    }

    fun updateDayStatus(day: Int, status: PillStatus) {
        val updated = _state.value.days.map { d ->
            if (d.day != day) d
            else {
                val takenAt = if (status == PillStatus.TAKEN) System.currentTimeMillis() else null
                // Save this day's status immediately to statusPrefs
                saveStatus(d.date, status, takenAt)
                d.copy(status = status, takenAt = takenAt)
            }
        }
        Log.d(TAG, "updateDayStatus: day=$day status=$status")
        _state.value = _state.value.copy(days = updated)
        applyAlarm(_state.value.settings, updated)
        PillWidget.requestUpdate(appContext)
        PillWidgetMedium.requestUpdate(appContext)
        PillWidgetCalendar.requestUpdate(appContext)
    }

    fun updateSettings(settings: ReminderSettings) {
        _state.value = _state.value.copy(settings = settings)
        applyAlarm(settings, _state.value.days)
        saveConfig()
    }

    fun addNote(content: String, date: Date, time: String) {
        val note = Note(
            id        = "note_${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}",
            date      = date,
            time      = time,
            content   = content,
            createdAt = Date(),
            updatedAt = Date()
        )
        _state.value = _state.value.copy(notes = _state.value.notes + note)
        saveConfig()
    }

    fun editNote(id: String, content: String) {
        val updated = _state.value.notes.map { n ->
            if (n.id == id) n.copy(content = content, updatedAt = Date()) else n
        }
        _state.value = _state.value.copy(notes = updated)
        saveConfig()
    }

    fun deleteNote(id: String) {
        _state.value = _state.value.copy(notes = _state.value.notes.filter { it.id != id })
        saveConfig()
    }

    fun updateWidgetTheme(kind: WidgetKind, colors: WidgetThemeColors) {
        val updatedThemes = when (kind) {
            WidgetKind.SMALL -> _state.value.widgetThemes.copy(small = colors)
            WidgetKind.MEDIUM -> _state.value.widgetThemes.copy(medium = colors)
            WidgetKind.CALENDAR -> _state.value.widgetThemes.copy(calendar = colors)
        }
        _state.value = _state.value.copy(widgetThemes = updatedThemes)
        saveConfig()
    }
}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AppViewModel(context.applicationContext) as T
    }
}
