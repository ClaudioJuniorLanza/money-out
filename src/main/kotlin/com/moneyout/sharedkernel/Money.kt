package com.moneyout.sharedkernel

import java.math.BigInteger

/**
 * Valor monetário exato: inteiro em unidade mínima ([minorUnits], centavos no BRL) + [currency] (ADR-0002).
 *
 * `Long` **assinado** porque o mesmo tipo representa saldo, estorno e lançamento de débito/crédito; a regra
 * "valor > 0" de um pagamento pertence ao agregado que o recebe, não ao VO. Overflow nunca dá a volta
 * silenciosamente: toda aritmética usa `Math.*Exact` e converte o estouro em [DomainException].
 */
data class Money(
    val minorUnits: Long,
    val currency: Currency,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(exact("soma") { Math.addExact(minorUnits, other.minorUnits) }, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(exact("subtracao") { Math.subtractExact(minorUnits, other.minorUnits) }, currency)
    }

    /** `-Long.MIN_VALUE` não cabe em `Long`; sem `negateExact` o resultado seria o próprio `MIN_VALUE`. */
    operator fun unaryMinus(): Money = Money(exact("negacao") { Math.negateExact(minorUnits) }, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    fun isZero(): Boolean = minorUnits == 0L

    fun isPositive(): Boolean = minorUnits > 0L

    fun isNegative(): Boolean = minorUnits < 0L

    /**
     * Divide o valor proporcionalmente a [ratios] sem perder nem criar centavo: a soma das partes é exatamente
     * este valor. Cada parte é o piso da fração exata e o resto (sempre menor que o número de partes) é
     * distribuído de 1 em 1 minor unit a partir das primeiras partes; assim cada parte difere da fração
     * exata em menos de 1 minor unit.
     *
     * O cálculo é feito sobre o módulo em [BigInteger] e o sinal é aplicado no fim: `total * ratio` pode
     * passar de `Long` (e `|Long.MIN_VALUE|` também), então nunca multiplicamos em `Long`.
     */
    fun allocate(ratios: List<Int>): List<Money> {
        if (ratios.isEmpty()) throw DomainException("allocate exige ao menos um ratio")
        if (ratios.any { it <= 0 }) throw DomainException("allocate exige ratios maiores que zero")

        val magnitude = BigInteger.valueOf(minorUnits).abs()
        val ratiosSum = BigInteger.valueOf(ratios.sumOf { it.toLong() })
        val floors = ratios.map { magnitude.multiply(BigInteger.valueOf(it.toLong())).divide(ratiosSum) }
        val remainder = magnitude.subtract(floors.fold(BigInteger.ZERO, BigInteger::add)).toInt()
        val sign = if (minorUnits < 0) -1 else 1

        return floors.mapIndexed { index, floor ->
            val part = if (index < remainder) floor.add(BigInteger.ONE) else floor
            Money(part.multiply(BigInteger.valueOf(sign.toLong())).longValueExact(), currency)
        }
    }

    private fun requireSameCurrency(other: Money) {
        if (currency != other.currency) throw DomainException("Operacao entre moedas diferentes")
    }

    private inline fun exact(
        operation: String,
        block: () -> Long,
    ): Long =
        try {
            block()
        } catch (_: ArithmeticException) {
            throw DomainException("Overflow de Long na $operation de Money")
        }

    companion object {
        fun zero(currency: Currency): Money = Money(0L, currency)
    }
}
