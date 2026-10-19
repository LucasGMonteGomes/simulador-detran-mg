package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;

/**
 * Nota de um simulado encerrado.
 *
 * @param tempoUsado do início até o encerramento; nunca passa da duração da prova
 */
public record ResultadoProva(long simuladoId, int acertos, int emBranco, int totalQuestoes, int notaCorte,
		boolean aprovado, Duration tempoUsado) {

	static ResultadoProva de(Simulado simulado) {
		int emBranco = (int) simulado.getRespostas().stream().filter(Resposta::isEmBranco).count();
		return new ResultadoProva(simulado.getId(), simulado.getAcertos(), emBranco, simulado.getTotalQuestoes(),
				simulado.getNotaCorte(), simulado.getAprovado(),
				Duration.between(simulado.getIniciadoEm(), simulado.getFinalizadoEm()));
	}

	public int erros() {
		return totalQuestoes - acertos - emBranco;
	}

	public double percentualAcertos() {
		return 100.0 * acertos / totalQuestoes;
	}

}
