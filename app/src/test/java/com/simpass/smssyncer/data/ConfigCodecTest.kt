package com.simpass.smssyncer.data

import org.junit.Assert.*
import org.junit.Test

class ConfigCodecTest {
    private val legacy = """
        {"enabled":true,"watched":[
          {"id":"a","number":"+821012345678","canonical":"+821012345678","label":"Provider A"},
          {"id":"b","number":"01055556666","canonical":"+821055556666","label":""}],
         "destinations":[{"id":"r","number":"01087654321","canonical":"+821087654321","label":"Receiver"}]}
    """.trimIndent()

    @Test fun migrationPreservesNamesIdsEnablementAndPreviousRouting() {
        assertTrue(ConfigCodec.isLegacy(legacy))
        val config = ConfigCodec.decode(legacy)
        assertTrue(config.enabled)
        assertEquals(listOf("a", "b"), config.providers.map { it.id })
        assertEquals("Provider A", config.providers[0].name)
        assertEquals("010-5555-6666", config.providers[1].name)
        assertEquals("01012345678", config.providers[0].number)
        config.providers.forEach {
            assertEquals("r", it.receivers.single().id)
            assertEquals("Receiver", it.receivers.single().label)
            assertEquals(it.receivers, NumberRules.recipients(config, it.canonical))
        }
    }

    @Test fun migratedReceiversCanBeChangedIndependently() {
        val migrated = ConfigCodec.decode(legacy)
        val changed = migrated.copy(providers = migrated.providers.mapIndexed { index, provider ->
            if (index == 0) provider.copy(receivers = emptyList()) else provider
        })
        val persisted = ConfigCodec.decode(ConfigCodec.encode(changed))
        assertTrue(NumberRules.recipients(persisted, "+821012345678").isEmpty())
        assertEquals("r", NumberRules.recipients(persisted, "+821055556666").single().id)
    }

    @Test fun newSchemaRoundTripPreservesProvidersAndDoesNotMigrateAgain() {
        val config = ConfigCodec.decode(legacy)
        val raw = ConfigCodec.encode(config)
        assertFalse(ConfigCodec.isLegacy(raw))
        assertEquals(config, ConfigCodec.decode(raw))
    }

    @Test fun freshOrEmptyConfigurationDoesNotCreatePhantomProviders() {
        assertEquals(ForwardingConfig(), ConfigCodec.decode("{}"))
        assertFalse(ConfigCodec.isLegacy("{}"))
        assertTrue(ConfigCodec.decode("""{"enabled":false,"watched":[],"destinations":[]}""").providers.isEmpty())
    }
}
