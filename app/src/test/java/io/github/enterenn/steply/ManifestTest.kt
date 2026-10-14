package io.github.enterenn.steply

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ManifestTest {
    @Test
    fun manifestDeclaresNoInternetPermission() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"))
    }
}
