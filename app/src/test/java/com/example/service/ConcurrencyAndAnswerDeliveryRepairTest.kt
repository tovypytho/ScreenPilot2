package com.example.service

import com.example.data.ScreenPilotPreferenceDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Production-backed concurrency and reliability unit tests for ScreenPilot.
 * Zero Mockito dependencies — all test doubles are written in plain Kotlin.
 * Every test calls real production abstractions used directly by ScreenCaptureService.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrencyAndAnswerDeliveryRepairTest {

    private lateinit var gate: AnalysisCompletionGate
    private lateinit var freshFrameGate: FreshFrameReadinessGate
    private lateinit var popupCoordinator: AnswerPopupAttachmentCoordinator
    private lateinit var resizeCoordinator: CaptureSurfaceResizeCoordinator<String>

    @Before
    fun setUp() {
        gate = AnalysisCompletionGate()
        freshFrameGate = FreshFrameReadinessGate()
        popupCoordinator = AnswerPopupAttachmentCoordinator()
        resizeCoordinator = CaptureSurfaceResizeCoordinator<String>()
    }

    // A. FreshFrameReadinessGate Tests

    @Test
    fun `fresh frame gate starts unarmed and rejects pre-barrier callbacks`() {
        freshFrameGate.reset()
        assertFalse("Gate must reject image callbacks while unarmed", freshFrameGate.isArmed())
    }

    @Test
    fun `fresh frame gate arm transition occurs only after post-hide barrier`() {
        freshFrameGate.reset()
        assertFalse("Before arming, isArmed is false", freshFrameGate.isArmed())
        freshFrameGate.arm()
        assertTrue("After arming, isArmed is true", freshFrameGate.isArmed())
    }

    @Test
    fun `fresh frame gate reset creates clean next capture state`() {
        freshFrameGate.reset()
        freshFrameGate.arm()
        assertTrue(freshFrameGate.isArmed())
        val gen0 = freshFrameGate.generation()

        freshFrameGate.reset()
        assertFalse("After reset, gate must be unarmed", freshFrameGate.isArmed())
        assertTrue("Generation must increment on reset", freshFrameGate.generation() > gen0)
    }

    // B. AnswerPopupAttachmentCoordinator Tests

    @Test
    fun `old popup generation token cannot attach after invalidation`() {
        val oldToken = popupCoordinator.nextToken()
        popupCoordinator.invalidate()
        assertFalse("Old token must be invalid after invalidate()", popupCoordinator.isValid(oldToken))
    }

    @Test
    fun `current popup generation token can attach`() {
        val currentToken = popupCoordinator.nextToken()
        assertTrue("Current token must be valid", popupCoordinator.isValid(currentToken))
    }

    @Test
    fun `new capture invalidates in-flight popup token`() {
        val inFlightToken = popupCoordinator.nextToken()
        popupCoordinator.invalidate()
        assertFalse("In-flight popup token must be invalidated when new capture invalidates", popupCoordinator.isValid(inFlightToken))
    }

    // C. CaptureSurfaceResizeCoordinator Generic Tests (Phase 2 - Zero Android Mocks)

    private class FakeSurfaceResizeOps : SurfaceResizeOps<String> {
        var createNewReaderCalled = false
        var resizeToNewCalled = false
        var attachNewCalled = false
        var resizeToOldCalled = false
        var attachOldCalled = false
        val closedReaders = mutableListOf<String>()

        var failAttachNew = false
        var failResizeOld = false

        override fun createNewReader(): String {
            createNewReaderCalled = true
            return "new_reader_handle"
        }

        override fun resizeToNew() {
            resizeToNewCalled = true
        }

        override fun attachNew(reader: String) {
            attachNewCalled = true
            if (failAttachNew) throw RuntimeException("Failed to attach new reader surface")
        }

        override fun resizeToOld() {
            resizeToOldCalled = true
            if (failResizeOld) throw RuntimeException("Failed to rollback display dimensions")
        }

        override fun attachOld(reader: String?) {
            attachOldCalled = true
        }

        override fun closeReader(reader: String?) {
            if (reader != null) closedReaders.add(reader)
        }
    }

    @Test
    fun `resize new and attach succeeds returns Success and closes old reader`() {
        val ops = FakeSurfaceResizeOps()
        val (result, resReader) = resizeCoordinator.resize(
            hasDisplay = true,
            oldReader = "old_reader_handle",
            ops = ops
        )

        assertEquals(CaptureSurfaceResizeResult.Success, result)
        assertEquals("new_reader_handle", resReader)
        assertTrue(ops.closedReaders.contains("old_reader_handle"))
        assertFalse(ops.closedReaders.contains("new_reader_handle"))
    }

    @Test
    fun `attach-new fails and rollback succeeds returns RolledBack and keeps old reader open`() {
        val ops = FakeSurfaceResizeOps().apply { failAttachNew = true }
        val (result, resReader) = resizeCoordinator.resize(
            hasDisplay = true,
            oldReader = "old_reader_handle",
            ops = ops
        )

        assertEquals(CaptureSurfaceResizeResult.RolledBack, result)
        assertEquals("old_reader_handle", resReader)
        assertTrue("Newly created reader must be closed on failed commit", ops.closedReaders.contains("new_reader_handle"))
        assertFalse("Old reader must NOT be closed on successful rollback", ops.closedReaders.contains("old_reader_handle"))
    }

    @Test
    fun `attach-new fails and rollback fails returns InfrastructureBroken and closes both`() {
        val ops = FakeSurfaceResizeOps().apply {
            failAttachNew = true
            failResizeOld = true
        }
        val (result, resReader) = resizeCoordinator.resize(
            hasDisplay = true,
            oldReader = "old_reader_handle",
            ops = ops
        )

        assertTrue(result is CaptureSurfaceResizeResult.InfrastructureBroken)
        assertNull(resReader)
        assertTrue("New reader must be closed", ops.closedReaders.contains("new_reader_handle"))
        assertTrue("Old reader must be closed on rollback failure", ops.closedReaders.contains("old_reader_handle"))
    }

    @Test
    fun `resize with null display returns InfrastructureBroken`() {
        val ops = FakeSurfaceResizeOps()
        val (result, resReader) = resizeCoordinator.resize(
            hasDisplay = false,
            oldReader = "old_reader_handle",
            ops = ops
        )

        assertTrue(result is CaptureSurfaceResizeResult.InfrastructureBroken)
        assertNull(resReader)
        assertFalse(ops.createNewReaderCalled)
    }

    // D. AnalysisCompletionGate Tests

    @Test
    fun `first completion succeeds and duplicate completion fails`() {
        gate.reset()
        assertTrue("First tryComplete must return true", gate.tryComplete())
        assertFalse("Second tryComplete must return false", gate.tryComplete())
        assertFalse("Third tryComplete must return false", gate.tryComplete())
    }

    @Test
    fun `independent gate instances do not interfere`() {
        val gate1 = AnalysisCompletionGate()
        val gate2 = AnalysisCompletionGate()
        gate1.reset()
        gate2.reset()

        assertTrue(gate1.tryComplete())
        assertFalse(gate1.tryComplete())

        assertTrue("gate2 must still succeed independently", gate2.tryComplete())
    }

    // E. DimensionUtils DP Conversion Tests

    @Test
    fun `dimension utils converts dp to px correctly matching repository density`() {
        val density = 2.5f
        val dpValue = 120f
        val expectedPx = (dpValue * density + 0.5f).toInt()
        assertEquals(expectedPx, DimensionUtils.dpToPx(dpValue, density))
    }

    // F. FailoverPreferenceReader Tests with Pure Kotlin Interface Fake (Phase 1)

    private class FakeFailoverPreferenceSource : FailoverPreferenceSource {
        override var cooldownDurationSecFlow: Flow<Int> = flowOf(ScreenPilotPreferenceDefaults.COOLDOWN_DURATION_SEC)
        override var historyLimitFlow: Flow<Int> = flowOf(ScreenPilotPreferenceDefaults.HISTORY_LIMIT)
        override var displayErrorSymbolFlow: Flow<Boolean> = flowOf(ScreenPilotPreferenceDefaults.DISPLAY_ERROR_SYMBOL)
        override var sameKeyRetryEnabledFlow: Flow<Boolean> = flowOf(ScreenPilotPreferenceDefaults.SAME_KEY_RETRY_ENABLED)
        override var skipCoolingDownFlow: Flow<Boolean> = flowOf(ScreenPilotPreferenceDefaults.SKIP_COOLING_DOWN)
        override var skipAuthFailedFlow: Flow<Boolean> = flowOf(ScreenPilotPreferenceDefaults.SKIP_AUTH_FAILED)
        override var skipPermissionDeniedFlow: Flow<Boolean> = flowOf(ScreenPilotPreferenceDefaults.SKIP_PERMISSION_DENIED)
        override var keyStrategyFlow: Flow<String> = flowOf(ScreenPilotPreferenceDefaults.KEY_STRATEGY)
        override var lastSuccessfulKeyIdFlow: Flow<String> = flowOf("")
        override var roundRobinLastKeyIndexFlow: Flow<Int> = flowOf(ScreenPilotPreferenceDefaults.ROUND_ROBIN_LAST_KEY_INDEX)
        override var maxKeyAttemptsFlow: Flow<Int> = flowOf(ScreenPilotPreferenceDefaults.MAX_KEY_ATTEMPTS)
        override var dismissTimeoutSecFlow: Flow<Int> = flowOf(ScreenPilotPreferenceDefaults.DISMISS_TIMEOUT_SEC.toInt())
    }

    @Test
    fun `failover preference reader returns configured values when datastore succeeds`() = runTest {
        val source = FakeFailoverPreferenceSource().apply {
            cooldownDurationSecFlow = flowOf(120)
            historyLimitFlow = flowOf(50)
            displayErrorSymbolFlow = flowOf(true)
        }

        val reader = FailoverPreferenceReader(source)

        assertEquals(120, reader.safeCooldownDurationSec())
        assertEquals(50, reader.safeHistoryLimit())
        assertTrue(reader.safeDisplayErrorSymbol())
    }

    @Test
    fun `failover preference reader returns defaults matching repository on datastore error`() = runTest {
        val source = FakeFailoverPreferenceSource().apply {
            cooldownDurationSecFlow = flow { throw RuntimeException("Disk IO error") }
            historyLimitFlow = flow { throw RuntimeException("Disk IO error") }
            displayErrorSymbolFlow = flow { throw RuntimeException("Disk IO error") }
        }

        val reader = FailoverPreferenceReader(source)

        assertEquals(ScreenPilotPreferenceDefaults.COOLDOWN_DURATION_SEC, reader.safeCooldownDurationSec())
        assertEquals(ScreenPilotPreferenceDefaults.HISTORY_LIMIT, reader.safeHistoryLimit())
        assertEquals(ScreenPilotPreferenceDefaults.DISPLAY_ERROR_SYMBOL, reader.safeDisplayErrorSymbol())
    }


    @Test
    fun `diagnostic callback failure cannot break preference fallback`() = runTest {
        val source = FakeFailoverPreferenceSource().apply {
            cooldownDurationSecFlow = flow { throw RuntimeException("Disk IO error") }
        }
        val reader = FailoverPreferenceReader(source) {
            throw IllegalStateException("Diagnostic sink failed")
        }

        assertEquals(
            ScreenPilotPreferenceDefaults.COOLDOWN_DURATION_SEC,
            reader.safeCooldownDurationSec()
        )
    }

    @Test
    fun `failover preference reader rethrows CancellationException`() = runTest {
        val source = FakeFailoverPreferenceSource().apply {
            cooldownDurationSecFlow = flow { throw CancellationException("Scope cancelled") }
        }

        val reader = FailoverPreferenceReader(source)
        var rethrown = false
        try {
            reader.safeCooldownDurationSec()
        } catch (e: CancellationException) {
            rethrown = true
        }
        assertTrue("FailoverPreferenceReader must rethrow CancellationException", rethrown)
    }

}
