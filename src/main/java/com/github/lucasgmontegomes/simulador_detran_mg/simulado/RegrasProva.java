package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Formato do Modo Prova, lido de {@code simulador.prova} no {@code application.yaml}. Regras inconsistentes
 * impedem a aplicação de subir.
 *
 * @param questoesPorDisciplina código da disciplina para a quantidade de questões; a soma deve dar o total
 */
@ConfigurationProperties("simulador.prova")
public record RegrasProva(int totalQuestoes, int notaCorte, Duration duracao,
		Map<String, Integer> questoesPorDisciplina) {

	public RegrasProva {
		if (totalQuestoes <= 0) {
			throw new IllegalArgumentException("simulador.prova.total-questoes deve ser positivo");
		}
		if (notaCorte < 0 || notaCorte > totalQuestoes) {
			throw new IllegalArgumentException(
					"simulador.prova.nota-corte deve estar entre 0 e " + totalQuestoes + " (é " + notaCorte + ")");
		}
		if (duracao == null || duracao.isNegative() || duracao.isZero()) {
			throw new IllegalArgumentException("simulador.prova.duracao deve ser positiva");
		}
		questoesPorDisciplina = Collections.unmodifiableMap(new LinkedHashMap<>(questoesPorDisciplina));
		int soma = questoesPorDisciplina.values().stream().mapToInt(Integer::intValue).sum();
		if (soma != totalQuestoes) {
			throw new IllegalArgumentException("simulador.prova.questoes-por-disciplina soma " + soma
					+ " questões, mas o total da prova é " + totalQuestoes);
		}
	}

}
