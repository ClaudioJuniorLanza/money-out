package com.moneyout.sharedkernel

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
