package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;

/**
 * Nota de um simulado encerrado.
 *
 * @param tempoUsado do início até o encerramento; nunca passa da duração da prova
 * @param duracaoProva tempo que a prova tinha; {@code null} em simulados sem cronômetro
 */
public record ResultadoProva(long simuladoId, int acertos, int emBranco, int totalQuestoes, int notaCorte,
		boolean aprovado, Duration tempoUsado, Duration duracaoProva) {

	static ResultadoProva de(Simulado simulado) {
		int emBranco = (int) simulado.getRespostas().stream().filter(Resposta::isEmBranco).count();
		Duration duracao = simulado.getExpiraEm() == null ? null
				: Duration.between(simulado.getIniciadoEm(), simulado.getExpiraEm());
		return new ResultadoProva(simulado.getId(), simulado.getAcertos(), emBranco, simulado.getTotalQuestoes(),
				simulado.getNotaCorte(), simulado.getAprovado(),
				Duration.between(simulado.getIniciadoEm(), simulado.getFinalizadoEm()), duracao);
	}

	public int erros() {
		return totalQuestoes - acertos - emBranco;
	}

	public double percentualAcertos() {
		return 100.0 * acertos / totalQuestoes;
	}

}
