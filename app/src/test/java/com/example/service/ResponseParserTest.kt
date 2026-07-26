package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponseParserTest {

    @Test
    fun multipleChoiceStructuredResponseIsAccepted() {
        val result = ResponseParser.parse(
            """{"question_type":"MULTIPLE_CHOICE","answer_index":3,"confidence":0.9}"""
        )
        assertTrue(result is ParsedAnswer.MultipleChoice)
        result as ParsedAnswer.MultipleChoice
        assertEquals(3, result.answerIndex)
        assertEquals(0.9, result.confidence ?: 0.0, 0.0001)
    }

    @Test
    fun freeResponseStructuredResponseIsAccepted() {
        val result = ResponseParser.parse(
            """{"question_type":"FREE_RESPONSE","answer_text":"Paris","confidence":0.95}"""
        )
        assertTrue(result is ParsedAnswer.FreeResponse)
        result as ParsedAnswer.FreeResponse
        assertEquals("Paris", result.answerText)
        assertEquals(0.95, result.confidence ?: 0.0, 0.0001)
    }

    @Test
    fun unclearStructuredResponseIsAcceptedWithoutAnswerFields() {
        val result = ResponseParser.parse(
            """{"question_type":"UNCLEAR","confidence":0.31}"""
        )
        assertTrue(result is ParsedAnswer.Unclear)
        assertEquals(0.31, result.confidence ?: 0.0, 0.0001)
    }

    @Test
    fun questionTypeIsCaseInsensitive() {
        val result = ResponseParser.parse(
            """{"question_type":"free_response","answer_text":"96"}"""
        )
        assertTrue(result is ParsedAnswer.FreeResponse)
        assertEquals("96", (result as ParsedAnswer.FreeResponse).answerText)
    }

    @Test
    fun freeResponseWhitespaceIsNormalized() {
        val result = ResponseParser.parse(
            """{"question_type":"FREE_RESPONSE","answer_text":"  Energi   cahaya\nmenjadi   energi kimia.  "}"""
        ) as ParsedAnswer.FreeResponse
        assertEquals("Energi cahaya menjadi energi kimia.", result.answerText)
    }

    @Test
    fun freeResponseLongTextIsBoundedInsteadOfFailing() {
        val longText = "a".repeat(700)
        val result = ResponseParser.parse(
            """{"question_type":"FREE_RESPONSE","answer_text":"$longText"}"""
        ) as ParsedAnswer.FreeResponse
        assertEquals(500, result.answerText.length)
        assertTrue(result.answerText.endsWith("…"))
    }

    @Test
    fun freeResponseMissingAnswerTextIsRejected() {
        expectFailure("""{"question_type":"FREE_RESPONSE"}""")
    }

    @Test
    fun freeResponseEmptyAnswerTextIsRejected() {
        expectFailure("""{"question_type":"FREE_RESPONSE","answer_text":"   "}""")
    }

    @Test
    fun multipleChoiceMissingIndexIsRejected() {
        expectFailure("""{"question_type":"MULTIPLE_CHOICE"}""")
    }

    @Test
    fun multipleChoiceIndexBelowRangeIsRejected() {
        expectFailure("""{"question_type":"MULTIPLE_CHOICE","answer_index":0}""")
    }

    @Test
    fun multipleChoiceIndexAboveRangeIsRejected() {
        expectFailure("""{"question_type":"MULTIPLE_CHOICE","answer_index":6}""")
    }

    @Test
    fun multipleChoiceDecimalIndexIsRejected() {
        expectFailure("""{"question_type":"MULTIPLE_CHOICE","answer_index":3.5}""")
    }

    @Test
    fun multipleChoiceStringIndexIsRejected() {
        expectFailure("""{"question_type":"MULTIPLE_CHOICE","answer_index":"3"}""")
    }

    @Test
    fun unknownQuestionTypeIsRejected() {
        expectFailure("""{"question_type":"ESSAY","answer_text":"x"}""")
    }

    @Test
    fun missingQuestionTypeWithoutLegacyIndexIsRejected() {
        expectFailure("""{"confidence":0.8}""")
    }

    @Test
    fun legacyStructuredMultipleChoiceResponseRemainsAccepted() {
        val result = ResponseParser.parse("""{"answer_index":4,"confidence":0.8}""")
        assertTrue(result is ParsedAnswer.MultipleChoice)
        result as ParsedAnswer.MultipleChoice
        assertEquals(4, result.answerIndex)
        assertEquals(0.8, result.confidence ?: 0.0, 0.0001)
    }

    @Test
    fun legacySingleDigitResponseRemainsAccepted() {
        val result = ResponseParser.parse("3")
        assertTrue(result is ParsedAnswer.MultipleChoice)
        result as ParsedAnswer.MultipleChoice
        assertEquals(3, result.answerIndex)
        assertNull(result.confidence)
    }

    @Test
    fun markdownFencedTypedJsonIsAccepted() {
        val result = ResponseParser.parse(
            "```json\n{\"question_type\":\"FREE_RESPONSE\",\"answer_text\":\"Tokyo\"}\n```"
        )
        assertTrue(result is ParsedAnswer.FreeResponse)
        assertEquals("Tokyo", (result as ParsedAnswer.FreeResponse).answerText)
    }

    @Test
    fun invalidConfidenceIsIgnored() {
        val result = ResponseParser.parse(
            """{"question_type":"MULTIPLE_CHOICE","answer_index":2,"confidence":1.4}"""
        ) as ParsedAnswer.MultipleChoice
        assertNull(result.confidence)
    }

    @Test
    fun stringConfidenceIsIgnored() {
        val result = ResponseParser.parse(
            """{"question_type":"FREE_RESPONSE","answer_text":"180","confidence":"0.9"}"""
        ) as ParsedAnswer.FreeResponse
        assertNull(result.confidence)
    }

    @Test
    fun plainTextExplanationIsRejected() {
        expectFailure("The answer is 3")
    }

    @Test
    fun emptyContentIsRejected() {
        expectFailure("")
    }

    @Test
    fun malformedJsonIsRejected() {
        expectFailure("{\"question_type\":\"FREE_RESPONSE\"")
    }

    @Test
    fun extractGeminiTextReadsFirstCandidateText() {
        val expected = "{\"question_type\":\"UNCLEAR\"}"
        val response = org.json.JSONObject().apply {
            put("candidates", org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("content", org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply { put("text", expected) })
                        })
                    })
                })
            })
        }.toString()
        assertEquals(expected, ResponseParser.extractGeminiText(response))
    }

    private fun expectFailure(value: String) {
        try {
            ResponseParser.parse(value)
            fail("Expected parser failure for: $value")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
