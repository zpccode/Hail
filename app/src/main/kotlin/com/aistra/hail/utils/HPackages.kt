package com.aistra.hail.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.aistra.hail.HailApp.Companion.app
import org.lsposed.hiddenapibypass.HiddenApiBypass

object HPackages {
    val myUserId get() = runCatching {
        val userHandle = android.os.Process.myUserHandle()
        if (HTarget.P) {
            HiddenApiBypass.invoke(UserHandle::class.java, userHandle, "getIdentifier") as Int
        } else {
            userHandle.javaClass.getMethod("getIdentifier").invoke(userHandle) as Int
        }
    }.getOrDefault(0)

    fun getUserHandle(userId: Int): UserHandle = runCatching {
        if (HTarget.P) {
            HiddenApiBypass.invoke(UserHandle::class.java, null, "of", userId) as UserHandle
        } else {
            UserHandle::class.java.getConstructor(Int::class.javaPrimitiveType).newInstance(userId)
        }
    }.getOrElse { android.os.Process.myUserHandle() }

    fun packageUri(packageName: String) = "package:$packageName"

    @RequiresApi(Build.VERSION_CODES.N)
    fun packageUid(packageName: String, userId: Int = myUserId): Int = runCatching {
        val pm = app.packageManager
        if (userId == myUserId) {
            if (HTarget.T) pm.getPackageUid(
                packageName, PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong())
            ) else pm.getPackageUid(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } else {
            if (HTarget.P) {
                HiddenApiBypass.invoke(
                    pm::class.java, pm, "getPackageUid", packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, userId
                ) as Int
            } else {
                pm.javaClass.getMethod(
                    "getPackageUid", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                ).invoke(pm, packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES, userId) as Int
            }
        }
    }.getOrDefault(0)

    fun getInstalledApplications(
        showClones: Boolean = false,
        flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192
    ): List<ApplicationInfo> {
        val pm = app.packageManager
        if (!showClones) {
            return if (HTarget.T) pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            else pm.getInstalledApplications(flags)
        }

        val um = app.getSystemService<UserManager>()!!
        val users = um.userProfiles
        val allApps = mutableListOf<ApplicationInfo>()
        for (user in users) {
            runCatching {
                val id = if (HTarget.P) {
                    HiddenApiBypass.invoke(UserHandle::class.java, user, "getIdentifier") as Int
                } else {
                    user.javaClass.getMethod("getIdentifier").invoke(user) as Int
                }
                val apps = if (HTarget.P) {
                    @Suppress("UNCHECKED_CAST")
                    HiddenApiBypass.invoke(
                        pm::class.java, pm, "getInstalledApplicationsAsUser", flags, id
                    ) as List<ApplicationInfo>
                } else {
                    @Suppress("UNCHECKED_CAST")
                    pm.javaClass.getMethod(
                        "getInstalledApplicationsAsUser", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                    ).invoke(pm, flags, id) as List<ApplicationInfo>
                }
                allApps.addAll(apps)
            }
        }
        return allApps
    }

    fun getUnhiddenPackageInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192,
        userId: Int = myUserId
    ) = runCatching {
        if (userId == myUserId) {
            if (HTarget.T) app.packageManager.getPackageInfo(
                packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
            )
            else app.packageManager.getPackageInfo(packageName, flags)
        } else {
            if (HTarget.P) {
                @Suppress("UNCHECKED_CAST")
                HiddenApiBypass.invoke(
                    app.packageManager::class.java, app.packageManager, "getPackageInfoAsUser", packageName, flags, userId
                ) as android.content.pm.PackageInfo
            } else {
                @Suppress("UNCHECKED_CAST")
                app.packageManager.javaClass.getMethod(
                    "getPackageInfoAsUser", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                ).invoke(app.packageManager, packageName, flags, userId) as android.content.pm.PackageInfo
            }
        }
    }.getOrNull()

    fun getApplicationInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192,
        userId: Int = myUserId
    ) = runCatching {
        if (userId == myUserId) {
            if (HTarget.T) app.packageManager.getApplicationInfo(
                packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong())
            )
            else app.packageManager.getApplicationInfo(packageName, flags)
        } else {
            if (HTarget.P) {
                @Suppress("UNCHECKED_CAST")
                HiddenApiBypass.invoke(
                    app.packageManager::class.java, app.packageManager, "getApplicationInfoAsUser", packageName, flags, userId
                ) as ApplicationInfo
            } else {
                @Suppress("UNCHECKED_CAST")
                app.packageManager.javaClass.getMethod(
                    "getApplicationInfoAsUser", String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
                ).invoke(app.packageManager, packageName, flags, userId) as ApplicationInfo
            }
        }
    }.getOrNull()

    fun isAppDisabled(packageName: String, userId: Int = myUserId): Boolean =
        getApplicationInfoOrNull(packageName, userId = userId)?.enabled?.not() ?: false

    fun isAppDisabled(info: ApplicationInfo): Boolean = !info.enabled

    fun isAppHidden(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let { isAppHidden(it) } ?: false
    fun isAppHidden(info: ApplicationInfo): Boolean = (ApplicationInfo::class.java.getField("privateFlags").get(info) as Int) and 1 == 1

    fun isAppStopped(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let { isAppStopped(it) } ?: false
    fun isAppStopped(info: ApplicationInfo): Boolean = info.flags and ApplicationInfo.FLAG_STOPPED == ApplicationInfo.FLAG_STOPPED

    fun isAppSuspended(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let { isAppSuspended(it) } ?: false
    fun isAppSuspended(info: ApplicationInfo): Boolean = when {
        HTarget.N -> info.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED
        else -> false
    }

    fun isAppUninstalled(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let { isAppUninstalled(it) } ?: true
    fun isAppUninstalled(info: ApplicationInfo): Boolean = info.flags and ApplicationInfo.FLAG_INSTALLED != ApplicationInfo.FLAG_INSTALLED

    fun isPrivilegedApp(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let { isPrivilegedApp(it) } ?: false
    fun isPrivilegedApp(info: ApplicationInfo): Boolean = (ApplicationInfo::class.java.getField("privateFlags").get(info) as Int) and 8 == 8


    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false

    fun forceStopApp(packageName: String): Boolean = runCatching {
        app.getSystemService<ActivityManager>()!!.let {
            if (HTarget.P) HiddenApiBypass.invoke(it::class.java, it, "forceStopPackage", packageName)
            else it::class.java.getMethod("forceStopPackage", String::class.java).invoke(it, packageName)
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }

    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean {
        getApplicationInfoOrNull(packageName) ?: return false
        if (disabled) forceStopApp(packageName)
        runCatching {
            val newState = when {
                !disabled -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            app.packageManager.setApplicationEnabledSetting(packageName, newState, 0)
        }.onFailure {
            HLog.e(it)
        }
        return isAppDisabled(packageName) == disabled
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun setAppRestricted(packageName: String, restricted: Boolean): Boolean = runCatching {
        app.getSystemService<AppOpsManager>()!!.let {
            HiddenApiBypass.invoke(
                it::class.java,
                it,
                "setMode",
                "android:run_any_in_background",
                packageUid(packageName),
                packageName,
                if (restricted) AppOpsManager.MODE_IGNORED else AppOpsManager.MODE_ALLOWED
            )
        }
        true
    }.getOrElse {
        HLog.e(it)
        false
    }
}