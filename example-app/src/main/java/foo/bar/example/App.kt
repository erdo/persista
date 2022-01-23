package foo.bar.example

import android.app.Application
import co.early.fore.kt.core.delegate.DebugDelegateDefault
import co.early.fore.kt.core.delegate.ForeDelegateHolder

/**
 * Copyright © 2015-2021 early.co. All rights reserved.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG){
            ForeDelegateHolder.setDelegate(DebugDelegateDefault("persista_"))
        }

        inst = this

        OG.setApplication(this)
        OG.init()
    }

    companion object {
        lateinit var inst: App private set
    }
}
