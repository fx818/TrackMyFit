package com.trackmyfit.app

import android.app.Application
import com.trackmyfit.app.data.Repository
import com.trackmyfit.app.notify.Notifier
import com.trackmyfit.app.notify.ReminderScheduler
import com.trackmyfit.app.widget.TrackMyFitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TrackMyFitApp : Application() {
    lateinit var repository: Repository
        private set

    /** True while an activity of ours is visible. */
    @Volatile var inForeground: Boolean = false

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
        Notifier.createChannels(this)
        ReminderScheduler.schedule(this, repository.data.value.settings)
        // Keep the home-screen widget in sync with the data.
        appScope.launch {
            repository.data.debounce(500).collect { TrackMyFitWidget.updateAll(this@TrackMyFitApp, it) }
        }
        // Re-schedule reminders whenever their settings change.
        appScope.launch {
            repository.data.map { it.settings }.distinctUntilChanged().drop(1).collect { ReminderScheduler.schedule(this@TrackMyFitApp, it) }
        }
    }
}
