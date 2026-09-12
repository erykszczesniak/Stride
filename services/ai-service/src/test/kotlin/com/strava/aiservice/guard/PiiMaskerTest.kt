package com.strava.aiservice.guard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PiiMasker")
class PiiMaskerTest {

    private val masker = PiiMasker()

    @Test
    @DisplayName("masks email addresses")
    fun masksEmail() {
        val r = masker.mask("send the plan to john.doe+run@example.co.uk please")
        assertThat(r.text).isEqualTo("send the plan to [email] please")
        assertThat(r.counts).containsEntry(PiiMasker.Type.EMAIL, 1)
    }

    @Test
    @DisplayName("masks international and local phone numbers")
    fun masksPhones() {
        assertThat(masker.mask("call me at +48 600 100 200").text).isEqualTo("call me at [phone]")
        assertThat(masker.mask("or +1-555-123-4567 later").text).isEqualTo("or [phone] later")
        assertThat(masker.mask("my number is 600100200").text).isEqualTo("my number is [phone]")
    }

    @Test
    @DisplayName("masks card numbers only when they pass Luhn")
    fun masksCardsWithLuhn() {
        // 4111 1111 1111 1111 is the classic Luhn-valid test PAN.
        assertThat(masker.mask("card 4111 1111 1111 1111 expires soon").text).isEqualTo("card [card] expires soon")
        // Same shape, Luhn-invalid: an id, not a card.
        val r = masker.mask("upload id 4111 1111 1111 1112")
        assertThat(r.counts).doesNotContainKey(PiiMasker.Type.CARD)
    }

    @Test
    @DisplayName("masks IBANs that pass the mod-97 check")
    fun masksIban() {
        assertThat(masker.mask("pay to GB82 WEST 1234 5698 7654 32").text).isEqualTo("pay to [iban]")
        assertThat(masker.mask("pay to PL61109010140000071219812874").text).isEqualTo("pay to [iban]")
        // Wrong check digits → left alone.
        assertThat(masker.mask("code GB00 WEST 1234 5698 7654 32").counts).doesNotContainKey(PiiMasker.Type.IBAN)
    }

    @Test
    @DisplayName("leaves training data untouched: distances, paces, heart rate, times, dates")
    fun keepsTrainingNumbers() {
        val text = "10.5 km in 55:30 at 5:17/km, 165 bpm avg, 1200 m climb, 2026-05-13 17:15, 21097 m half marathon"
        val r = masker.mask(text)
        assertThat(r.text).isEqualTo(text)
        assertThat(r.total).isZero()
    }

    @Test
    @DisplayName("counts every occurrence by type")
    fun countsByType() {
        val r = masker.mask("a@b.io and c@d.io, +48 600 100 200")
        assertThat(r.counts).containsEntry(PiiMasker.Type.EMAIL, 2).containsEntry(PiiMasker.Type.PHONE, 1)
        assertThat(r.total).isEqualTo(3)
    }
}
