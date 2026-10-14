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
    fun `R$ 100,00 em 3 partes iguais vira 33,34 mais 33,33 mais 33,33`() {
        /** @Quando R$ 100,00 é dividido em 3 ratios iguais */
        val parts = brl(10_000).allocate(listOf(1, 1, 1))

        /** @Então o centavo que sobra vai para a primeira parte */
        parts shouldBe listOf(brl(3_334), brl(3_333), brl(3_333))
    }

    @Test
    fun `total negativo aplica o mesmo contrato com sinal`() {
        brl(-10_000).allocate(listOf(1, 1, 1)) shouldBe listOf(brl(-3_334), brl(-3_333), brl(-3_333))
    }

    @Test
    fun `total vezes ratio que estoura Long e calculado sem overflow`() {
        /** @Quando Long.MAX_VALUE é dividido com ratios que fazem total * ratio passar de Long */
        val parts = brl(Long.MAX_VALUE).allocate(listOf(Int.MAX_VALUE, Int.MAX_VALUE))

        /** @Então a soma continua exatamente o total e as partes diferem em no máximo 1 */
        parts.sumOf { it.minorUnits.toBigInteger() } shouldBe big(Long.MAX_VALUE)
        parts shouldBe listOf(brl(4_611_686_018_427_387_904), brl(4_611_686_018_427_387_903))
    }

    @Test
    fun `Long MIN_VALUE e alocavel sem overflow`() {
        val parts = brl(Long.MIN_VALUE).allocate(listOf(1))
        parts shouldBe listOf(brl(Long.MIN_VALUE))

        val halves = brl(Long.MIN_VALUE).allocate(listOf(1, 1))
        halves shouldBe listOf(brl(Long.MIN_VALUE / 2), brl(Long.MIN_VALUE / 2))
    }

    @Test
    fun `ratios vazio lanca DomainException`() {
        shouldThrow<DomainException> { brl(100).allocate(emptyList()) }
    }

    @Test
    fun `ratio zero ou negativo lanca DomainException`() {
        shouldThrow<DomainException> { brl(100).allocate(listOf(1, 0)) }
        shouldThrow<DomainException> { brl(100).allocate(listOf(1, -3)) }
    }

    @Test
    fun `propriedade - a soma das partes e exatamente o total`() =
        runBlocking<Unit> {
            checkAll(totals, ratiosArb) { total, ratios ->
                /** @Então nenhum centavo é perdido ou criado */
                brl(total).allocate(ratios).sumOf { it.minorUnits.toBigInteger() } shouldBe big(total)
            }
        }

    @Test
    fun `propriedade - o tamanho da lista e o numero de ratios`() =
        runBlocking<Unit> {
            checkAll(totals, ratiosArb) { total, ratios ->
                brl(total).allocate(ratios).size shouldBe ratios.size
            }
        }

    @Test
    fun `propriedade - cada parte difere da fracao exata em menos de 1 minor unit`() =
        runBlocking<Unit> {
            checkAll(totals, ratiosArb) { total, ratios ->
                val sum = ratios.sumOf { it.toLong() }.toBigInteger()
                brl(total).allocate(ratios).zip(ratios).forEach { (part, ratio) ->
                    /** @Então |parte - total*ratio/soma| < 1, em aritmética inteira: |parte*soma - total*ratio| < soma */
                    val error = (part.minorUnits.toBigInteger() * sum - big(total) * ratio.toBigInteger()).abs()
                    (error < sum) shouldBe true
                }
            }
        }

    @Test
    fun `propriedade - ratios iguais geram partes que diferem em no maximo 1`() =
        runBlocking<Unit> {
            checkAll(totals, Arb.int(1..50), Arb.int(1..1_000)) { total, count, ratio ->
                val parts = brl(total).allocate(List(count) { ratio }).map { it.minorUnits.toBigInteger() }
                val spread = parts.max() - parts.min()
                (spread <= BigInteger.ONE) shouldBe true
            }
        }

    @Test
    fun `propriedade - entrada invalida sempre lanca DomainException`() =
        runBlocking<Unit> {
            val invalid =
                Arb
                    .list(Arb.int(Int.MIN_VALUE..Int.MAX_VALUE), 1..8)
                    .filterInvalid()
            checkAll(totals, invalid) { total, ratios ->
                shouldThrow<DomainException> { brl(total).allocate(ratios) }
            }
        }

    private fun Arb<List<Int>>.filterInvalid(): Arb<List<Int>> =
        Arb.choice(
            Arb.constant(emptyList()),
            this.filter { list -> list.any { it <= 0 } },
        )
}
