package it.linkspockets

import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import it.linkspockets.di.androidModule
import it.linkspockets.di.initKoin
import it.linkspockets.presentation.App

class LinksPocketsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule(this))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App { isDark ->
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.rgb(245, 245, 245), Color.rgb(27, 27, 27),
                    ) { isDark },
                )
            }
        }
    }
}
