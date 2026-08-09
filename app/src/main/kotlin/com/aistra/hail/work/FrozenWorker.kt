package com.aistra.hail.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages

class FrozenWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        inputData.getString(HailData.KEY_PACKAGE)?.let { pkg ->
            val userId = inputData.getInt(HailData.KEY_USER_ID, HPackages.myUserId)
            AppManager.setAppFrozen(pkg, inputData.getBoolean(HailData.KEY_FROZEN, true), userId)
            return Result.success()
        }
        return Result.failure()
    }
}