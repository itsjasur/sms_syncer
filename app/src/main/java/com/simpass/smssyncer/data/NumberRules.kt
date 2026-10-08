package com.simpass.smssyncer.data

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.security.MessageDigest

object NumberRules {
    private val phoneUtil = PhoneNumberUtil.getInstance()

    // Exact canonical matching avoids accidentally forwarding numbers with similar suffixes.
    fun canonicalize(raw: String, allowShortCode: Boolean): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.any { it !in "0123456789+ ().-" }) return null
        val compact = trimmed.filter { it in '0'..'9' || it == '+' }
        if (compact.count { it == '+' } > 1 || ('+' in compact && !compact.startsWith('+'))) return null
        if (allowShortCode && compact.all { it.isDigit() } && compact.length in 3..6) return compact
        return try {
            val parsed = phoneUtil.parse(compact, "KR")
            if (phoneUtil.isValidNumberForRegion(parsed, "KR")) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
            } else null
        } catch (_: NumberParseException) {
            null
        }
    }

    // Retain canonical identities for matching and duplicate receipts, but use domestic
    // numbers for the screen, editing, and SMS submission.
    fun domesticNumber(canonical: String): String = koreanNationalNumber(canonical)
        ?.filter { it in '0'..'9' } ?: canonical

    fun displayNumber(canonical: String): String = koreanNationalNumber(canonical) ?: canonical

    private fun koreanNationalNumber(number: String): String? = try {
        val parsed = phoneUtil.parse(number, "KR")
        if (phoneUtil.isValidNumberForRegion(parsed, "KR")) {
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
        } else null
    } catch (_: NumberParseException) {
        null
    }

    fun providerError(config: ForwardingConfig, provider: Provider): NumberValidationError? {
        if (provider.name.isBlank()) return NumberValidationError.NAME_REQUIRED
        if (canonicalize(provider.number, true) != provider.canonical) return NumberValidationError.INVALID_PROVIDER
        val others = config.providers.filterNot { it.id == provider.id }
        if (others.any { it.canonical == provider.canonical }) return NumberValidationError.DUPLICATE
        if (provider.receivers.size > 20) return NumberValidationError.RECIPIENT_LIMIT
        if (provider.receivers.any { canonicalize(it.number, false) != it.canonical }) return NumberValidationError.INVALID_RECEIVER
        if (provider.receivers.distinctBy { it.canonical }.size != provider.receivers.size) return NumberValidationError.DUPLICATE
        val providerNumbers = others.map { it.canonical }.toSet() + provider.canonical
        if (provider.receivers.any { it.canonical in providerNumbers } ||
            others.any { other -> other.receivers.any { it.canonical == provider.canonical } }) {
            return NumberValidationError.OVERLAP
        }
        return null
    }

    fun recipients(config: ForwardingConfig, canonicalSender: String?): List<PhoneEntry> {
        if (!config.enabled || !config.isReady || canonicalSender == null) return emptyList()
        val provider = config.providers.find { it.canonical == canonicalSender } ?: return emptyList()
        return provider.receivers.filter { it.canonical != canonicalSender }.distinctBy { it.canonical }
    }

    fun fingerprint(sender: String, body: String, sentAt: Long): String {
        val bytes = "$sender\u0000$sentAt\u0000$body".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
