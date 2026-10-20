package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Relógio que só anda quando o teste manda. Permite testar o fim do prazo da prova sem esperar o tempo de verdade.
 */
public class RelogioDeTeste extends Clock {

	public static final Instant INICIO = Instant.parse("2026-10-20T13:00:00Z");

	private volatile Instant agora;

	public RelogioDeTeste(Instant inicio) {
		this.agora = inicio;
	}

	public void definir(Instant instante) {
		this.agora = instante;
	}

	public void avancar(Duration duracao) {
		this.agora = agora.plus(duracao);
	}

	@Override
	public Instant instant() {
		return agora;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return Clock.fixed(agora, zona);
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Configuracao {

		@Bean
		@Primary
		RelogioDeTeste relogioDeTeste() {
			return new RelogioDeTeste(INICIO);
		}

	}

}
