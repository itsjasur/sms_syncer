package com.simpass.smssyncer.sms

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.simpass.smssyncer.R
import com.simpass.smssyncer.data.ForwardingStore
import com.simpass.smssyncer.data.PhoneEntry
import com.simpass.smssyncer.data.NumberRules

class SmsForwarder(private val context: Context, private val store: ForwardingStore) {
    @SuppressLint("MissingPermission") // Checked before receipt processing and again before each submission.
    fun forward(sender: String, body: String, recipients: List<PhoneEntry>) {
        val providerName = store.config().providers.find { it.canonical == sender }?.name ?: return
        val subscription = SmsManager.getDefaultSmsSubscriptionId()
        // Equivalent to isValidSubscriptionId, which is only public from API 29.
        // Int.MAX_VALUE represents the unresolved default subscription, not a SIM.
        val manager = if (subscription in 0 until Int.MAX_VALUE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscription)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subscription)
            }
        } else null

        if (manager == null) return
        recipients.forEach { recipient ->
            try {
                val latest = store.config()
                if (!latest.enabled || !SmsAccess.hasPermissions(context) ||
                    NumberRules.recipients(latest, sender).none { it.id == recipient.id && it.canonical == recipient.canonical }) {
                    return@forEach
                }
                // Use the matched provider's name and preserve the original body.
                val text = context.getString(R.string.forwarded_message, providerName, body)
                val parts = manager.divideMessage(text)
                val destination = NumberRules.domesticNumber(recipient.canonical)
                if (parts.size == 1) {
                    manager.sendTextMessage(destination, null, parts.first(), null, null)
                } else {
                    manager.sendMultipartTextMessage(destination, null, parts, null, null)
                }
            } catch (_: Exception) {
                // A failed receiver must not block the other receivers. Keep the
                // diagnostic generic: no phone numbers, message text, or send records.
                Log.w("SmsSyncer", "SMS submission failed for a configured receiver.")
            }
        }
    }
}
