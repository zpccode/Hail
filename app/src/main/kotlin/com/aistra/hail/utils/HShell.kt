package com.aistra.hail.utils

import android.os.Build
import androidx.annotation.RequiresApi

object HShell {
    private fun userArg(userId: Int) = "--user $userId"

    fun execute(command: String, root: Boolean): Pair<Int, String?> = runCatching {
        ProcessBuilder(if (root) "su" else "sh").redirectErrorStream(true).start().run {
            outputStream.use { it.write(command.toByteArray()) }
            waitFor() to inputStream.bufferedReader().use { it.readText() }.also { destroy() }
        }
    }.getOrElse { 1 to it.stackTraceToString() }

    private fun execSU(command: String) = execute(command, true)

    val checkSU get() = execSU("whoami").first == 0

    val lockScreen get() = execSU("input keyevent KEYCODE_POWER").first == 0

    fun forceStopApp(packageName: String, userId: Int = HPackages.myUserId): Boolean = execSU("am force-stop ${userArg(userId)} $packageName").first == 0

    fun setAppDisabled(packageName: String, disabled: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (disabled) "disable" else "enable"} ${userArg(userId)} $packageName").first == 0

    fun setAppHidden(packageName: String, hidden: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (hidden) "hide" else "unhide"} ${userArg(userId)} $packageName").first == 0

    fun setAppSuspended(packageName: String, suspended: Boolean, userId: Int = HPackages.myUserId): Boolean =
        execSU("pm ${if (suspended) "suspend" else "unsuspend"} ${userArg(userId)} $packageName").first == 0

    fun uninstallApp(packageName: String, userId: Int = HPackages.myUserId) = execSU(
        "pm ${if (HPackages.canUninstallNormally(packageName)) "uninstall" else "uninstall ${userArg(userId)}"} $packageName"
    ).first == 0

    fun reinstallApp(packageName: String, userId: Int = HPackages.myUserId) = execSU("pm install-existing ${userArg(userId)} $packageName").first == 0

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean, userId: Int = HPackages.myUserId) = execSU(
        "appops set ${userArg(userId)} $packageName RUN_ANY_IN_BACKGROUND ${if (restricted) "ignore" else "allow"}"
    ).first == 0

    fun autoClickAddShortcut() {
        val buttons = listOf("添加", "确定", "始终允许", "允许", "Add", "Confirm", "Allow")
        val cmd = StringBuilder("uiautomator dump /sdcard/view.xml > /dev/null")
        buttons.forEach { text ->
            cmd.append(" && if grep -q \"text=\\\"$text\\\"\" /sdcard/view.xml; then ")
            cmd.append("coords=\$(sed -n \"s/.*text=\\\"$text\\\".*bounds=\\\"\\[\\([0-9]*\\),\\([0-9]*\\)\\]\\[\\([0-9]*\\),\\([0-9]*\\)\\].*/\\1 \\2 \\3 \\4/p\" /sdcard/view.xml | head -1); ")
            cmd.append("if [ ! -z \"\$coords\" ]; then set -- \$coords; ")
            cmd.append("input tap \$(((\$1+\$3)/2)) \$(((\$2+\$4)/2)); fi; fi")
        }
        execSU(cmd.toString())
    }
}