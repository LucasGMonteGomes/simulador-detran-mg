package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RegrasProva.class)
class ConfiguracaoSimulado {

	/** Fonte única do horário: nos testes, é trocada por um relógio controlado para simular o tempo passando. */
	@Bean
	Clock relogio() {
		return Clock.systemUTC();
	}

}
