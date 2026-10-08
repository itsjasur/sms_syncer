package com.simpass.smssyncer.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object SmsAccess {
    val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS)

    fun hasPermissions(context: Context): Boolean = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
