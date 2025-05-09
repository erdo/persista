package foo.bar.example

import android.app.Application
import co.early.fore.core.delegate.DebugDelegateDefault
import co.early.fore.core.delegate.Fore

/**
 * Copyright © 2015-2021 early.co. All rights reserved.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG){
            Fore.setDelegate(DebugDelegateDefault("persista_"))
        }

        inst = this

        OG.setApplication(this)
        OG.init()
    }

    companion object {
        lateinit var inst: App private set
    }
}
