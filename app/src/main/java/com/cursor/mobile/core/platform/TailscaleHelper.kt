package com.cursor.mobile.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

object TailscaleHelper {
    const val PACKAGE_NAME = "com.tailscale.ipn"

    private const val PLAY_STORE_URI = "market://details?id=$PACKAGE_NAME"
    private const val PLAY_STORE_WEB_URI =
        "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"

    enum class Result {
        OPENED_APP,
        OPENED_PLAY_STORE,
        OPENED_VPN_SETTINGS
    }

    fun isInstalled(context: Context): Boolean {
        return context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) != null
    }

    fun openTailscale(context: Context): Result {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return Result.OPENED_APP
        }

        Toast.makeText(
            context,
            "Tailscale not installed. Opening Play Store…",
            Toast.LENGTH_LONG
        ).show()

        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (playStoreIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(playStoreIntent)
            return Result.OPENED_PLAY_STORE
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URI)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (webIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(webIntent)
            return Result.OPENED_PLAY_STORE
        }

        Toast.makeText(
            context,
            "Opening VPN settings. Enable Tailscale to connect.",
            Toast.LENGTH_LONG
        ).show()
        val vpnIntent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(vpnIntent)
        return Result.OPENED_VPN_SETTINGS
    }
}
