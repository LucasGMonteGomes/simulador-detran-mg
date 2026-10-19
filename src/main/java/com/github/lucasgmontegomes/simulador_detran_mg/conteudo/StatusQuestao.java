package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

public enum StatusQuestao {

	/** Ainda não revisada. Pode aparecer no Modo Treino, nunca no Modo Prova. */
	RASCUNHO,

	/** Gabarito e explicação conferidos. Pode aparecer em qualquer modo. */
	REVISADA,

	/** Alguém apontou um erro durante um simulado. Fica fora dos sorteios até ser corrigida. */
	REPORTADA

}
