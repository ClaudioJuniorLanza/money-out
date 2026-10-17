package com.moneyout.infrastructure.config

import com.moneyout.infrastructure.id.UuidV7Generator
import com.moneyout.sharedkernel.IdGenerator
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@SpringBootTest
class TimeAndIdConfigurationTest(
    @param:Autowired private val context: ApplicationContext,
) {
    @Test
    fun `Should expose exactly one Clock in UTC`() {
        /** @Quando os beans do tipo Clock são consultados */
        val clocks = context.getBeansOfType<Clock>()

        /** @Então existe um único Clock e a zona é UTC */
        clocks.size shouldBe 1
        clocks.values.single().zone shouldBe ZoneOffset.UTC
    }

    @Test
    fun `Should expose an IdGenerator implemented by UuidV7Generator`() {
        /** @Quando os beans do tipo IdGenerator são consultados */
        val generators = context.getBeansOfType<IdGenerator>()

        /** @Então o único IdGenerator é o UuidV7Generator */
        generators.size shouldBe 1
        generators.values.single().shouldBeInstanceOf<UuidV7Generator>()
    }

    @Test
    fun `Should build the IdGenerator from the injected Clock`() {
        /** @Dado um Clock fixo entregue à configuração */
        val fixedInstant = Instant.parse("2026-10-10T12:00:00.123Z")
        val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

        /** @Quando o IdGenerator é criado pela configuração e gera um id */
        val uuid = TimeAndIdConfiguration().idGenerator(clock).next()

        /** @Então o timestamp embutido no id é o instante do Clock injetado */
        (uuid.mostSignificantBits ushr TIMESTAMP_SHIFT) shouldBe fixedInstant.toEpochMilli()
    }

    private companion object {
        const val TIMESTAMP_SHIFT = 16 // UUIDv7: 48 bits de timestamp acima de versão + rand_a
    }
}
