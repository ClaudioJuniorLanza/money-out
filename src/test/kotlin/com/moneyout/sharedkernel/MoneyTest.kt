package com.moneyout.sharedkernel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Cobre os ACs 1–3 de "Dinheiro seguro" (F0-01).
 *
 * AC 2 (moedas diferentes) **não tem teste**: o `Currency` só tem BRL (ADR-0002, 2ª moeda fora do escopo), então
 * é impossível construir dois `Money` de moedas distintas sem inventar uma moeda só para testar. O guard
 * `requireSameCurrency` existe em `Money`; o teste entra junto com a 2ª moeda (premissa da spec).
 */
class MoneyTest {
    private fun brl(minorUnits: Long) = Money(minorUnits, Currency.BRL)

    @Test
    fun `Should add exactly in minor units`() {
        /** @Dado dois valores BRL */
        val a = brl(10_050)
        val b = brl(1_295)

        /** @Quando são somados */
        val result = a + b

        /** @Então o resultado é a soma exata em centavos */
        result shouldBe brl(11_345)
    }

    @Test
    fun `Should subtract exactly and allow a negative result`() {
        /** @Dado um valor menor e um maior */
        val smaller = brl(1_000)
        val larger = brl(2_500)

        /** @Quando o maior é subtraído do menor */
        val result = smaller - larger

        /** @Então o resultado é -15,00 */
        result shouldBe brl(-1_500)
    }

    @Test
    fun `Should throw DomainException when adding beyond Long MAX_VALUE`() {
        /** @Dado o maior valor representável */
        val max = brl(Long.MAX_VALUE)

        /** @Então DomainException é lançada em vez de dar a volta */
        shouldThrow<DomainException> {
            /** @Quando 1 centavo é somado */
            max + brl(1)
        }
    }

    @Test
    fun `Should throw DomainException when subtracting below Long MIN_VALUE`() {
        /** @Dado o menor valor representável */
        val min = brl(Long.MIN_VALUE)

        /** @Então DomainException é lançada em vez de dar a volta */
        shouldThrow<DomainException> {
            /** @Quando 1 centavo é subtraído */
            min - brl(1)
        }
    }

    @Test
    fun `Should throw DomainException when negating Long MIN_VALUE`() {
        /** @Dado o menor valor representável, cujo oposto não cabe em Long */
        val min = brl(Long.MIN_VALUE)

        /** @Então DomainException é lançada */
        shouldThrow<DomainException> {
            /** @Quando o valor é negado */
            -min
        }
    }

    @Test
    fun `Should invert the sign when negating`() {
        /** @Dado valores positivo, negativo e no limite superior */
        val positive = brl(500)
        val negative = brl(-500)
        val max = brl(Long.MAX_VALUE)

        /** @Quando cada um é negado */
        val negatedPositive = -positive
        val negatedNegative = -negative
        val negatedMax = -max

        /** @Então o sinal é invertido */
        negatedPositive shouldBe brl(-500)

        /** @E negar um negativo devolve o positivo */
        negatedNegative shouldBe brl(500)

        /** @E negar o máximo devolve o oposto exato */
        negatedMax shouldBe brl(-Long.MAX_VALUE)
    }

    @Test
    fun `Should create a zero Money in the given currency`() {
        /** @Quando um Money zero em BRL é criado */
        val zero = Money.zero(Currency.BRL)

        /** @Então ele é igual a zero centavos */
        zero shouldBe brl(0)

        /** @E isZero é verdadeiro */
        zero.isZero() shouldBe true
    }

    @Test
    fun `Should classify the sign with isZero, isPositive and isNegative`() {
        /** @Dado valores zero, positivo e negativo */
        val zero = brl(0)
        val positive = brl(1)
        val negative = brl(-1)

        /** @Então zero só é zero */
        listOf(zero.isZero(), zero.isPositive(), zero.isNegative()) shouldBe listOf(true, false, false)

        /** @E o positivo só é positivo */
        listOf(positive.isZero(), positive.isPositive(), positive.isNegative()) shouldBe listOf(false, true, false)

        /** @E o negativo só é negativo */
        listOf(negative.isZero(), negative.isPositive(), negative.isNegative()) shouldBe listOf(false, false, true)
    }

    @Test
    fun `Should compare by minor units`() {
        /** @Dado valores BRL desordenados */
        val values = listOf(brl(300), brl(-5), brl(0))

        /** @Quando são ordenados */
        val sorted = values.sorted()

        /** @Então a ordem é crescente por minor units */
        sorted shouldBe listOf(brl(-5), brl(0), brl(300))

        /** @E valores iguais comparam como 0 */
        brl(100).compareTo(brl(100)) shouldBe 0

        /** @E um centavo a mais é maior */
        (brl(100) < brl(101)) shouldBe true
    }

    @Test
    fun `Should return the original value when adding and subtracting the same amount`() =
        runBlocking<Unit> {
            /** @Dado quaisquer valores cuja soma não estoura Long */
            val safe = Arb.long(-Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)

            checkAll(safe, safe) { a, b ->
                /** @Quando b é somado e depois subtraído de a */
                val result = (brl(a) + brl(b)) - brl(b)

                /** @Então o resultado é a */
                result shouldBe brl(a)
            }
        }
}
