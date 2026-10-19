package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

public enum ModoSimulado {

	/** Distribuição oficial, cronômetro e sem feedback até o fim. Usado para medir a evolução. */
	PROVA,

	/** Focado nos subtemas mais fracos, com feedback imediato. Fica fora da métrica de evolução. */
	TREINO

}
