package com.example.data

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class KeyRotationPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repository = PreferencesRepository(context)
    private val slots = (1..3).map { id ->
        GeminiKeySlot(id.toString(), "Key $id", true, id, "***")
    }

    @Before fun clearPreferences() = runBlocking { context.dataStore.edit { it.clear() }; Unit }

    @Test fun roundRobinReservesEachSlotBeforeRequest() = runBlocking {
        assertEquals(listOf("1", "2", "3"), repository.reserveKeyOrder(slots).map { it.id })
        assertEquals(listOf("2", "3", "1"), repository.reserveKeyOrder(slots).map { it.id })
        assertEquals(listOf("3", "1", "2"), repository.reserveKeyOrder(slots).map { it.id })
        assertEquals("1", repository.reserveKeyOrder(slots).first().id)
    }

    @Test fun removedSlotDoesNotCorruptRotation() = runBlocking {
        repository.reserveKeyOrder(slots)
        assertEquals("2", repository.reserveKeyOrder(slots.drop(1)).first().id)
        assertEquals("3", repository.reserveKeyOrder(slots.drop(1)).first().id)
    }

    @Test fun concurrentReservationsChooseDifferentStartingSlots() = runBlocking {
        val starts = coroutineScope {
            (1..3).map { async { repository.reserveKeyOrder(slots).first().id } }.map { it.await() }
        }
        assertEquals(setOf("1", "2", "3"), starts.toSet())
    }

    @Test fun oldStickyStrategyMigratesOnceButManualChoiceRemains() = runBlocking {
        context.dataStore.edit { it[PreferencesRepository.KEY_STRATEGY] = "Sticky Success with Sequential Failover" }
        assertEquals("Round Robin", repository.keyStrategyFlow.first())
        repository.migrateKeyStrategyIfNeeded()
        assertEquals("Round Robin", repository.keyStrategyFlow.first())
        repository.setKeyStrategy("Sticky Success with Sequential Failover")
        assertEquals("Sticky Success with Sequential Failover", repository.keyStrategyFlow.first())
    }

    @Test fun oldAlwaysFirstStrategyAlsoMigrates() = runBlocking {
        context.dataStore.edit { it[PreferencesRepository.KEY_STRATEGY] = "Always Start at Key 1" }
        repository.migrateKeyStrategyIfNeeded()
        assertEquals("Round Robin", repository.keyStrategyFlow.first())
    }

    @Test fun essayNotificationDefaultsOffAndPersists() = runBlocking {
        assertFalse(repository.essayNotificationEnabledFlow.first())
        repository.setEssayNotificationEnabled(true)
        assertEquals(true, repository.essayNotificationEnabledFlow.first())
    }
}
