package com.urbanpay.core.shared;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfig {

    /** Precisão de microssegundos, a mesma do TIMESTAMPTZ do Postgres (resposta == valor gravado). */
    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}