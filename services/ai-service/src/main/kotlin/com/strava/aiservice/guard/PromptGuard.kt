package com.strava.aiservice.guard

/**
 * Input guard for text that ends up inside a prompt. It is deliberately
 * narrow: it blocks the handful of patterns that only make sense as an
 * attempt to reprogram the model (instruction overrides, system-prompt
 * exfiltration, role hijacks, chat-template tokens) plus obvious noise.
 * Ordinary coaching questions — including ones that contain words like
 * "ignore" or "system" in a sporting sense — must pass; the golden set in
 * `src/test/resources/evals/guard-golden.json` pins that boundary and the
 * eval test fails the build when agreement drops below its threshold.
 *
 * The guard never rewrites content; it either admits the text or rejects it
 * with a reason that is safe to show the user and to count in a metric.
 */
class PromptGuard(private val maxRepeatedChars: Int = 40) {

    sealed interface Decision {
        data object Allowed : Decision
        data class Blocked(val reason: Reason) : Decision
    }

    enum class Reason(val userMessage: String) {
        INSTRUCTION_OVERRIDE("Request blocked: it asks the assistant to ignore its instructions"),
        PROMPT_EXFILTRATION("Request blocked: it asks for the assistant's hidden instructions"),
        ROLE_HIJACK("Request blocked: it tries to change what the assistant is"),
        TEMPLATE_TOKENS("Request blocked: it contains chat-template control tokens"),
        CONTROL_CHARACTERS("Request blocked: it contains control characters"),
        NOISE("Request blocked: it is not readable text"),
    }

    private data class Rule(val reason: Reason, val pattern: Regex)

    private val rules: List<Rule> = listOf(
        // "ignore/disregard/forget/override ... (previous|your|all|the) instructions|rules|prompt|guidelines"
        Rule(
            Reason.INSTRUCTION_OVERRIDE,
            Regex(
                "\\b(ignore|disregard|forget|override|bypass)\\b[^.!?\\n]{0,40}\\b(instructions?|rules?|prompts?|guidelines?|directions?|constraints?|policy|policies)\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Rule(
            Reason.INSTRUCTION_OVERRIDE,
            Regex("\\b(new|updated|real|actual)\\s+(instructions?|rules?|system\\s+prompt)\\s*:", RegexOption.IGNORE_CASE)
        ),
        Rule(
            Reason.PROMPT_EXFILTRATION,
            Regex(
                "\\b(reveal|show|print|repeat|output|display|leak|dump|tell me)\\b[^.!?\\n]{0,40}\\b(system\\s+prompt|hidden\\s+(prompt|instructions?)|initial\\s+(prompt|instructions?)|your\\s+(instructions?|rules|guidelines|configuration))\\b",
                RegexOption.IGNORE_CASE
            )
        ),
        Rule(
            Reason.ROLE_HIJACK,
            Regex(
                "\\b(you are now|from now on you are|act as|pretend (to be|you are)|roleplay as|simulate being)\\b[^.!?\\n]{0,60}\\b(an? )?(unrestricted|uncensored|jailbroken|different|new|evil|system|developer|admin|root|dan\\b|gpt|assistant without)",
                RegexOption.IGNORE_CASE
            )
        ),
        Rule(
            Reason.ROLE_HIJACK,
            Regex("\\b(developer\\s+mode|jailbreak|do anything now|DAN\\s+mode)\\b", RegexOption.IGNORE_CASE)
        ),
        Rule(
            Reason.TEMPLATE_TOKENS,
            Regex(
                "(<\\|im_(start|end)\\|>|<\\|(system|user|assistant|endoftext)\\|>|\\[/?INST]|<<SYS>>|<\\|start_header_id\\|>|###\\s*(system|instruction)\\s*:|^\\s*system\\s*:\\s*$)",
                setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
            )
        ),
    )

    /** C0/C1 controls except tab/newline, zero-width and bidi override characters, BOM. */
    private val controlChars = Regex(
        "[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]"
    )

    fun check(text: String): Decision {
        if (controlChars.containsMatchIn(text)) return Decision.Blocked(Reason.CONTROL_CHARACTERS)
        if (isNoise(text)) return Decision.Blocked(Reason.NOISE)
        for (rule in rules) {
            if (rule.pattern.containsMatchIn(text)) return Decision.Blocked(rule.reason)
        }
        return Decision.Allowed
    }

    /** Runs of one character (e.g. 200 × "a") or text with almost no letters. */
    private fun isNoise(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        var run = 1
        for (i in 1 until trimmed.length) {
            run = if (trimmed[i] == trimmed[i - 1]) run + 1 else 1
            if (run > maxRepeatedChars) return true
        }
        if (trimmed.length >= 20) {
            val readable = trimmed.count { it.isLetterOrDigit() || it.isWhitespace() }
            if (readable.toDouble() / trimmed.length < 0.5) return true
        }
        return false
    }
}
