package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * O que a tela de uma questão precisa mostrar durante a prova. De propósito, não diz qual alternativa é a correta.
 *
 * @param respondidas ordens das questões que já têm alternativa marcada
 * @param tempoRestante calculado no servidor; o navegador só faz a contagem regressiva a partir dele
 */
public record QuestaoEmProva(long simuladoId, int ordem, int totalQuestoes, String enunciado, String imagem,
		List<Opcao> alternativas, Long alternativaMarcadaId, Set<Integer> respondidas, Duration tempoRestante) {

	public QuestaoEmProva {
		alternativas = List.copyOf(alternativas);
		respondidas = Set.copyOf(respondidas);
	}

	public record Opcao(long id, String letra, String texto) {
	}

	public boolean temAnterior() {
		return ordem > 1;
	}

	public boolean temProxima() {
		return ordem < totalQuestoes;
	}

	public boolean respondida(int numero) {
		return respondidas.contains(numero);
	}

	public int emBranco() {
		return totalQuestoes - respondidas.size();
	}

}
