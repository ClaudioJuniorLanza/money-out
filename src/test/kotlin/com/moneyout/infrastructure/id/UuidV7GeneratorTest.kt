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
    fun `tem versao 7 e variante RFC`() {
        val uuid = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC)).next()

        uuid.version() shouldBe 7
        uuid.variant() shouldBe 2
    }

    @Test
    fun `embute o timestamp do clock nos 48 bits mais significativos`() {
        val uuid = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC)).next()

        (uuid.mostSignificantBits ushr 16) shouldBe fixedInstant.toEpochMilli()
    }

    @Test
    fun `ids de instantes diferentes ordenam pelo tempo`() {
        val earlier = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC)).next()
        val later = UuidV7Generator(Clock.fixed(fixedInstant.plusMillis(1), ZoneOffset.UTC)).next()

        // UUID.compareTo é assinado; comparamos como texto (hex fixo), que é como o Postgres ordena uuid.
        earlier.toString() shouldBeLessThan later.toString()
    }

    @Test
    fun `nao repete ids no mesmo milissegundo`() {
        val generator = UuidV7Generator(Clock.fixed(fixedInstant, ZoneOffset.UTC))

        val ids = List(10_000) { generator.next() }

        ids.toSet().size shouldBe ids.size
    }
}
