package com.gryglicki.kotlin

import org.junit.Assert.assertEquals
import org.junit.Test

class SequenceTest {

    @Test
    fun kotlin_sequences_can_be_used_multiple_times() {
        //Given
        val seq = listOf(1, 2, 3).asSequence()

        //When
        val sum = seq.sum()
        val count = seq.count()

        //Then
        assertEquals(6, sum)
        assertEquals(3, count)
    }
}