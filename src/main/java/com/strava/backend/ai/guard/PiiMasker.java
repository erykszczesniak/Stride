package com.strava.backend.ai.guard;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Masks personal identifiers before text leaves the application (to a
 * provider) and before it is persisted. The coach never needs an email
 * address, a phone number, a card number or an IBAN to reason about a
 * workout, so the masked form loses nothing the product uses. Distances,
 * paces, heart rates and times ("5:30/km", "10.5 km", "165 bpm") must survive
 * untouched — the tests pin those.
 *
 * <p>Card numbers are only masked when they pass the Luhn check, so a
 * 16-digit activity id or a Strava upload id does not get eaten.
 */
public class PiiMasker {

    public enum Type {
        EMAIL("[email]"),
        PHONE("[phone]"),
        CARD("[card]"),
        IBAN("[iban]");

        private final String placeholder;

        Type(String placeholder) {
            this.placeholder = placeholder;
        }

        public String placeholder() {
            return placeholder;
        }
    }

    public record Result(String text, Map<Type, Integer> counts) {
        public int total() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // IBAN first: it contains digit runs a phone/card pattern would otherwise partially eat.
    private static final Pattern IBAN = Pattern.compile("\\b[A-Z]{2}\\d{2}(?:[ \\-]?[A-Z0-9]{4}){2,7}(?:[ \\-]?[A-Z0-9]{1,4})?\\b");

    // 13–19 digits with optional spaces/dashes between groups; validated with Luhn.
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ \\-]?){12,18}\\d\\b");

    // International (+48 600 100 200, +1-555-123-4567) or local 9–11 digit groups.
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\d.:/])(?:\\+\\d{1,3}[ \\-]?)?(?:\\(?\\d{2,4}\\)?[ \\-]?)?\\d{3}[ \\-]?\\d{3}[ \\-]?\\d{2,4}(?![\\d.:/])");

    public Result mask(String text) {
        Map<Type, Integer> counts = new EnumMap<>(Type.class);
        String out = text;
        out = EMAIL.matcher(out).replaceAll(m -> {
            counts.merge(Type.EMAIL, 1, Integer::sum);
            return Type.EMAIL.placeholder();
        });
        out = IBAN.matcher(out).replaceAll(m -> {
            if (looksLikeIban(m.group())) {
                counts.merge(Type.IBAN, 1, Integer::sum);
                return Type.IBAN.placeholder();
            }
            return m.group();
        });
        out = CARD.matcher(out).replaceAll(m -> {
            String digits = digitsOf(m.group());
            if (digits.length() >= 13 && digits.length() <= 19 && luhn(digits)) {
                counts.merge(Type.CARD, 1, Integer::sum);
                return Type.CARD.placeholder();
            }
            return m.group();
        });
        out = PHONE.matcher(out).replaceAll(m -> {
            String digits = digitsOf(m.group());
            if (digits.length() >= 9 && digits.length() <= 13) {
                counts.merge(Type.PHONE, 1, Integer::sum);
                return Type.PHONE.placeholder();
            }
            return m.group();
        });
        return new Result(out, counts);
    }

    private static String digitsOf(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private static boolean looksLikeIban(String candidate) {
        String compact = candidate.replace(" ", "").replace("-", "");
        if (compact.length() < 15 || compact.length() > 34) return false;
        // mod-97 check per ISO 13616
        String rearranged = compact.substring(4) + compact.substring(0, 4);
        int remainder = 0;
        for (char ch : rearranged.toCharArray()) {
            String value = Character.isDigit(ch) ? String.valueOf(ch) : String.valueOf(ch - 'A' + 10);
            for (char d : value.toCharArray()) {
                remainder = (remainder * 10 + (d - '0')) % 97;
            }
        }
        return remainder == 1;
    }

    private static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }
}
