package com.strava.backend.ai.guard;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Input guard for text that ends up inside a prompt. It is deliberately
 * narrow: it blocks the handful of patterns that only make sense as an
 * attempt to reprogram the model (instruction overrides, system-prompt
 * exfiltration, role hijacks, chat-template tokens) plus obvious noise.
 * Ordinary coaching questions — including ones that contain words like
 * "ignore" or "system" in a sporting sense — must pass; the golden set in
 * {@code src/test/resources/evals/guard-golden.json} pins that boundary and
 * the eval test fails the build when agreement drops below its threshold.
 *
 * <p>The guard never rewrites content; it either admits the text or rejects
 * it with a reason that is safe to show the user and to count in a metric.
 */
public class PromptGuard {

    public sealed interface Decision permits Decision.Allowed, Decision.Blocked {
        record Allowed() implements Decision { }
        record Blocked(Reason reason) implements Decision { }

        Allowed ALLOWED = new Allowed();

        static Decision allowed() {
            return ALLOWED;
        }
    }

    public enum Reason {
        INSTRUCTION_OVERRIDE("Request blocked: it asks the assistant to ignore its instructions"),
        PROMPT_EXFILTRATION("Request blocked: it asks for the assistant's hidden instructions"),
        ROLE_HIJACK("Request blocked: it tries to change what the assistant is"),
        TEMPLATE_TOKENS("Request blocked: it contains chat-template control tokens"),
        CONTROL_CHARACTERS("Request blocked: it contains control characters"),
        NOISE("Request blocked: it is not readable text");

        private final String userMessage;

        Reason(String userMessage) {
            this.userMessage = userMessage;
        }

        public String userMessage() {
            return userMessage;
        }
    }

    private record Rule(Reason reason, Pattern pattern) { }

    private static final List<Rule> RULES = List.of(
            // "ignore/disregard/forget/override ... (previous|your|all|the) instructions|rules|prompt|guidelines"
            new Rule(Reason.INSTRUCTION_OVERRIDE, Pattern.compile(
                    "\\b(ignore|disregard|forget|override|bypass)\\b[^.!?\\n]{0,40}\\b(instructions?|rules?|prompts?|guidelines?|directions?|constraints?|policy|policies)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new Rule(Reason.INSTRUCTION_OVERRIDE, Pattern.compile(
                    "\\b(new|updated|real|actual)\\s+(instructions?|rules?|system\\s+prompt)\\s*:",
                    Pattern.CASE_INSENSITIVE)),
            new Rule(Reason.PROMPT_EXFILTRATION, Pattern.compile(
                    "\\b(reveal|show|print|repeat|output|display|leak|dump|tell me)\\b[^.!?\\n]{0,40}\\b(system\\s+prompt|hidden\\s+(prompt|instructions?)|initial\\s+(prompt|instructions?)|your\\s+(instructions?|rules|guidelines|configuration))\\b",
                    Pattern.CASE_INSENSITIVE)),
            new Rule(Reason.ROLE_HIJACK, Pattern.compile(
                    "\\b(you are now|from now on you are|act as|pretend (to be|you are)|roleplay as|simulate being)\\b[^.!?\\n]{0,60}\\b(an? )?(unrestricted|uncensored|jailbroken|different|new|evil|system|developer|admin|root|dan\\b|gpt|assistant without)",
                    Pattern.CASE_INSENSITIVE)),
            new Rule(Reason.ROLE_HIJACK, Pattern.compile(
                    "\\b(developer\\s+mode|jailbreak|do anything now|DAN\\s+mode)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new Rule(Reason.TEMPLATE_TOKENS, Pattern.compile(
                    "(<\\|im_(start|end)\\|>|<\\|(system|user|assistant|endoftext)\\|>|\\[/?INST]|<<SYS>>|<\\|start_header_id\\|>|###\\s*(system|instruction)\\s*:|^\\s*system\\s*:\\s*$)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE))
    );

    /** C0/C1 controls except tab/newline, zero-width and bidi override characters, BOM. */
    private static final Pattern CONTROL_CHARS = Pattern.compile(
            "[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]");

    private final int maxRepeatedChars;

    public PromptGuard() {
        this(40);
    }

    public PromptGuard(int maxRepeatedChars) {
        this.maxRepeatedChars = maxRepeatedChars;
    }

    public Decision check(String text) {
        if (CONTROL_CHARS.matcher(text).find()) return new Decision.Blocked(Reason.CONTROL_CHARACTERS);
        if (isNoise(text)) return new Decision.Blocked(Reason.NOISE);
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(text).find()) return new Decision.Blocked(rule.reason());
        }
        return Decision.allowed();
    }

    /** Runs of one character (e.g. 200 × "a") or text with almost no letters. */
    private boolean isNoise(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return true;
        int run = 1;
        for (int i = 1; i < trimmed.length(); i++) {
            run = trimmed.charAt(i) == trimmed.charAt(i - 1) ? run + 1 : 1;
            if (run > maxRepeatedChars) return true;
        }
        if (trimmed.length() >= 20) {
            long readable = trimmed.chars().filter(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c)).count();
            if ((double) readable / trimmed.length() < 0.5) return true;
        }
        return false;
    }
}
