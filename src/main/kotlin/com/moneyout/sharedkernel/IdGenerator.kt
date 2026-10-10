package com.moneyout.sharedkernel

import java.util.UUID

/**
 * Porta de saída: o domínio pede um identificador novo sem saber como ele é gerado.
 * A implementação (ex.: UUIDv7) mora na infraestrutura.
 */
fun interface IdGenerator {
    fun next(): UUID
}
