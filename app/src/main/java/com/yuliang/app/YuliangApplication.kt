package com.yuliang.app

import android.app.Application
import com.yuliang.app.data.database.YuliangDatabase
import com.yuliang.app.data.repository.*
import com.yuliang.app.data.settings.UserSettingsStore
import com.yuliang.app.data.transfer.DataTransferService

class YuliangApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val database = YuliangDatabase.create(application)
    val planRepository: PlanRepository = RoomPlanRepository(database.monthlyPlanDao())
    val ledgerRepository: LedgerRepository = RoomLedgerRepository(database.ledgerDao())
    val categoryRepository: CategoryRepository = RoomCategoryRepository(database.categoryDao())
    val fixedExpenseRepository: FixedExpenseRepository = RoomFixedExpenseRepository(database)
    val settings = UserSettingsStore(application)
    val dataTransfer = DataTransferService(database, settings)
}
