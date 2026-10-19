package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RegrasProvaTest {

	private static final Map<String, Integer> OFICIAL = Map.of("legislacao", 12, "direcao-defensiva", 10,
			"primeiros-socorros", 3, "meio-ambiente", 3, "mecanica", 2);

	@Test
	void aceitaAsRegrasOficiais() {
		RegrasProva regras = new RegrasProva(30, 21, Duration.ofMinutes(40), OFICIAL);

		assertThat(regras.questoesPorDisciplina()).containsExactlyInAnyOrderEntriesOf(OFICIAL);
	}

	@Test
	void rejeitaDistribuicaoQueNaoSomaOTotal() {
		assertThatThrownBy(() -> new RegrasProva(31, 21, Duration.ofMinutes(40), OFICIAL))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("soma 30 questões, mas o total da prova é 31");
	}

	@Test
	void rejeitaNotaDeCorteMaiorQueOTotal() {
		assertThatThrownBy(() -> new RegrasProva(30, 31, Duration.ofMinutes(40), OFICIAL))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("nota-corte");
	}

	@Test
	void rejeitaDuracaoZero() {
		assertThatThrownBy(() -> new RegrasProva(30, 21, Duration.ZERO, OFICIAL))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("duracao");
	}

}
