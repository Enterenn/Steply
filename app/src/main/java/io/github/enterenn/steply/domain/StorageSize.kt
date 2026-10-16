package io.github.enterenn.steply.domain

import java.util.Locale

private const val Kibi = 1024L

fun storageFileBytes(database: Long, wal: Long, shm: Long): Long =
    present(database) + present(wal) + present(shm)

fun formatStorageSize(bytes: Long, locale: Locale): String {
    val safe = present(bytes)
    val french = locale.language.equals("fr", ignoreCase = true)
    val separator = if (french) ',' else '.'
    if (safe < Kibi) return "$safe ${if (french) "o" else "B"}"
    val mega = Kibi * Kibi
    return if (safe < mega) {
        formatScaled(safe, Kibi, separator, if (french) "Ko" else "KB")
    } else {
        formatScaled(safe, mega, separator, if (french) "Mo" else "MB")
    }
}

private fun present(bytes: Long): Long = if (bytes > 0L) bytes else 0L

private fun formatScaled(bytes: Long, unitBytes: Long, separator: Char, unit: String): String {
    val whole = bytes / unitBytes
    val remainder = bytes % unitBytes
    var fraction = (remainder * 10 + unitBytes / 2) / unitBytes
    var shown = whole
    if (fraction >= 10L) {
        shown += 1
        fraction = 0
    }
    val number = if (fraction == 0L) shown.toString() else "$shown$separator$fraction"
    return "$number $unit"
}
