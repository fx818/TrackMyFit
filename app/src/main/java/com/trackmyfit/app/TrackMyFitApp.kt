package com.trackmyfit.app

import android.app.Application
import com.trackmyfit.app.data.Repository

class TrackMyFitApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(this)
    }
}
