package com.sarangi.audio

import com.sarangi.audio.pitch.PitchDetector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class PitchDetectorTest {

    private lateinit var pitchDetector: PitchDetector

    @Before
    fun setup() {
        pitchDetector = PitchDetector()
        pitchDetector.setNoiseFloor(0.001)
    }

    @Test
    fun `detect A4 at 440 Hz`() {
        val samples = generateSineWave(440.0, 44100, 2048)
        val result = pitchDetector.yinPitchDetection(samples, 44100)

        assertNotNull(result)
        result?.let {
            assertEquals(440.0, it.frequency, 5.0)
            assertTrue(it.confidence > 0.8)
            assertTrue(it.noteName.startsWith("A"))
            assertTrue(abs(it.centsDeviation) < 20)
        }
    }

    @Test
    fun `detect G3 at 196 Hz (low violin range)`() {
        val samples = generateSineWave(196.0, 44100, 4096)
        val result = pitchDetector.yinPitchDetection(samples, 44100)

        assertNotNull(result)
        result?.let {
            assertEquals(196.0, it.frequency, 10.0)
            assertTrue(it.confidence > 0.7)
        }
    }

    @Test
    fun `detect E5 at 659 Hz`() {
        val samples = generateSineWave(659.0, 44100, 2048)
        val result = pitchDetector.yinPitchDetection(samples, 44100)

        assertNotNull(result)
        result?.let {
            assertEquals(659.0, it.frequency, 10.0)
            assertTrue(it.confidence > 0.7)
        }
    }

    @Test
    fun `silence returns null or low confidence`() {
        val samples = DoubleArray(2048) { 0.0 }
        val result = pitchDetector.yinPitchDetection(samples, 44100)

        // Either null or very low confidence
        if (result != null) {
            assertTrue(result.confidence < 0.5)
        }
    }

    @Test
    fun `noise returns no clear pitch`() {
        val samples = DoubleArray(2048) { (Math.random() - 0.5) * 0.1 }
        val result = pitchDetector.yinPitchDetection(samples, 44100)

        // Either null or low confidence
        if (result != null) {
            assertTrue(result.confidence < 0.85)
        }
    }

    private fun generateSineWave(frequency: Double, sampleRate: Int, numSamples: Int): DoubleArray {
        return DoubleArray(numSamples) { i ->
            0.8 * sin(2 * Math.PI * frequency * i / sampleRate)
        }
    }
}
