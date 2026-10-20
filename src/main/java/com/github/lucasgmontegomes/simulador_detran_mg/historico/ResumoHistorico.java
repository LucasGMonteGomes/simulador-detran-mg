package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import java.util.List;
import java.util.Optional;

/**
 * Números que resumem a evolução. Uma nota isolada oscila muito; por isso o resumo olha as últimas provas juntas.
 *
 * @param provas da mais antiga para a mais recente
 */
public record ResumoHistorico(List<ProvaRealizada> provas) {

	/** Quantas provas recentes entram na média e na taxa de aprovação. */
	public static final int JANELA = 5;

	public ResumoHistorico {
		provas = List.copyOf(provas);
	}

	public int quantidade() {
		return provas.size();
	}

	public Optional<ProvaRealizada> ultima() {
		return provas.isEmpty() ? Optional.empty() : Optional.of(provas.getLast());
	}

	/** Acertos da última prova menos os da penúltima; vazio se ainda não houver duas provas. */
	public Optional<Integer> variacaoDaUltima() {
		if (provas.size() < 2) {
			return Optional.empty();
		}
		return Optional.of(provas.getLast().acertos() - provas.get(provas.size() - 2).acertos());
	}

	public List<ProvaRealizada> recentes() {
		return provas.subList(Math.max(0, provas.size() - JANELA), provas.size());
	}

	public double mediaRecente() {
		return recentes().stream().mapToInt(ProvaRealizada::acertos).average().orElse(0);
	}

	public long aprovadasRecentes() {
		return recentes().stream().filter(ProvaRealizada::aprovado).count();
	}

	public int melhorNota() {
		return provas.stream().mapToInt(ProvaRealizada::acertos).max().orElse(0);
	}

}
