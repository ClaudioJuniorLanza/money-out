package com.moneyout.infrastructure.config

import com.moneyout.infrastructure.id.UuidV7Generator
import com.moneyout.sharedkernel.IdGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Tempo e ids vêm do container, nunca de chamadas globais no código puro.
 * UTC porque eventos e `TIMESTAMPTZ` são gravados em UTC; fuso só na apresentação.
 */
@Configuration
class TimeAndIdConfiguration {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun idGenerator(clock: Clock): IdGenerator = UuidV7Generator(clock)
}
