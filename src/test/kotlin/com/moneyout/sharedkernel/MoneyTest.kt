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
    fun `somar devolve o resultado exato em minor units`() {
        /** @Quando dois valores BRL são somados */
        val result = brl(10_050) + brl(1_295)

        /** @Então o resultado é a soma exata em centavos */
        result shouldBe brl(11_345)
    }

    @Test
    fun `subtrair devolve o resultado exato e pode ficar negativo`() {
        /** @Quando um valor maior é subtraído de um menor */
        val result = brl(1_000) - brl(2_500)

        /** @Então o resultado é -15,00 */
        result shouldBe brl(-1_500)
    }

    @Test
    fun `somar alem de Long MAX_VALUE lanca DomainException`() {
        /** @Quando a soma estoura Long, @Então DomainException em vez de dar a volta */
        shouldThrow<DomainException> { brl(Long.MAX_VALUE) + brl(1) }
    }

    @Test
    fun `subtrair abaixo de Long MIN_VALUE lanca DomainException`() {
        shouldThrow<DomainException> { brl(Long.MIN_VALUE) - brl(1) }
    }

    @Test
    fun `negar Long MIN_VALUE lanca DomainException`() {
        shouldThrow<DomainException> { -brl(Long.MIN_VALUE) }
    }

    @Test
    fun `negar inverte o sinal`() {
        (-brl(500)) shouldBe brl(-500)
        (-brl(-500)) shouldBe brl(500)
        (-brl(Long.MAX_VALUE)) shouldBe brl(-Long.MAX_VALUE)
    }

    @Test
    fun `zero devolve Money de zero na moeda informada`() {
        Money.zero(Currency.BRL) shouldBe brl(0)
        Money.zero(Currency.BRL).isZero() shouldBe true
    }

    @Test
    fun `isZero isPositive e isNegative classificam o sinal`() {
        brl(0).isZero() shouldBe true
        brl(0).isPositive() shouldBe false
        brl(0).isNegative() shouldBe false

        brl(1).isZero() shouldBe false
        brl(1).isPositive() shouldBe true
        brl(1).isNegative() shouldBe false

        brl(-1).isZero() shouldBe false
        brl(-1).isPositive() shouldBe false
        brl(-1).isNegative() shouldBe true
    }

    @Test
    fun `compareTo ordena por minor units`() {
        (brl(100) < brl(101)) shouldBe true
        (brl(-1) < brl(0)) shouldBe true
        brl(100).compareTo(brl(100)) shouldBe 0
        listOf(brl(300), brl(-5), brl(0)).sorted() shouldBe listOf(brl(-5), brl(0), brl(300))
    }

    @Test
    fun `somar e subtrair o mesmo valor devolve o original`() =
        runBlocking {
            /** @Dado quaisquer valores cuja soma não estoura Long */
            val safe = Arb.long(-Long.MAX_VALUE / 2, Long.MAX_VALUE / 2)

            checkAll(safe, safe) { a, b ->
                /** @Então (a + b) - b == a */
                ((brl(a) + brl(b)) - brl(b)) shouldBe brl(a)
            }
        }
}
