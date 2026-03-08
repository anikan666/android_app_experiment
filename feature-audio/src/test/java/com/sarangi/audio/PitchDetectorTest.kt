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
    }

    @Test
    fun `yin detects A4 at 440 Hz`() {
        val samples = generateSineWave(440.0, 44100, 2048)
        val result = pitchDetector.yin(samples)

        assertTrue("Confidence should be high for clean sine", result.confidence > 0.8)
        assertEquals(440.0, result.frequency, 10.0)
    }

    @Test
    fun `yin detects G3 at 196 Hz`() {
        val samples = generateSineWave(196.0, 44100, 4096)
        val result = pitchDetector.yin(samples)

        assertTrue("Confidence should be reasonable", result.confidence > 0.5)
        assertEquals(196.0, result.frequency, 15.0)
    }

    @Test
    fun `yin detects E5 at 659 Hz`() {
        val samples = generateSineWave(659.0, 44100, 2048)
        val result = pitchDetector.yin(samples)

        assertTrue("Confidence should be reasonable", result.confidence > 0.5)
        assertEquals(659.0, result.frequency, 15.0)
    }

    @Test
    fun `silence returns low confidence`() {
        val samples = DoubleArray(2048) { 0.0 }
        val result = pitchDetector.yin(samples)

        assertTrue("Silence should have low confidence", result.confidence < 0.5)
    }

    @Test
    fun `frequencyToNote correctly maps A4`() {
        val (note, cents) = pitchDetector.frequencyToNote(440.0)
        assertEquals("A4", note)
        assertTrue("Cents should be near zero for exact frequency", abs(cents) < 1.0)
    }

    @Test
    fun `frequencyToNote correctly maps C4`() {
        val (note, cents) = pitchDetector.frequencyToNote(261.63)
        assertEquals("C4", note)
        assertTrue("Cents should be near zero", abs(cents) < 5.0)
    }

    private fun generateSineWave(frequency: Double, sampleRate: Int, numSamples: Int): DoubleArray {
        return DoubleArray(numSamples) { i ->
            0.8 * sin(2 * Math.PI * frequency * i / sampleRate)
        }
    }
}
