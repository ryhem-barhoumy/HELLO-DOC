package tn.rnu.isetr.hellodoc.network

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import tn.rnu.isetr.hellodoc.BuildConfig
import java.net.URI

object LocalBackendAccess {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
    fun needsPermission(): Boolean {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37) return false
        val uri = URI(BuildConfig.API_BASE_URL)
        val host = uri.host.orEmpty().removePrefix("[").removeSuffix("]")
        if (host in setOf("localhost", "127.0.0.1", "::1")) return false
        return uri.scheme == "http" || host.startsWith("10.") || host.startsWith("192.168.") ||
            Regex("172\\.(1[6-9]|2[0-9]|3[01])\\..*").matches(host) || host.endsWith(".local") || host.startsWith("fd") || host.startsWith("fe80:")
    }
    fun isAllowed(context: Context): Boolean = !needsPermission() ||
        ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED
}
