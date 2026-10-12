package com.trackmyfit.app.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trackmyfit.app.data.ThemeMode
import com.trackmyfit.app.ui.theme.TrackMyFitTheme

/** Shown by Health Connect when the user asks why TrackMyFit wants step data. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrackMyFitTheme(ThemeMode.SYSTEM) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TrackMyFit & your step data", style = MaterialTheme.typography.headlineSmall)
                        Text("TrackMyFit only READS your step count from Health Connect to show your daily steps, step goal progress and step history on the dashboard.")
                        Text("Step data is never uploaded, shared or sold. Everything stays on this phone. TrackMyFit has no account, no servers and no internet access to your data.")
                        Text("You can revoke access at any time in Health Connect settings.")
                        Button(onClick = { finish() }) { Text("OK") }
                    }
                }
            }
        }
    }
}
