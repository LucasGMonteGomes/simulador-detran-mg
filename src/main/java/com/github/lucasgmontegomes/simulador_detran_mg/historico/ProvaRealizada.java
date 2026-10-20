package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import java.time.Duration;
import java.time.Instant;

/**
 * Uma prova encerrada, como aparece no histórico.
 *
 * @param numero posição da prova no histórico, a partir de 1 (a mais antiga)
 */
public record ProvaRealizada(int numero, long simuladoId, Instant iniciadoEm, Instant finalizadoEm, int acertos,
		int emBranco, int totalQuestoes, int notaCorte, boolean aprovado) {

	public Duration tempoUsado() {
		return Duration.between(iniciadoEm, finalizadoEm);
	}

	public int percentual() {
		return Math.round(100f * acertos / totalQuestoes);
	}

}
