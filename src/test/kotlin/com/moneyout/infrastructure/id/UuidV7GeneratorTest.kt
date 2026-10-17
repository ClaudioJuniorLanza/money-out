package com.moneyout.infrastructure.id

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class UuidV7GeneratorTest {
    private val fixedInstant = Instant.parse("2026-10-10T12:00:00.123Z")

    @Test
    fun `Should generate a version 7 UUID with the RFC variant`() {
        /** @Dado um gerador com relógio fixo */
        val generator = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

        /** @Quando um id é gerado */
        val uuid = generator.next()

        /** @Então a versão é 7 */
        uuid.version() shouldBe 7

        /** @E a variante é a da RFC */
        uuid.variant() shouldBe 2
    }

    @Test
    fun `Should embed the clock timestamp in the 48 most significant bits`() {
        /** @Dado um gerador com relógio fixo */
        val generator = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

        /** @Quando um id é gerado */
        val uuid = generator.next()

        /** @Então os 48 bits mais significativos são o epoch em milissegundos do relógio */
        (uuid.mostSignificantBits ushr 16) shouldBe fixedInstant.toEpochMilli()
    }

    @Test
    fun `Should order ids by time when generated at different instants`() {
        /** @Dado dois geradores cujos relógios diferem em 1 ms */
        val earlierGenerator = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC))
        val laterGenerator = UuidV7Generator(Clock.fixed(fixedInstant.plusMillis(1), ZoneOffset.UTC))

        /** @Quando cada um gera um id */
        val earlier = earlierGenerator.next()
        val later = laterGenerator.next()

        /** @Então o id mais antigo ordena antes do mais recente, comparando como texto (hex fixo), que é como o Postgres ordena uuid, porque UUID.compareTo é assinado */
        earlier.toString() shouldBeLessThan later.toString()
    }

    @Test
    fun `Should not repeat ids within the same millisecond`() {
        /** @Dado um gerador com relógio fixo */
        val generator = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

        /** @Quando 10.000 ids são gerados no mesmo milissegundo */
        val ids = List(10_000) { generator.next() }

        /** @Então todos são distintos */
        ids.toSet().size shouldBe ids.size
    }
}
