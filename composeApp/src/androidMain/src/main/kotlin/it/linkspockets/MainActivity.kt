package it.linkspockets

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
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
        setContent { App() }
    }
}
