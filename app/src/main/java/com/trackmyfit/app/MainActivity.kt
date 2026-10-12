package com.trackmyfit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.ui.AppRoot
import com.trackmyfit.app.ui.MainViewModel
import com.trackmyfit.app.ui.theme.TrackMyFitTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) vm.requestNav(intent?.getStringExtra("tab"))
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            TrackMyFitTheme(themeMode = data.settings.theme) {
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("tab")?.let { vm.requestNav(it) }
    }

    override fun onStart() {
        super.onStart()
        (application as TrackMyFitApp).inForeground = true
    }

    override fun onResume() {
        super.onResume()
        vm.refreshSteps()
    }

    override fun onStop() {
        super.onStop()
        (application as TrackMyFitApp).inForeground = false
    }
}
