package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

/**
 * @param nome caminho relativo à pasta {@code questoes/}, usado nas mensagens de erro
 */
public record ArquivoLido(String nome, ArquivoQuestoes arquivo) {
}
