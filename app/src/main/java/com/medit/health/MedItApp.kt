package com.medit.health

import android.app.Application
import com.medit.health.data.local.MedItDatabaseHelper
import com.medit.health.data.repository.MedItRepository

class MedItApp : Application() {

    lateinit var repository: MedItRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val dbHelper = MedItDatabaseHelper.getInstance(this)
        repository = MedItRepository(dbHelper, this)
    }
}
