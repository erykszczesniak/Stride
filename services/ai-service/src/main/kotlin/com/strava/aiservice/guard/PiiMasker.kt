package com.strava.aiservice.guard

/**
 * Masks personal identifiers before text leaves the service (to a provider)
 * and before it is persisted. The coach never needs an email address, a
 * phone number, a card number or an IBAN to reason about a workout, so the
 * masked form loses nothing the product uses. Distances, paces, heart rates
 * and times ("5:30/km", "10.5 km", "165 bpm") must survive untouched — the
 * tests pin those.
 *
 * Card numbers are only masked when they pass the Luhn check, so a 16-digit
 * activity id or a Strava upload id does not get eaten.
 */
class PiiMasker {

    enum class Type(val placeholder: String) {
        EMAIL("[email]"),
        PHONE("[phone]"),
        CARD("[card]"),
        IBAN("[iban]"),
    }

    data class Result(val text: String, val counts: Map<Type, Int>) {
        val total: Int get() = counts.values.sum()
    }

    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    // IBAN first: it contains digit runs a phone/card pattern would otherwise partially eat.
    private val iban = Regex("""\b[A-Z]{2}\d{2}(?:[ \-]?[A-Z0-9]{4}){2,7}(?:[ \-]?[A-Z0-9]{1,4})?\b""")

    // 13–19 digits with optional spaces/dashes between groups; validated with Luhn.
    private val card = Regex("""\b(?:\d[ \-]?){12,18}\d\b""")

    // International (+48 600 100 200, +1-555-123-4567) or local 9–11 digit groups.
    private val phone = Regex("""(?<![\d.:/])(?:\+\d{1,3}[ \-]?)?(?:\(?\d{2,4}\)?[ \-]?)?\d{3}[ \-]?\d{3}[ \-]?\d{2,4}(?![\d.:/])""")

    fun mask(text: String): Result {
        val counts = mutableMapOf<Type, Int>()
        var out = text
        out = email.replace(out) { counts.merge(Type.EMAIL, 1, Int::plus); Type.EMAIL.placeholder }
        out = iban.replace(out) { m ->
            if (looksLikeIban(m.value)) { counts.merge(Type.IBAN, 1, Int::plus); Type.IBAN.placeholder } else m.value
        }
        out = card.replace(out) { m ->
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 13..19 && luhn(digits)) { counts.merge(Type.CARD, 1, Int::plus); Type.CARD.placeholder } else m.value
        }
        out = phone.replace(out) { m ->
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 9..13) { counts.merge(Type.PHONE, 1, Int::plus); Type.PHONE.placeholder } else m.value
        }
        return Result(out, counts)
    }

    private fun looksLikeIban(candidate: String): Boolean {
        val compact = candidate.replace(" ", "").replace("-", "")
        if (compact.length !in 15..34) return false
        // mod-97 check per ISO 13616
        val rearranged = compact.substring(4) + compact.substring(0, 4)
        var remainder = 0
        for (ch in rearranged) {
            val value = if (ch.isDigit()) ch.toString() else (ch - 'A' + 10).toString()
            for (d in value) remainder = (remainder * 10 + (d - '0')) % 97
        }
        return remainder == 1
    }

    private fun luhn(digits: String): Boolean {
        var sum = 0
        var double = false
        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (double) { d *= 2; if (d > 9) d -= 9 }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }
}
