package com.strava.aiservice.guard

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Offline evaluation of the prompt guard against a golden set. Two gates:
 * every `must` case has to agree with the guard (a hard regression), and the
 * overall agreement has to stay above the threshold in the file. Mismatches
 * are printed so a failing build says which case moved.
 *
 * This is the deterministic half of the AI eval suite; the live rubric evals
 * against a real provider live next to it and run only with an API key.
 */
@DisplayName("PromptGuard golden eval")
class GuardGoldenEvalTest {

    private data class Case(val id: String, val text: String, val expect: String, val must: Boolean = false)
    private data class Golden(val description: String, val threshold: Double, val cases: List<Case>)

    private val guard = PromptGuard()

    @Test
    fun agreementWithGoldenSet() {
        val golden: Golden = jacksonObjectMapper()
            .readValue(javaClass.getResourceAsStream("/evals/guard-golden.json")!!)

        val mismatches = mutableListOf<String>()
        val hardFailures = mutableListOf<String>()
        for (case in golden.cases) {
            val decision = guard.check(case.text)
            val actual = if (decision is PromptGuard.Decision.Blocked) "block" else "allow"
            if (actual != case.expect) {
                val line = "${case.id}: expected ${case.expect}, got $actual" +
                    (if (decision is PromptGuard.Decision.Blocked) " (${decision.reason})" else "") +
                    " — \"${case.text.take(60)}\""
                mismatches += line
                if (case.must) hardFailures += line
            }
        }

        val agreement = 1.0 - mismatches.size.toDouble() / golden.cases.size
        println("guard golden eval: ${golden.cases.size} cases, agreement ${"%.3f".format(agreement)}, threshold ${golden.threshold}")
        mismatches.forEach { println("  MISMATCH $it") }

        assertThat(hardFailures).describedAs("hard (must=true) golden cases").isEmpty()
        assertThat(agreement).describedAs("agreement with golden set").isGreaterThanOrEqualTo(golden.threshold)
    }
}
