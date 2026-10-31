package com.example.link

import android.app.Application
import com.example.link.data.ConnectionStore
import com.example.link.data.GuardianRepository

class GuardianApplication : Application() {
    lateinit var connectionStore: ConnectionStore
        private set
    lateinit var repository: GuardianRepository
        private set

    override fun onCreate() {
        super.onCreate()
        connectionStore = ConnectionStore(this)
        repository = GuardianRepository(connectionStore)
    }
}