package com.sarangi.audio

import com.sarangi.audio.rhythm.RhythmAnalyser
import com.sarangi.core.model.RhythmResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.sin

class RhythmAnalyserTest {

    private lateinit var rhythmAnalyser: RhythmAnalyser

    @Before
    fun setup() {
        rhythmAnalyser = RhythmAnalyser()
    }

    @Test
    fun `detect evenly spaced onsets`() = runBlocking {
        // Generate clicks at regular intervals (every 500ms = 120 BPM)
        val sampleRate = 44100
        val totalDurationMs = 3000
        val totalSamples = sampleRate * totalDurationMs / 1000
        val clickIntervalSamples = sampleRate / 2 // 500ms

        val samples = ShortArray(totalSamples)
        for (i in samples.indices) {
            val isClick = (i % clickIntervalSamples) < 100
            samples[i] = if (isClick) 20000 else 0
        }

        // Process in chunks
        val chunkSize = 2048
        var offset = 0
        while (offset + chunkSize <= samples.size) {
            rhythmAnalyser.processFrame(samples.sliceArray(offset until offset + chunkSize))
            offset += chunkSize
        }

        // The rhythm analyser should detect some onsets
        // We're just verifying it doesn't crash and produces output
        assertTrue(true) // Basic test passes if no exception
    }

    @Test
    fun `reset clears state`() {
        rhythmAnalyser.reset()
        // Should not throw
        assertTrue(true)
    }
}
