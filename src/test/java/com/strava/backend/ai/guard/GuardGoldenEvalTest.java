package com.strava.backend.ai.guard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Offline evaluation of the prompt guard against a golden set. Two gates:
 * every {@code must} case has to agree with the guard (a hard regression),
 * and the overall agreement has to stay above the threshold in the file.
 * Mismatches are printed so a failing build says which case moved.
 *
 * <p>This is the deterministic half of the AI eval suite; the live rubric
 * evals against a real provider live in {@code com.strava.backend.eval} and
 * run only with an API key.
 */
@DisplayName("PromptGuard golden eval")
class GuardGoldenEvalTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Case(String id, String text, String expect, boolean must) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Golden(String description, double threshold, List<Case> cases) { }

    private final PromptGuard guard = new PromptGuard();

    @Test
    void agreementWithGoldenSet() throws Exception {
        Golden golden;
        try (InputStream in = getClass().getResourceAsStream("/evals/guard-golden.json")) {
            golden = new ObjectMapper().readValue(in, Golden.class);
        }

        List<String> mismatches = new ArrayList<>();
        List<String> hardFailures = new ArrayList<>();
        for (Case c : golden.cases()) {
            PromptGuard.Decision decision = guard.check(c.text());
            String actual = decision instanceof PromptGuard.Decision.Blocked ? "block" : "allow";
            if (!actual.equals(c.expect())) {
                String reason = decision instanceof PromptGuard.Decision.Blocked b ? " (" + b.reason() + ")" : "";
                String line = c.id() + ": expected " + c.expect() + ", got " + actual + reason
                        + " — \"" + c.text().substring(0, Math.min(60, c.text().length())) + "\"";
                mismatches.add(line);
                if (c.must()) hardFailures.add(line);
            }
        }

        double agreement = 1.0 - (double) mismatches.size() / golden.cases().size();
        System.out.printf(Locale.ROOT, "guard golden eval: %d cases, agreement %.3f, threshold %.2f%n",
                golden.cases().size(), agreement, golden.threshold());
        mismatches.forEach(m -> System.out.println("  MISMATCH " + m));

        assertThat(hardFailures).describedAs("hard (must=true) golden cases").isEmpty();
        assertThat(agreement).describedAs("agreement with golden set").isGreaterThanOrEqualTo(golden.threshold());
    }
}
