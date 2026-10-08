package com.simpass.smssyncer.data

import org.junit.Assert.*
import org.junit.Test

class NumberRulesTest {
    private fun entry(id: String, number: String) = PhoneEntry(id, number, number)
    private val source = entry("source", "+821012345678")
    private val recipient = entry("recipient", "+821087654321")
    private val provider = Provider(source.id, "Provider A", source.number, source.canonical, listOf(recipient))
    private val config = ForwardingConfig(true, listOf(provider))

    @Test fun koreanDomesticAndInternationalFormatsMatch() {
        assertEquals("+821012345678", NumberRules.canonicalize("010-1234-5678", true))
        assertEquals("+821012345678", NumberRules.canonicalize("01012345678", true))
        assertEquals("+821012345678", NumberRules.canonicalize("+82 10 1234 5678", true))
    }

    @Test fun shortCodesAreOnlyAllowedAsSenders() {
        assertEquals("12345", NumberRules.canonicalize("12345", true))
        assertNull(NumberRules.canonicalize("12345", false))
    }

    @Test fun rejectsNamesAndMalformedNumbers() {
        listOf("BANK", "", "+82+10", "01012345678 ext 1", "12", "++12025550123", "2025550123+").forEach {
            assertNull(it, NumberRules.canonicalize(it, true))
        }
    }

    @Test fun onlyWatchedSendersAreForwarded() {
        assertEquals(listOf(recipient), NumberRules.recipients(config, source.canonical))
        assertTrue(NumberRules.recipients(config, "+821012345679").isEmpty())
        assertTrue(NumberRules.recipients(config, "12345678").isEmpty())
        assertTrue(NumberRules.recipients(config, null).isEmpty())
    }

    @Test fun disabledOrIncompleteConfigurationDoesNotSend() {
        assertTrue(NumberRules.recipients(config.copy(enabled = false), source.canonical).isEmpty())
        assertTrue(NumberRules.recipients(config.copy(providers = emptyList()), source.canonical).isEmpty())
        assertTrue(NumberRules.recipients(config.copy(providers = listOf(provider.copy(receivers = emptyList()))), source.canonical).isEmpty())
    }

    @Test fun matchedSmsGoesToEveryUniqueRecipientAndNeverBackToSender() {
        val second = entry("second", "+8223123456")
        val brokenConfig = config.copy(providers = listOf(provider.copy(receivers = listOf(recipient, recipient.copy(id = "duplicate"), source, second))))
        assertEquals(listOf(recipient, second), NumberRules.recipients(brokenConfig, source.canonical))
    }

    @Test fun editingDoesNotTreatOwnNumberAsDuplicate() {
        assertNull(NumberRules.providerError(config, provider.copy(name = "Renamed")))
        assertEquals(NumberValidationError.DUPLICATE, NumberRules.providerError(config, provider.copy(id = "another")))
    }

    @Test fun watchedAndRecipientNumbersCannotOverlap() {
        assertEquals(NumberValidationError.OVERLAP, NumberRules.providerError(config, provider.copy(receivers = listOf(source))))
        assertEquals(NumberValidationError.OVERLAP, NumberRules.providerError(config,
            Provider("another", "Provider B", recipient.number, recipient.canonical)))
    }

    @Test fun duplicateFingerprintsPreserveLegitimateRepeatedMessages() {
        val first = NumberRules.fingerprint(source.canonical, "hello", 123L)
        assertEquals(first, NumberRules.fingerprint(source.canonical, "hello", 123L))
        assertNotEquals(first, NumberRules.fingerprint(source.canonical, "hello", 124L))
        assertNotEquals(first, NumberRules.fingerprint(recipient.canonical, "hello", 123L))
        assertNotEquals(first, NumberRules.fingerprint(source.canonical, "other", 123L))
    }

    @Test fun localMobileNumbersAreUsedForDisplayAndSending() {
        assertEquals("010-1234-5678", NumberRules.displayNumber("+821012345678"))
        assertEquals("01012345678", NumberRules.domesticNumber("+821012345678"))
        assertEquals("010-1234-5678", source.displayNumber)
        assertEquals("12345", NumberRules.displayNumber("12345"))
    }

    @Test fun koreanLandlinesKeepTheirAreaCode() {
        assertEquals("+8223123456", NumberRules.canonicalize("02-312-3456", false))
        assertEquals("02-312-3456", NumberRules.displayNumber("+8223123456"))
        assertEquals("023123456", NumberRules.domesticNumber("+8223123456"))
        assertEquals("+82312345678", NumberRules.canonicalize("031-234-5678", false))
        assertEquals("0312345678", NumberRules.domesticNumber("+82312345678"))
    }

    @Test fun koreanBusinessNumbersDoNotGetAnExtraLeadingZero() {
        val business = NumberRules.canonicalize("1588-1234", true)
        assertNotNull(business)
        assertEquals("1588-1234", NumberRules.displayNumber(business!!))
        assertEquals("15881234", NumberRules.domesticNumber(business))
    }

    @Test fun newNumbersMustBeKorean() {
        assertNull(NumberRules.canonicalize("+1 202 555 0123", false))
        assertNull(NumberRules.canonicalize("+44 20 7946 0123", true))
    }

    @Test fun existingInternationalEntriesStillMatchLocalIncomingNumbers() {
        val incoming = NumberRules.canonicalize("01012345678", true)
        assertEquals(listOf(recipient), NumberRules.recipients(config, incoming))
        assertEquals(NumberValidationError.DUPLICATE,
            NumberRules.providerError(config, provider.copy(id = "another", canonical = incoming!!)))
    }

    @Test fun eachProviderRoutesOnlyToItsOwnReceivers() {
        val sourceB = entry("sourceB", "+821055556666")
        val receiverB = entry("receiverB", "+821099998888")
        val providerB = Provider(sourceB.id, "Provider B", sourceB.number, sourceB.canonical, listOf(receiverB))
        val routing = config.copy(providers = listOf(provider, providerB))
        assertEquals(listOf(recipient), NumberRules.recipients(routing, source.canonical))
        assertEquals(listOf(receiverB), NumberRules.recipients(routing, sourceB.canonical))
    }

    @Test fun sharedReceiversAreAllowedAcrossProviders() {
        val second = Provider("second", "Provider B", "01055556666", "+821055556666", listOf(recipient))
        assertNull(NumberRules.providerError(config, second))
        assertEquals(listOf(recipient), NumberRules.recipients(config.copy(providers = listOf(provider, second)), second.canonical))
    }

    @Test fun deletingOrChangingOneProviderDoesNotRerouteAnother() {
        val second = Provider("second", "Provider B", "01055556666", "+821055556666", listOf(recipient))
        val updated = config.copy(providers = listOf(provider.copy(receivers = emptyList()), second))
        assertTrue(NumberRules.recipients(updated, source.canonical).isEmpty())
        assertEquals(listOf(recipient), NumberRules.recipients(updated, second.canonical))
        assertTrue(NumberRules.recipients(updated.copy(providers = listOf(second)), source.canonical).isEmpty())
    }

    @Test fun providerAndReceiverValidationIsScopedCorrectly() {
        assertEquals(NumberValidationError.NAME_REQUIRED, NumberRules.providerError(config, provider.copy(name = " ")))
        assertEquals(NumberValidationError.INVALID_PROVIDER, NumberRules.providerError(config, provider.copy(number = "invalid")))
        assertEquals(NumberValidationError.INVALID_RECEIVER, NumberRules.providerError(config,
            provider.copy(receivers = listOf(recipient.copy(number = "12345")))))
        assertEquals(NumberValidationError.DUPLICATE, NumberRules.providerError(config,
            provider.copy(receivers = listOf(recipient, recipient.copy(id = "duplicate")))))
        assertEquals(NumberValidationError.RECIPIENT_LIMIT, NumberRules.providerError(config,
            provider.copy(receivers = List(21) { recipient.copy(id = "receiver$it") })))
        assertNull(NumberRules.providerError(config, provider.copy(receivers = emptyList())))
    }
}
