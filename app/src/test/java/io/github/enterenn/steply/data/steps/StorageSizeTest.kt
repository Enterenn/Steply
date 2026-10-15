package io.github.enterenn.steply.data.steps

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageSizeTest {
    @Test
    fun zero() {
        assertEquals("0 B", formatStorageSize(0, Locale.US))
    }

    @Test
    fun justBelowKilobyte() {
        assertEquals("1023 B", formatStorageSize(1023, Locale.US))
    }

    @Test
    fun kilobyte() {
        assertEquals("1 KB", formatStorageSize(1024, Locale.US))
    }

    @Test
    fun megabyte() {
        assertEquals("1 MB", formatStorageSize(1024L * 1024L, Locale.US))
    }

    @Test
    fun trailingDecimalRemoved() {
        assertEquals("2 KB", formatStorageSize(2L * 1024L, Locale.US))
    }

    @Test
    fun frenchComma() {
        assertEquals("1,5 Ko", formatStorageSize(1536, Locale.FRANCE))
    }
}
