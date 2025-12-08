package com.example.team1application

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import android.content.Intent

fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageAccess(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        // 特定のアプリ（本アプリ）の設定画面へ誘導するためのデータ
        data = android.net.Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}