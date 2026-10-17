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
import java.time.ZoneOffset

@SpringBootTest
class TimeAndIdConfigurationTest(
    @param:Autowired private val context: ApplicationContext,
) {
    @Test
    fun `Should expose exactly one Clock in UTC`() {
        /** @Dado o contexto Spring em execução, @Quando os beans do tipo Clock são consultados */
        val clocks = context.getBeansOfType<Clock>()

        /** @Então existe um único Clock e a zona é UTC */
        clocks.size shouldBe 1
        clocks.values.single().zone shouldBe ZoneOffset.UTC
    }

    @Test
    fun `Should expose an IdGenerator implemented by UuidV7Generator`() {
        /** @Dado o contexto Spring em execução, @Quando os beans do tipo IdGenerator são consultados */
        val generators = context.getBeansOfType<IdGenerator>()

        /** @Então o único IdGenerator é o UuidV7Generator */
        generators.size shouldBe 1
        generators.values.single().shouldBeInstanceOf<UuidV7Generator>()
    }
}
