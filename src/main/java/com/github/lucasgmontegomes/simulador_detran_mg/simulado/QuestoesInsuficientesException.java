package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.util.List;

/**
 * Não há questões revisadas suficientes para montar a prova. A mensagem diz quantas faltam em cada disciplina.
 */
public class QuestoesInsuficientesException extends RuntimeException {

	private final List<String> faltas;

	public QuestoesInsuficientesException(List<String> faltas) {
		super("Questões revisadas insuficientes para montar a prova:\n - " + String.join("\n - ", faltas));
		this.faltas = List.copyOf(faltas);
	}

	public List<String> getFaltas() {
		return faltas;
	}

}
