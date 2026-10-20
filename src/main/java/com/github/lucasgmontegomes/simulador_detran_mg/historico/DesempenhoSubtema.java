package com.github.lucasgmontegomes.simulador_detran_mg.historico;

/**
 * Acertos em um subtema somando todas as provas, e separados entre as provas recentes e as anteriores.
 */
public record DesempenhoSubtema(String subtema, String disciplina, Periodo geral, Periodo recente, Periodo anterior) {

	/** Abaixo disso, a taxa de um período oscila demais para comparar (cada questão vale muitos pontos). */
	static final int MINIMO_PARA_COMPARAR = 3;

	/** Diferença, em pontos percentuais, a partir da qual a mudança conta como melhora ou piora. */
	static final int VARIACAO_RELEVANTE = 20;

	/** Com menos questões que isso no total, a taxa é mostrada com um aviso de "poucos dados". */
	static final int MINIMO_CONFIAVEL = 5;

	public enum Tendencia {

		MELHORANDO, PIORANDO, ESTAVEL, SEM_DADOS

	}

	public record Periodo(int acertos, int total) {

		public int percentual() {
			return total == 0 ? 0 : Math.round(100f * acertos / total);
		}

	}

	/**
	 * Taxa de acerto com suavização de Laplace: soma um acerto e um erro fictícios. Assim, errar 1 de 1 (0%) não
	 * parece pior que errar 6 de 8 (25%) - com tão pouca amostra, 1 de 1 diz muito menos.
	 */
	public double taxaSuavizada() {
		return (geral.acertos() + 1.0) / (geral.total() + 2.0);
	}

	public boolean poucosDados() {
		return geral.total() < MINIMO_CONFIAVEL;
	}

	public Tendencia tendencia() {
		if (recente.total() < MINIMO_PARA_COMPARAR || anterior.total() < MINIMO_PARA_COMPARAR) {
			return Tendencia.SEM_DADOS;
		}
		int variacao = variacao();
		if (variacao >= VARIACAO_RELEVANTE) {
			return Tendencia.MELHORANDO;
		}
		if (variacao <= -VARIACAO_RELEVANTE) {
			return Tendencia.PIORANDO;
		}
		return Tendencia.ESTAVEL;
	}

	/** Pontos percentuais das provas recentes em relação às anteriores. */
	public int variacao() {
		return recente.percentual() - anterior.percentual();
	}

}
