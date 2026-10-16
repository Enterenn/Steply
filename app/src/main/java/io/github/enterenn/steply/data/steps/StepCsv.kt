package io.github.enterenn.steply.data.steps

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import io.github.enterenn.steply.domain.stepDelta
import kotlin.math.abs

/**
 * Export Astra : un intervalle de pas, pas un compteur.
 * En-tête exact, dans cet ordre.
 */
const val STEP_CSV_HEADER =
    "id,start_time,end_time,type,value,unit,resolution,provider,device_id,zone_offset"

private val headerColumns = STEP_CSV_HEADER.split(',')

data class CsvReading(
    val cumulativeSteps: Long,
    val recordedAt: Instant,
    val zoneOffset: String,
)

sealed interface StepCsvParse {
    data class Accepted(val readings: List<CsvReading>) : StepCsvParse
    data class Rejected(val problem: StepCsvProblem) : StepCsvParse
}

sealed interface StepCsvProblem {
    data object Empty : StepCsvProblem
    data object Header : StepCsvProblem
    data class Row(val number: Int, val reason: StepCsvRowReason) : StepCsvProblem
}

enum class StepCsvRowReason {
    Columns,
    Required,
    Type,
    Value,
    Unit,
    Resolution,
    Time,
    Order,
    Offset,
}

/**
 * Un fichier refusé ne remplace rien. Un fichier accepté remplace l'historique :
 * un second import du même fichier ne l'ajoute pas.
 */
fun applyStepCsv(current: List<CsvReading>, parsed: StepCsvParse): List<CsvReading> = when (parsed) {
    is StepCsvParse.Accepted -> parsed.readings
    is StepCsvParse.Rejected -> current
}

/**
 * Les pas affichés sont des écarts entre relevés cumulatifs.
 * Si deux résolutions couvrent le même instant, la plus fine est gardée.
 * L'identifiant du fichier n'entre pas dans le relevé.
 */
fun parseStepCsv(content: String): StepCsvParse {
    val source = if (content.startsWith('\uFEFF')) content.substring(1) else content
    if (source.isBlank()) return StepCsvParse.Rejected(StepCsvProblem.Empty)
    val records = splitRecords(source)
    if (records.isEmpty() || records.all { it.isBlank() }) return StepCsvParse.Rejected(StepCsvProblem.Empty)
    if (parseFields(records.first()) != headerColumns) return StepCsvParse.Rejected(StepCsvProblem.Header)
    val buckets = mutableListOf<Bucket>()
    var dataRow = 0
    for (index in 1 until records.size) {
        if (records[index].isBlank()) continue
        dataRow++
        when (val row = parseRow(records[index], dataRow)) {
            is RowParse.Bad -> return StepCsvParse.Rejected(StepCsvProblem.Row(dataRow, row.reason))
            is RowParse.Ok -> buckets += row.bucket
        }
    }
    return toReadings(keepFinest(buckets))
}

fun writeStepCsv(readings: List<CsvReading>): String = buildString {
    append(STEP_CSV_HEADER)
    append('\n')
    for (index in 1 until readings.size) {
        val previous = readings[index - 1]
        val current = readings[index]
        val delta = stepDelta(previous.cumulativeSteps, current.cumulativeSteps) ?: continue
        append(escape(index.toString()))
        append(',')
        append(escape(DateTimeFormatter.ISO_INSTANT.format(previous.recordedAt)))
        append(',')
        append(escape(DateTimeFormatter.ISO_INSTANT.format(current.recordedAt)))
        append(',')
        append("steps,")
        append(delta.steps)
        append(",count,")
        append(canonicalResolution(previous.recordedAt, current.recordedAt))
        append(",internal_phone,smartphone,")
        append(escape(previous.zoneOffset))
        append('\n')
    }
}

/**
 * Le cumul du fichier et le compteur du téléphone ne sont pas la même échelle.
 * On décale la série pour qu'elle finisse sur le compteur connu, sans changer les écarts.
 * Un relevé au moment de l'import, de même cumul, empêche de répartir les pas suivants
 * sur les jours déjà importés.
 */
fun anchorReadings(
    readings: List<CsvReading>,
    hardwareCumulative: Long?,
    now: Instant,
    zoneOffset: String,
): List<CsvReading> {
    if (readings.isEmpty()) return readings
    val shifted = shiftOnto(readings, hardwareCumulative) ?: readings
    val last = shifted.last()
    if (!last.recordedAt.isBefore(now)) return shifted
    return shifted + CsvReading(last.cumulativeSteps, now, zoneOffset)
}

private data class Bucket(
    val rowNumber: Int,
    val start: Instant,
    val end: Instant,
    val steps: Long,
    val zoneOffset: String,
    val rank: Int,
)

private sealed interface RowParse {
    data class Ok(val bucket: Bucket) : RowParse
    data class Bad(val reason: StepCsvRowReason) : RowParse
}

private fun parseRow(record: String, rowNumber: Int): RowParse {
    val fields = parseFields(record)
    if (fields.size != headerColumns.size) return RowParse.Bad(StepCsvRowReason.Columns)
    val values = headerColumns.zip(fields).toMap()
    if (headerColumns.any { values.getValue(it).trim().isEmpty() }) return RowParse.Bad(StepCsvRowReason.Required)
    if (values.getValue("type") != "steps") return RowParse.Bad(StepCsvRowReason.Type)
    val steps = values.getValue("value").toLongOrNull()
    if (steps == null || steps < 0L) return RowParse.Bad(StepCsvRowReason.Value)
    if (values.getValue("unit") != "count") return RowParse.Bad(StepCsvRowReason.Unit)
    val rank = resolutionRank(values.getValue("resolution")) ?: return RowParse.Bad(StepCsvRowReason.Resolution)
    val start = parseInstant(values.getValue("start_time")) ?: return RowParse.Bad(StepCsvRowReason.Time)
    val end = parseInstant(values.getValue("end_time")) ?: return RowParse.Bad(StepCsvRowReason.Time)
    if (end.isBefore(start)) return RowParse.Bad(StepCsvRowReason.Order)
    val zone = values.getValue("zone_offset")
    if (!validOffset(zone)) return RowParse.Bad(StepCsvRowReason.Offset)
    return RowParse.Ok(Bucket(rowNumber, start, end, steps, zone, rank))
}

private fun resolutionRank(resolution: String): Int? = when (resolution) {
    "5min" -> 0
    "1hour" -> 1
    "1d" -> 2
    else -> null
}

private fun parseInstant(raw: String): Instant? = try {
    Instant.parse(raw)
} catch (_: DateTimeParseException) {
    null
}

private fun validOffset(raw: String): Boolean {
    if (raw != "Z" && !raw.matches(Regex("[+-]\\d{2}:\\d{2}"))) return false
    return try {
        ZoneOffset.of(raw)
        true
    } catch (_: DateTimeException) {
        false
    }
}

private fun keepFinest(buckets: List<Bucket>): List<Bucket> {
    val accepted = mutableListOf<Bucket>()
    val ordered = buckets.sortedWith(compareBy({ it.rank }, { it.start }, { it.end }))
    for (bucket in ordered) {
        if (accepted.any { overlaps(it, bucket) }) continue
        accepted += bucket
    }
    return accepted.sortedWith(compareBy({ it.start }, { it.end }))
}

private fun overlaps(first: Bucket, second: Bucket): Boolean {
    val laterStart = if (first.start > second.start) first.start else second.start
    val earlierEnd = if (first.end < second.end) first.end else second.end
    val point = first.start == first.end || second.start == second.end
    return laterStart < earlierEnd || (laterStart == earlierEnd && point)
}

private fun toReadings(buckets: List<Bucket>): StepCsvParse {
    val readings = mutableListOf<CsvReading>()
    var cumulative = 0L
    for (bucket in buckets) {
        val lastTime = readings.lastOrNull()?.recordedAt
        if (lastTime == null || lastTime < bucket.start) {
            readings += CsvReading(cumulative, bucket.start, bucket.zoneOffset)
        }
        val next = try {
            Math.addExact(cumulative, bucket.steps)
        } catch (_: ArithmeticException) {
            return StepCsvParse.Rejected(StepCsvProblem.Row(bucket.rowNumber, StepCsvRowReason.Value))
        }
        cumulative = next
        val atEnd = readings.last().recordedAt == bucket.end
        if (atEnd && bucket.end == bucket.start) {
            readings += CsvReading(cumulative, bucket.end, bucket.zoneOffset)
        } else if (atEnd) {
            readings[readings.lastIndex] = CsvReading(cumulative, bucket.end, bucket.zoneOffset)
        } else {
            readings += CsvReading(cumulative, bucket.end, bucket.zoneOffset)
        }
    }
    return StepCsvParse.Accepted(readings)
}

private fun shiftOnto(readings: List<CsvReading>, hardwareCumulative: Long?): List<CsvReading>? {
    if (hardwareCumulative == null) return readings
    val offset = try {
        Math.subtractExact(hardwareCumulative, readings.last().cumulativeSteps)
    } catch (_: ArithmeticException) {
        return null
    }
    if (offset == 0L) return readings
    return try {
        readings.map { it.copy(cumulativeSteps = Math.addExact(it.cumulativeSteps, offset)) }
    } catch (_: ArithmeticException) {
        null
    }
}

private fun canonicalResolution(start: Instant, end: Instant): String {
    val seconds = java.time.Duration.between(start, end).seconds.coerceAtLeast(0L)
    val choices = longArrayOf(5L * 60L, 60L * 60L, 24L * 60L * 60L)
    val labels = arrayOf("5min", "1hour", "1d")
    var best = 0
    var bestDistance = Long.MAX_VALUE
    for (index in choices.indices) {
        val distance = abs(choices[index] - seconds)
        if (distance < bestDistance) {
            bestDistance = distance
            best = index
        }
    }
    return labels[best]
}

private fun escape(value: String): String {
    val special = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    if (!special) return value
    return "\"${value.replace("\"", "\"\"")}\""
}

private fun splitRecords(content: String): List<String> {
    val records = mutableListOf<String>()
    val buffer = StringBuilder()
    var quoted = false
    var index = 0
    while (index < content.length) {
        val char = content[index]
        if (char == '"') {
            buffer.append(char)
            if (quoted && index + 1 < content.length && content[index + 1] == '"') {
                buffer.append('"')
                index += 2
                continue
            }
            quoted = !quoted
            index++
            continue
        }
        if (!quoted && (char == '\n' || char == '\r')) {
            if (char == '\r' && index + 1 < content.length && content[index + 1] == '\n') index++
            records += buffer.toString()
            buffer.clear()
            index++
            continue
        }
        buffer.append(char)
        index++
    }
    if (buffer.isNotEmpty()) records += buffer.toString()
    return records
}

private fun parseFields(record: String): List<String> {
    val fields = mutableListOf<String>()
    val buffer = StringBuilder()
    var quoted = false
    var index = 0
    while (index < record.length) {
        val char = record[index]
        if (quoted) {
            if (char == '"') {
                if (index + 1 < record.length && record[index + 1] == '"') {
                    buffer.append('"')
                    index += 2
                    continue
                }
                quoted = false
                index++
                continue
            }
            buffer.append(char)
            index++
            continue
        }
        when (char) {
            '"' -> {
                quoted = true
                index++
            }
            ',' -> {
                fields += buffer.toString()
                buffer.clear()
                index++
            }
            else -> {
                buffer.append(char)
                index++
            }
        }
    }
    fields += buffer.toString()
    return fields
}
