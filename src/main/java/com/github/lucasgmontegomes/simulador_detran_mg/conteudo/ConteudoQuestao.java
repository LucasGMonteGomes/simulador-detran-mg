package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.util.List;

/**
 * Tudo o que define uma versão de questão. Se qualquer campo mudar, é outra versão: assim simulados antigos
 * continuam apontando para o texto que você realmente viu.
 */
public record ConteudoQuestao(String enunciado, String imagem, List<Opcao> alternativas, String explicacao,
		String referenciaLegal, String trechoFonte) {

	public ConteudoQuestao {
		alternativas = List.copyOf(alternativas);
	}

	public record Opcao(String texto, boolean correta) {
	}

}
