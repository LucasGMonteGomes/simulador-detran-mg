package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.lucasgmontegomes.simulador_detran_mg.historico.DesempenhoSubtema.Periodo;
import com.github.lucasgmontegomes.simulador_detran_mg.historico.DesempenhoSubtema.Tendencia;

class DesempenhoSubtemaTest {

	@Test
	void melhoraQuandoAsRecentesSobemVinteOuMaisPontos() {
		DesempenhoSubtema desempenho = desempenho(new Periodo(3, 4), new Periodo(1, 4));

		assertThat(desempenho.tendencia()).isEqualTo(Tendencia.MELHORANDO);
		assertThat(desempenho.variacao()).isEqualTo(50);
	}

	@Test
	void pioraQuandoAsRecentesCaemVinteOuMaisPontos() {
		assertThat(desempenho(new Periodo(1, 4), new Periodo(4, 4)).tendencia()).isEqualTo(Tendencia.PIORANDO);
	}

	@Test
	void pequenaVariacaoEhEstavel() {
		DesempenhoSubtema desempenho = desempenho(new Periodo(2, 3), new Periodo(3, 5));

		assertThat(desempenho.variacao()).isEqualTo(7);
		assertThat(desempenho.tendencia()).isEqualTo(Tendencia.ESTAVEL);
	}

	@Test
	void naoComparaPeriodoComPoucasQuestoes() {
		assertThat(desempenho(new Periodo(2, 2), new Periodo(0, 5)).tendencia()).isEqualTo(Tendencia.SEM_DADOS);
		assertThat(desempenho(new Periodo(0, 5), new Periodo(0, 0)).tendencia()).isEqualTo(Tendencia.SEM_DADOS);
	}

	@Test
	void umaUnicaQuestaoErradaNaoPareceOPiorSubtema() {
		DesempenhoSubtema umaErrada = geral(0, 1);
		DesempenhoSubtema seisErradasDeOito = geral(2, 8);

		// Sem suavização seria 0% contra 25%; com ela, oito questões pesam mais que uma.
		assertThat(seisErradasDeOito.taxaSuavizada()).isLessThan(umaErrada.taxaSuavizada());
	}

	@Test
	void avisaQuandoHaPoucasQuestoesNoTotal() {
		assertThat(geral(2, 4).poucosDados()).isTrue();
		assertThat(geral(2, 5).poucosDados()).isFalse();
	}

	private static DesempenhoSubtema desempenho(Periodo recente, Periodo anterior) {
		Periodo geral = new Periodo(recente.acertos() + anterior.acertos(), recente.total() + anterior.total());
		return new DesempenhoSubtema("Placas de advertência", "Legislação de trânsito", geral, recente, anterior);
	}

	private static DesempenhoSubtema geral(int acertos, int total) {
		return desempenho(new Periodo(acertos, total), new Periodo(0, 0));
	}

}
