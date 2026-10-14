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
    fun `contexto expoe exatamente um Clock em UTC`() {
        /** @Quando o contexto Spring sobe */
        val clocks = context.getBeansOfType<Clock>()

        /** @Então existe um único Clock e a zona é UTC */
        clocks.size shouldBe 1
        clocks.values.single().zone shouldBe ZoneOffset.UTC
    }

    @Test
    fun `contexto expoe IdGenerator implementado por UuidV7Generator`() {
        /** @Quando o contexto Spring sobe */
        val generators = context.getBeansOfType<IdGenerator>()

        /** @Então o único IdGenerator é o UuidV7Generator */
        generators.size shouldBe 1
        generators.values.single().shouldBeInstanceOf<UuidV7Generator>()
    }
}
