package com.simpass.smssyncer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.simpass.smssyncer.data.ForwardingStore
import com.simpass.smssyncer.sms.SmsAccess
import com.simpass.smssyncer.ui.SmsSyncerApp
import com.simpass.smssyncer.ui.SmsSyncerTheme

class MainActivity : ComponentActivity() {
    private val permissionsGranted = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = ForwardingStore(this)
        setContent {
            SmsSyncerTheme {
                SmsSyncerApp(
                    store = store,
                    permissionsGranted = permissionsGranted.value,
                    refreshPermissions = { permissionsGranted.value = SmsAccess.hasPermissions(this) },
                    openSettings = {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionsGranted.value = SmsAccess.hasPermissions(this)
    }
}
