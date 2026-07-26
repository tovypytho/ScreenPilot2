package com.example.service

import com.example.data.GeminiKeyHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class FailoverDecisionTest {

    @Test
    fun testEvaluateHttp400ReturnsStopRotation() {
        val exception = ApiException(400, "Bad Request")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.StopRotation)
    }

    @Test
    fun testEvaluateHttp401ReturnsContinueToNextKey() {
        val exception = ApiException(401, "Unauthorized")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.AUTH_FAILED.name, continueAction.healthStatus)
        assertEquals("401", continueAction.failureType)
        assertEquals(0L, continueAction.cooldownMs)
    }

    @Test
    fun testEvaluateHttp403ReturnsContinueToNextKey() {
        val exception = ApiException(403, "Forbidden")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.PERMISSION_DENIED.name, continueAction.healthStatus)
        assertEquals("403", continueAction.failureType)
        assertEquals(0L, continueAction.cooldownMs)
    }

    @Test
    fun testEvaluateHttp404ReturnsStopRotation() {
        val exception = ApiException(404, "Not Found")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.StopRotation)
    }

    @Test
    fun testEvaluateHttp429ReturnsContinueWithCooldown() {
        val exception = ApiException(429, "Too Many Requests")
        val action = FailoverDecision.evaluate(exception, 45)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.COOLDOWN.name, continueAction.healthStatus)
        assertEquals("429", continueAction.failureType)
        assertEquals(45000L, continueAction.cooldownMs)
    }

    @Test
    fun testEvaluateHttp500ReturnsContinueWithFailed() {
        val exception = ApiException(500, "Internal Server Error")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.TEMPORARY_FAILURE.name, continueAction.healthStatus)
        assertEquals("500", continueAction.failureType)
        assertEquals(0L, continueAction.cooldownMs)
    }

    @Test
    fun testEvaluateSocketTimeoutExceptionReturnsContinueWithTimeout() {
        val exception = SocketTimeoutException("Read timed out")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.TEMPORARY_FAILURE.name, continueAction.healthStatus)
        assertEquals("Timeout", continueAction.failureType)
        assertEquals(0L, continueAction.cooldownMs)
    }

    @Test
    fun testEvaluateGenericExceptionReturnsContinueWithError() {
        val exception = RuntimeException("Some generic failure")
        val action = FailoverDecision.evaluate(exception, 30)
        assertTrue(action is FailoverAction.ContinueToNextKey)
        val continueAction = action as FailoverAction.ContinueToNextKey
        assertEquals(GeminiKeyHealth.TEMPORARY_FAILURE.name, continueAction.healthStatus)
        assertEquals("Error", continueAction.failureType)
        assertEquals(0L, continueAction.cooldownMs)
    }
}
