package com.moneyout.shared.domain

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

/**
 * Gera UUIDv7 (RFC 9562): 48 bits de timestamp em ms + versão + bits aleatórios.
 *
 * Por que v7 e não v4 como chave primária? IDs v7 crescem com o tempo, então os inserts caem no fim
 * do índice B-tree (boa localidade de cache e menos page splits), enquanto v4 espalha os inserts.
 *
 * Limite conhecido: dentro do mesmo milissegundo a ordem não é garantida (a RFC permite).
 * O tempo vem de um [Clock] injetado, nunca de `Instant.now()`.
 */
class UuidV7Generator(
    private val clock: Clock,
    private val random: SecureRandom = SecureRandom(),
) {
    fun generate(): UUID {
        val millis = clock.millis() and TIMESTAMP_MASK
        val randA = random.nextLong() and RAND_A_MASK
        val randB = random.nextLong() and RAND_B_MASK

        val mostSignificant = (millis shl 16) or VERSION_7 or randA
        val leastSignificant = VARIANT_RFC or randB
        return UUID(mostSignificant, leastSignificant)
    }

    private companion object {
        const val TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL // 48 bits
        const val RAND_A_MASK = 0x0FFFL // 12 bits
        const val RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL // 62 bits
        const val VERSION_7 = 0x7000L
        const val VARIANT_RFC = Long.MIN_VALUE // bits "10" no topo
    }
}
