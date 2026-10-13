package com.moneyout.sharedkernel

/**
 * Moedas aceitas pelo sistema (ADR-0002). Só BRL por ora; câmbio está fora de escopo.
 * [minorUnitDigits] define a escala: BRL tem 2 casas (centavos).
 */
enum class Currency(
    val code: String,
    val numericCode: Int,
    val symbol: String,
    val minorUnitDigits: Int,
) {
    BRL(code = "BRL", numericCode = 986, symbol = "R$", minorUnitDigits = 2),
    ;

    /** Quantas unidades mínimas cabem em uma unidade principal (ex.: 100 centavos por real). */
    val minorUnitsPerMajor: Long
        get() = generateSequence(1L) { it * 10 }.elementAt(minorUnitDigits)
}
