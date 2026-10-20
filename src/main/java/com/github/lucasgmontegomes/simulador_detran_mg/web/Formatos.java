package com.github.lucasgmontegomes.simulador_detran_mg.web;

import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * Formatação usada nos templates, como {@code ${@formatos.relogio(duracao)}}.
 */
@Component("formatos")
public class Formatos {

	/** {@code 1:05} para um minuto e cinco segundos; {@code 40:00} para a prova inteira. */
	public String relogio(Duration duracao) {
		if (duracao == null) {
			return "—";
		}
		long segundos = Math.max(0, duracao.toSeconds());
		return "%d:%02d".formatted(segundos / 60, segundos % 60);
	}

	public String minutos(Duration duracao) {
		long minutos = duracao.toMinutes();
		return minutos == 1 ? "1 minuto" : minutos + " minutos";
	}

}
