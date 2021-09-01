package com.hossam.pick

import android.app.Application
import android.content.Context

class MyApp : Application() {



    companion object {
        private val app by lazy { MyApp() }
        fun getContext(): Context = app.applicationContext
    }


}