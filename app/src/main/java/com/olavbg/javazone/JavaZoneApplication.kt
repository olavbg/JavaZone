package com.olavbg.javazone

import android.app.Application
import com.olavbg.javazone.crash.CrashHandler

class JavaZoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
