package com.github.lucasgmontegomes.simulador_detran_mg.web;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Formatação usada nos templates, como {@code ${@formatos.relogio(duracao)}}. Datas no fuso do computador onde a
 * aplicação roda.
 */
@Component("formatos")
public class Formatos {

	private static final Locale PT_BR = Locale.of("pt", "BR");

	private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR)
		.withZone(ZoneId.systemDefault());

	/** {@code 1:05} para um minuto e cinco segundos; {@code 60:00} para a prova inteira. */
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

	public String dataHora(Instant instante) {
		return DATA_HORA.format(instante);
	}

	/** Uma casa decimal, com vírgula: {@code 19,4}. */
	public String decimal(double valor) {
		NumberFormat formato = NumberFormat.getNumberInstance(PT_BR);
		formato.setMinimumFractionDigits(1);
		formato.setMaximumFractionDigits(1);
		return formato.format(valor);
	}

	/** Com sinal, para variações: {@code +3}, {@code -2}, {@code 0}. */
	public String comSinal(int valor) {
		return valor > 0 ? "+" + valor : String.valueOf(valor);
	}

}
