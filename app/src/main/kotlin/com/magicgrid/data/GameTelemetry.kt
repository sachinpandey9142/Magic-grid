package com.magicgrid.data

import android.content.Context
import android.util.Log

/**
 * Lightweight local telemetry helper for balancing and UX tuning.
 * Stores event counts on-device and emits debug logs.
 */
object GameTelemetry {
    private const val PREFS_NAME = "MagicGridTelemetry"
    private const val EVENT_COUNT_PREFIX = "event_count_"
    private const val EVENT_LAST_TS_PREFIX = "event_last_ts_"
    private const val TAG = "GameTelemetry"

    fun track(context: Context, event: String, levelId: Int? = null, detail: String? = null) {
        if (event.isBlank()) return

        val safeEvent = event.trim().lowercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val countKey = EVENT_COUNT_PREFIX + safeEvent
        val lastTsKey = EVENT_LAST_TS_PREFIX + safeEvent

        val nextCount = prefs.getInt(countKey, 0) + 1
        val now = System.currentTimeMillis()

        prefs.edit()
            .putInt(countKey, nextCount)
            .putLong(lastTsKey, now)
            .apply()

        Log.d(
            TAG,
            "event=$safeEvent count=$nextCount level=${levelId ?: -1} detail=${detail ?: "-"}"
        )
    }

    fun getEventCount(context: Context, event: String): Int {
        if (event.isBlank()) return 0
        val safeEvent = event.trim().lowercase()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(EVENT_COUNT_PREFIX + safeEvent, 0)
    }
}
