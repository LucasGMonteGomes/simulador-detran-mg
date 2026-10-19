package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

/**
 * Em quantos simulados uma questão já apareceu (respondida ou não).
 */
public record AparicoesQuestao(Long questaoId, Long vezes) {
}
