package com.simpass.smssyncer.data

import com.simpass.smssyncer.R

data class PhoneEntry(
    val id: String,
    val number: String,
    val canonical: String,
    val label: String = "",
) {
    val displayNumber: String get() = NumberRules.displayNumber(canonical)
}

data class Provider(
    val id: String,
    val name: String,
    val number: String,
    val canonical: String,
    val receivers: List<PhoneEntry> = emptyList(),
) {
    val displayNumber: String get() = NumberRules.displayNumber(canonical)
}

enum class NumberValidationError(val messageRes: Int) {
    DUPLICATE(R.string.duplicate_number),
    OVERLAP(R.string.overlapping_number),
    RECIPIENT_LIMIT(R.string.recipient_limit),
    NAME_REQUIRED(R.string.provider_name_required),
    INVALID_PROVIDER(R.string.invalid_watched_number),
    INVALID_RECEIVER(R.string.invalid_destination_number),
}

data class ForwardingConfig(
    val enabled: Boolean = false,
    val providers: List<Provider> = emptyList(),
) {
    val isReady: Boolean get() = providers.any { it.receivers.isNotEmpty() }
}
