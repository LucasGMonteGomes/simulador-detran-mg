package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

public record ResultadoImportacao(int criadas, int novasVersoes, int inalteradas) {

	public int total() {
		return criadas + novasVersoes + inalteradas;
	}

}
