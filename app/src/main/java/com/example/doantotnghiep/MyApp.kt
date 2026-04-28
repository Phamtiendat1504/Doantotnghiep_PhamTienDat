package com.example.doantotnghiep

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.doantotnghiep.Utils.PresenceManager

class MyApp : Application(), Application.ActivityLifecycleCallbacks {

    private var startedActivityCount = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        if (startedActivityCount == 1) {
            // App is in foreground
            PresenceManager.goOnline()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount--
        if (startedActivityCount == 0) {
            // App is in background
            PresenceManager.goOffline()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}