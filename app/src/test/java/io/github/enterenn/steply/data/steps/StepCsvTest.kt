package io.github.enterenn.steply.data.steps

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StepCsvTest {
    private val zone = ZoneOffset.of("+02:00")
    private val may22 = LocalDate.of(2026, 5, 22)
    private val may23 = LocalDate.of(2026, 5, 23)

    @Test
    fun acceptsAstraHeaderWithNoRows() {
        val existing = listOf(reading(5, "2026-05-22T10:00:00Z"))
        val parsed = parseStepCsv("\uFEFF$STEP_CSV_HEADER\n")
        assertTrue(parsed is StepCsvParse.Accepted)
        assertEquals(emptyList<CsvReading>(), (parsed as StepCsvParse.Accepted).readings)
        assertEquals(emptyList<CsvReading>(), applyStepCsv(existing, parsed))
    }

    @Test
    fun rejectsInvalidRowWithoutReplacing() {
        val current = listOf(reading(5, "2026-05-22T08:00:00Z"))
        val parsed = parseStepCsv(
            file(
                row("kept-out", "2026-05-22T10:00:00Z", "2026-05-22T10:05:00Z", 10),
                row("bad", "2026-05-22T10:05:00Z", "2026-05-22T10:10:00Z", -3),
            ),
        )
        assertEquals(
            StepCsvProblem.Row(2, StepCsvRowReason.Value),
            (parsed as StepCsvParse.Rejected).problem,
        )
        assertEquals(current, applyStepCsv(current, parsed))
    }

    @Test
    fun ignoresExternalId() {
        val start = "2026-05-22T14:30:00Z"
        val end = "2026-05-22T14:35:00Z"
        val numericId = parseStepCsv(file(row("100", start, end, 40))) as StepCsvParse.Accepted
        val quotedId = parseStepCsv(file(row("\"ext,ernal\"", start, end, 40))) as StepCsvParse.Accepted
        assertEquals(quotedId.readings, numericId.readings)
        assertEquals(0L, numericId.readings.first().cumulativeSteps)
        assertEquals(40L, numericId.readings.last().cumulativeSteps)
    }

    @Test
    fun roundTripPreservesStepsPerDay() {
        val parsed = parseStepCsv(sample()) as StepCsvParse.Accepted
        val again = parseStepCsv(writeStepCsv(parsed.readings)) as StepCsvParse.Accepted
        assertEquals(50L, steps(parsed.readings, may22))
        assertEquals(130L, steps(parsed.readings, may23))
        assertEquals(steps(parsed.readings, may22), steps(again.readings, may22))
        assertEquals(steps(parsed.readings, may23), steps(again.readings, may23))
    }

    @Test
    fun secondImportDoesNotDouble() {
        val parsed = parseStepCsv(sample())
        val once = applyStepCsv(emptyList(), parsed)
        val twice = applyStepCsv(once, parsed)
        assertEquals(once, twice)
        assertEquals(50L, steps(twice, may22))
        assertEquals(130L, steps(twice, may23))
    }

    @Test
    fun keepsFinerBucketsWhenResolutionsOverlap() {
        val withDaily = file(
            row(
                "day",
                "2026-05-22T21:30:00Z",
                "2026-05-23T06:30:00Z",
                9_000,
                resolution = "1d",
            ),
            row("hour-1", "2026-05-22T21:30:00Z", "2026-05-22T22:30:00Z", 100, resolution = "1hour"),
            row("hour-2", "2026-05-22T22:30:00Z", "2026-05-23T06:30:00Z", 80, resolution = "1hour"),
        )
        val parsed = parseStepCsv(withDaily) as StepCsvParse.Accepted
        assertEquals(50L, steps(parsed.readings, may22))
        assertEquals(130L, steps(parsed.readings, may23))
    }

    @Test
    fun anchorsImportedStepsWithoutChangingDays() {
        val imported = (parseStepCsv(sample()) as StepCsvParse.Accepted).readings
        val now = Instant.parse("2026-10-15T12:00:00Z")
        val anchored = anchorReadings(imported, 10_000L, now, "+02:00")
        assertEquals(10_000L, anchored.last().cumulativeSteps)
        assertEquals(now, anchored.last().recordedAt)
        assertEquals(50L, steps(anchored, may22))
        assertEquals(130L, steps(anchored, may23))
        val after = anchored + reading(10_010L, "2026-10-15T13:00:00Z")
        assertEquals(10L, steps(after, LocalDate.of(2026, 10, 15)))
        assertEquals(50L, steps(after, may22))
    }

    private fun sample(): String = file(
        row("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "2026-05-22T21:30:00Z", "2026-05-22T22:30:00Z", 100, "1hour"),
        row("bbbbbbbb-cccc-dddd-eeee-ffffffffffff", "2026-05-22T22:30:00Z", "2026-05-23T06:30:00Z", 80, "1hour"),
    )

    private fun file(vararg rows: String): String = (listOf(STEP_CSV_HEADER) + rows).joinToString("\n") + "\n"

    private fun row(
        id: String,
        start: String,
        end: String,
        value: Int,
        resolution: String = "5min",
    ): String = "$id,$start,$end,steps,$value,count,$resolution,internal_phone,smartphone,+02:00"

    private fun reading(cumulative: Long, instant: String) =
        CsvReading(cumulative, Instant.parse(instant), "+02:00")

    private fun steps(readings: List<CsvReading>, day: LocalDate): Long = daySteps(
        readings.map { StepReading(it.cumulativeSteps, it.recordedAt) },
        day,
        zone,
    )
}
