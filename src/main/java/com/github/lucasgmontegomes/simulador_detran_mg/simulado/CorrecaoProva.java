package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;
import java.util.List;

/**
 * Prova encerrada: a nota, o desempenho por disciplina e o gabarito comentado de cada questão.
 */
public record CorrecaoProva(ResultadoProva resultado, List<DesempenhoDisciplina> porDisciplina,
		List<ItemCorrigido> itens) {

	public CorrecaoProva {
		porDisciplina = List.copyOf(porDisciplina);
		itens = List.copyOf(itens);
	}

	public record DesempenhoDisciplina(String nome, int acertos, int total) {

		public int percentual() {
			return total == 0 ? 0 : Math.round(100f * acertos / total);
		}

	}

	public enum Situacao {

		ACERTOU, ERROU, EM_BRANCO

	}

	public record ItemCorrigido(int ordem, String disciplina, String subtema, String enunciado, String imagem,
			List<OpcaoCorrigida> alternativas, Situacao situacao, String explicacao, String referencia,
			Duration tempoGasto) {

		public ItemCorrigido {
			alternativas = List.copyOf(alternativas);
		}

		public boolean acertou() {
			return situacao == Situacao.ACERTOU;
		}

		public boolean emBranco() {
			return situacao == Situacao.EM_BRANCO;
		}

	}

	public record OpcaoCorrigida(String letra, String texto, boolean correta, boolean marcada) {
	}

}
