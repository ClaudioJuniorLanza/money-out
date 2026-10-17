package com.moneyout.sharedkernel

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CurrencyTest {
    @Test
    fun `Should use cents for BRL`() {
        /** @Dado a moeda BRL */
        val currency = Currency.BRL

        /** @Então ela tem duas casas decimais */
        currency.minorUnitDigits shouldBe 2

        /** @E 100 minor units por unidade maior */
        currency.minorUnitsPerMajor shouldBe 100L
    }

    @Test
    fun `Should expose the ISO 4217 codes for BRL`() {
        /** @Dado a moeda BRL */
        val currency = Currency.BRL

        /** @Então o código alfabético é BRL */
        currency.code shouldBe "BRL"

        /** @E o código numérico é 986 */
        currency.numericCode shouldBe 986
    }
}
