package com.trackmyfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trackmyfit.app.ui.AppRoot
import com.trackmyfit.app.ui.MainViewModel
import com.trackmyfit.app.ui.theme.TrackMyFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val data by vm.data.collectAsStateWithLifecycle()
            TrackMyFitTheme(themeMode = data.settings.theme) {
                AppRoot(vm)
            }
        }
    }
}
