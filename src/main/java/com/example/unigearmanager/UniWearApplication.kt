package com.example.unigearmanager

import android.app.Application

class UniWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UniWearRepository.init(this)
    }
}

