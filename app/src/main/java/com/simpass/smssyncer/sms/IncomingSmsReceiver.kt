package com.simpass.smssyncer.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.simpass.smssyncer.data.ForwardingStore
import com.simpass.smssyncer.data.NumberRules
import java.util.concurrent.Executors

class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION || !SmsAccess.hasPermissions(context)) return
        val messages = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()
            ?.takeIf { it.isNotEmpty() } ?: return
        val sender = messages.first().originatingAddress ?: return
        val canonical = NumberRules.canonicalize(sender, allowShortCode = true) ?: return
        // Only combine segments belonging to the same sender.
        if (messages.any { NumberRules.canonicalize(it.originatingAddress ?: "", true) != canonical }) return
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        if (body.isBlank()) return
        val sentAt = messages.first().timestampMillis
        val pending = goAsync()
        executor.execute {
            try {
                val store = ForwardingStore(context)
                val recipients = NumberRules.recipients(store.config(), canonical)
                if (recipients.isEmpty() || !SmsAccess.hasPermissions(context)) return@execute
                if (!store.claimMessage(NumberRules.fingerprint(canonical, body, sentAt), System.currentTimeMillis())) return@execute
                SmsForwarder(context, store).forward(canonical, body, recipients)
            } catch (_: Exception) {
                // Do not crash the system receiver or expose message bodies in logs.
                // Each recipient's submission failure is handled by SmsForwarder.
            } finally {
                pending.finish()
            }
        }
    }

    companion object { private val executor = Executors.newSingleThreadExecutor() }
}
