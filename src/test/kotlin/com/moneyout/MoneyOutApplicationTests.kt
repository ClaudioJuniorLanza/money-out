package com.moneyout

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext

@SpringBootTest
class MoneyOutApplicationTests(
    @param:Autowired private val context: ConfigurableApplicationContext,
) {
    @Test
    fun `Should start the application context`() {
        /** @Quando o estado do contexto é consultado */
        val active = context.isActive

        /** @Então o contexto está ativo */
        active shouldBe true
    }
}
