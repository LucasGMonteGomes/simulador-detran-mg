package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.util.List;

/**
 * Lista todos os problemas de uma vez, para você corrigir os arquivos sem precisar rodar a importação várias vezes.
 */
public class QuestoesInvalidasException extends RuntimeException {

	private final List<String> erros;

	public QuestoesInvalidasException(List<String> erros) {
		super(erros.size() + " problema(s) nos arquivos de questões:\n - " + String.join("\n - ", erros));
		this.erros = List.copyOf(erros);
	}

	public List<String> getErros() {
		return erros;
	}

}
