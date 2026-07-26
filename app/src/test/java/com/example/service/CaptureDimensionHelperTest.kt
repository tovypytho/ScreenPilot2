package com.example.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CaptureDimensionHelperTest {

    @Test
    fun testSameWidthHeightAndDensityRequiresNoResize() {
        val current = CaptureDimensions(1080, 1920, 480)
        assertFalse(CaptureDimensionHelper.isResizeRequired(current, 1080, 1920, 480))
    }

    @Test
    fun testPortraitToLandscapeRequiresResize() {
        val current = CaptureDimensions(1080, 1920, 480)
        assertTrue(CaptureDimensionHelper.isResizeRequired(current, 1920, 1080, 480))
    }

    @Test
    fun testLandscapeToPortraitRequiresResize() {
        val current = CaptureDimensions(1920, 1080, 480)
        assertTrue(CaptureDimensionHelper.isResizeRequired(current, 1080, 1920, 480))
    }

    @Test
    fun testDensityChangeRequiresResize() {
        val current = CaptureDimensions(1080, 1920, 480)
        assertTrue(CaptureDimensionHelper.isResizeRequired(current, 1080, 1920, 320))
    }

    @Test
    fun testZeroWidthIsRejected() {
        assertFalse(CaptureDimensionHelper.isValid(0, 1920, 480))
        try {
            CaptureDimensions(0, 1920, 480)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testZeroHeightIsRejected() {
        assertFalse(CaptureDimensionHelper.isValid(1080, 0, 480))
        try {
            CaptureDimensions(1080, 0, 480)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testZeroDensityIsRejected() {
        assertFalse(CaptureDimensionHelper.isValid(1080, 1920, 0))
        try {
            CaptureDimensions(1080, 1920, 0)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun testNegativeDimensionsAreRejected() {
        assertFalse(CaptureDimensionHelper.isValid(-1080, 1920, 480))
        assertFalse(CaptureDimensionHelper.isValid(1080, -1920, 480))
        assertFalse(CaptureDimensionHelper.isValid(1080, 1920, -480))
        
        try {
            CaptureDimensions(-1080, 1920, 480)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {}
        
        try {
            CaptureDimensions(1080, -1920, 480)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {}
        
        try {
            CaptureDimensions(1080, 1920, -480)
            fail("Expected exception not thrown")
        } catch (e: IllegalArgumentException) {}
    }

    @Test
    fun testNormalizedCoordinatesBelowZeroClampToTopLeft() {
        val result = CaptureDimensionHelper.normalizedToPixel(-0.5f, -0.2f, 1080, 1920, 100, 100)
        assertEquals(0, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun testNormalizedCoordinatesAboveOneClampInsideBottomRightEdge() {
        // screen width 1080, height 1920. element is 100x100.
        // normalized above 1.0 (e.g. 1.5)
        val result = CaptureDimensionHelper.normalizedToPixel(1.5f, 1.2f, 1080, 1920, 100, 100)
        assertEquals(980, result.first)
        assertEquals(1820, result.second)
    }

    @Test
    fun testButtonLargerThanAvailableBoundsDoesNotProduceNegativeCoordinates() {
        // screen is 1080x1920, button is 1200x2000
        val result = CaptureDimensionHelper.clampCoordinates(100, 100, 1080, 1920, 1200, 2000)
        assertTrue(result.first >= 0)
        assertTrue(result.second >= 0)
        assertEquals(0, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun testScaleToMax_widthConstraint() {
        // Width is larger, so width should be scaled to maxDim and height proportionally
        val (w, h) = CaptureDimensionHelper.scaleToMax(1200, 800, 600)
        assertEquals(600, w)
        assertEquals(400, h)
    }

    @Test
    fun testScaleToMax_heightConstraint() {
        // Height is larger, so height should be scaled to maxDim and width proportionally
        val (w, h) = CaptureDimensionHelper.scaleToMax(800, 1200, 600)
        assertEquals(400, w)
        assertEquals(600, h)
    }
}
