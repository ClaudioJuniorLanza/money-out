package com.moneyout.sharedkernel

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigInteger

/** Cobre ACs 4–6 de "Dinheiro seguro" e os três edge cases da spec (F0-02). */
class MoneyAllocateTest {
    private fun brl(minorUnits: Long) = Money(minorUnits, Currency.BRL)

    // Bordas explícitas misturadas ao intervalo completo (negativos, zero, MIN_VALUE, MAX_VALUE).
    private val totals: Arb<Long> =
        Arb.choice(
            Arb.long(),
            Arb.element(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE + 1),
        )
    private val ratiosArb: Arb<List<Int>> =
        Arb.list(
            Arb.choice(Arb.int(1..1_000), Arb.element(1, Int.MAX_VALUE)),
            1..12,
        )

    private fun big(value: Long) = BigInteger.valueOf(value)

    @Test
    fun `Should split R$ 100,00 in 3 equal parts as 33,34 plus 33,33 plus 33,33`() {
        /** @Dado R$ 100,00 e 3 ratios iguais */
        val total = brl(10_000)

        /** @Quando o valor é alocado */
        val parts = total.allocate(listOf(1, 1, 1))

        /** @Então o centavo que sobra vai para a primeira parte */
        parts shouldBe listOf(brl(3_334), brl(3_333), brl(3_333))
    }

    @Test
    fun `Should apply the same contract with sign when the total is negative`() {
        /** @Dado um total negativo e 3 ratios iguais */
        val total = brl(-10_000)

        /** @Quando o valor é alocado */
        val parts = total.allocate(listOf(1, 1, 1))

        /** @Então o centavo que sobra vai para a primeira parte, com sinal */
        parts shouldBe listOf(brl(-3_334), brl(-3_333), brl(-3_333))
    }

    @Test
    fun `Should not give an extra minor unit to a part whose exact fraction is already whole`() {
        /** @Dado 2 minor units e ratios [2, 1, 1], em que a primeira parte já é exata (1) */
        val total = brl(2)

        /** @Quando o valor é alocado */
        val parts = total.allocate(listOf(2, 1, 1))

        /** @Então o centavo que sobra vai para a primeira parte fracionária, não para a exata */
        parts shouldBe listOf(brl(1), brl(1), brl(0))
    }

    @Test
    fun `Should allocate without overflow when total times ratio exceeds Long`() {
        /** @Dado Long.MAX_VALUE e ratios que fazem total * ratio passar de Long */
        val total = brl(Long.MAX_VALUE)

        /** @Quando o valor é alocado */
        val parts = total.allocate(listOf(Int.MAX_VALUE, Int.MAX_VALUE))

        /** @Então a soma continua exatamente o total */
        parts.sumOf { it.minorUnits.toBigInteger() } shouldBe big(Long.MAX_VALUE)

        /** @E as partes diferem em no máximo 1 */
        parts shouldBe listOf(brl(4_611_686_018_427_387_904), brl(4_611_686_018_427_387_903))
    }

    @Test
    fun `Should allocate Long MIN_VALUE without overflow`() {
        /** @Dado o menor valor representável */
        val total = brl(Long.MIN_VALUE)

        /** @Quando é alocado em uma parte e em duas partes iguais */
        val whole = total.allocate(listOf(1))
        val halves = total.allocate(listOf(1, 1))

        /** @Então a parte única é o próprio total */
        whole shouldBe listOf(brl(Long.MIN_VALUE))

        /** @E as metades somam exatamente o total */
        halves shouldBe listOf(brl(Long.MIN_VALUE / 2), brl(Long.MIN_VALUE / 2))
    }

    @Test
    fun `Should throw DomainException when ratios are empty`() {
        /** @Dado um valor a alocar */
        val total = brl(100)

        /** @Então DomainException é lançada */
        shouldThrow<DomainException> {
            /** @Quando a lista de ratios é vazia */
            total.allocate(emptyList())
        }
    }

    @Test
    fun `Should throw DomainException when a ratio is zero or negative`() {
        /** @Dado um valor a alocar */
        val total = brl(100)

        /** @Então DomainException é lançada para ratio zero */
        shouldThrow<DomainException> {
            /** @Quando um ratio é zero */
            total.allocate(listOf(1, 0))
        }

        /** @E também para ratio negativo */
        shouldThrow<DomainException> {
            /** @Quando um ratio é negativo */
            total.allocate(listOf(1, -3))
        }
    }

    @Test
    fun `Should keep the sum of the parts equal to the total`() =
        runBlocking<Unit> {
            /** @Dado qualquer total (incluindo bordas) e qualquer lista de ratios positivos */
            checkAll(totals, ratiosArb) { total, ratios ->
                /** @Quando o total é alocado */
                val parts = brl(total).allocate(ratios)

                /** @Então nenhum centavo é perdido ou criado */
                parts.sumOf { it.minorUnits.toBigInteger() } shouldBe big(total)
            }
        }

    @Test
    fun `Should return as many parts as ratios`() =
        runBlocking<Unit> {
            /** @Dado qualquer total (incluindo bordas) e qualquer lista de ratios positivos */
            checkAll(totals, ratiosArb) { total, ratios ->
                /** @Quando o total é alocado */
                val parts = brl(total).allocate(ratios)

                /** @Então o tamanho da lista é o número de ratios */
                parts.size shouldBe ratios.size
            }
        }

    @Test
    fun `Should keep each part within 1 minor unit of the exact fraction`() =
        runBlocking<Unit> {
            /** @Dado qualquer total (incluindo bordas) e qualquer lista de ratios positivos */
            checkAll(totals, ratiosArb) { total, ratios ->
                /** @Quando o total é alocado */
                val sum = ratios.sumOf { it.toLong() }.toBigInteger()
                val parts = brl(total).allocate(ratios)

                /** @Então |parte - total*ratio/soma| < 1, em aritmética inteira: |parte*soma - total*ratio| < soma */
                parts.zip(ratios).forEach { (part, ratio) ->
                    val error = (part.minorUnits.toBigInteger() * sum - big(total) * ratio.toBigInteger()).abs()
                    (error < sum) shouldBe true
                }
            }
        }

    @Test
    fun `Should produce parts differing by at most 1 when ratios are equal`() =
        runBlocking<Unit> {
            /** @Dado qualquer total, uma quantidade de partes e um mesmo ratio repetido */
            checkAll(totals, Arb.int(1..50), Arb.int(1..1_000)) { total, count, ratio ->
                /** @Quando o total é alocado */
                val parts = brl(total).allocate(List(count) { ratio }).map { it.minorUnits.toBigInteger() }

                /** @Então a diferença entre a maior e a menor parte é de no máximo 1 */
                (parts.max() - parts.min() <= BigInteger.ONE) shouldBe true
            }
        }

    @Test
    fun `Should always throw DomainException for invalid ratios`() =
        runBlocking<Unit> {
            /** @Dado qualquer total e uma lista de ratios vazia ou com algum ratio não positivo */
            val invalid = Arb.list(Arb.int(Int.MIN_VALUE..Int.MAX_VALUE), 1..8).filterInvalid()

            checkAll(totals, invalid) { total, ratios ->
                /** @Então DomainException é lançada */
                shouldThrow<DomainException> {
                    /** @Quando o total é alocado */
                    brl(total).allocate(ratios)
                }
            }
        }

    private fun Arb<List<Int>>.filterInvalid(): Arb<List<Int>> =
        Arb.choice(
            Arb.constant(emptyList()),
            this.filter { list -> list.any { it <= 0 } },
        )
}
