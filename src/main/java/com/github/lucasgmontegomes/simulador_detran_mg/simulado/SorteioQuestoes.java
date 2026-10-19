package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;

/**
 * Escolhe as questões de uma prova. Dentro de cada disciplina, a prioridade é:
 * <ol>
 * <li>questões que apareceram menos vezes em simulados anteriores (repetir questão infla a nota);</li>
 * <li>entre as igualmente vistas, o subtema menos usado até ali nesta prova (cobre mais conteúdo);</li>
 * <li>nos empates restantes, sorteio.</li>
 * </ol>
 */
class SorteioQuestoes {

	/**
	 * @param aparicoes em quantos simulados a questão já apareceu
	 */
	record Candidata(Questao questao, String disciplina, Long subtemaId, long aparicoes) {
	}

	private final RandomGenerator aleatorio;

	SorteioQuestoes(RandomGenerator aleatorio) {
		this.aleatorio = aleatorio;
	}

	/**
	 * @return as questões já embaralhadas, na ordem em que aparecem na prova
	 * @throws QuestoesInsuficientesException se alguma disciplina não tiver candidatas suficientes
	 */
	List<Questao> sortear(Map<String, Integer> quantidadePorDisciplina, Collection<Candidata> candidatas) {
		Map<String, List<Candidata>> porDisciplina = candidatas.stream()
			.collect(Collectors.groupingBy(Candidata::disciplina));

		List<String> faltas = new ArrayList<>();
		List<Questao> sorteadas = new ArrayList<>();
		quantidadePorDisciplina.forEach((disciplina, quantidade) -> {
			List<Candidata> disponiveis = porDisciplina.getOrDefault(disciplina, List.of());
			if (disponiveis.size() < quantidade) {
				faltas.add(disciplina + ": precisa de " + quantidade + ", há " + disponiveis.size());
			}
			else {
				sorteadas.addAll(escolher(disponiveis, quantidade));
			}
		});
		if (!faltas.isEmpty()) {
			throw new QuestoesInsuficientesException(faltas);
		}
		Collections.shuffle(sorteadas, aleatorio);
		return sorteadas;
	}

	private List<Questao> escolher(List<Candidata> disponiveis, int quantidade) {
		Map<Long, Integer> usosPorSubtema = new HashMap<>();
		Comparator<Candidata> subtemaMenosUsado = Comparator
			.comparingInt(candidata -> usosPorSubtema.getOrDefault(candidata.subtemaId(), 0));
		List<Questao> escolhidas = new ArrayList<>(quantidade);

		for (List<Candidata> faixa : agruparPorAparicoes(disponiveis)) {
			List<Candidata> fila = new ArrayList<>(faixa);
			Collections.shuffle(fila, aleatorio);
			while (!fila.isEmpty() && escolhidas.size() < quantidade) {
				// min() devolve o primeiro empatado; como a fila foi embaralhada, o desempate é aleatório.
				Candidata proxima = fila.stream().min(subtemaMenosUsado).orElseThrow();
				fila.remove(proxima);
				escolhidas.add(proxima.questao());
				usosPorSubtema.merge(proxima.subtemaId(), 1, Integer::sum);
			}
			if (escolhidas.size() == quantidade) {
				break;
			}
		}
		return escolhidas;
	}

	/** Faixas de candidatas com o mesmo número de aparições, da menos vista para a mais vista. */
	private static Collection<List<Candidata>> agruparPorAparicoes(List<Candidata> candidatas) {
		return candidatas.stream()
			.collect(Collectors.groupingBy(Candidata::aparicoes, TreeMap::new, Collectors.toList()))
			.values();
	}

}
