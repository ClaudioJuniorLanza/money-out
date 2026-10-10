package com.moneyout.shared.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CurrencyTest {
    @Test
    fun `BRL usa centavos`() {
        Currency.BRL.minorUnitDigits shouldBe 2
        Currency.BRL.minorUnitsPerMajor shouldBe 100L
    }

    @Test
    fun `BRL tem codigo ISO 4217`() {
        Currency.BRL.code shouldBe "BRL"
        Currency.BRL.numericCode shouldBe 986
    }
}
