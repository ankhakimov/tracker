package com.example.gpstracker

import android.app.Application
import com.example.gpstracker.db.MainDB

class MainApp: Application() {
    val database by lazy {
        MainDB.getDatabase(this)
    }
}