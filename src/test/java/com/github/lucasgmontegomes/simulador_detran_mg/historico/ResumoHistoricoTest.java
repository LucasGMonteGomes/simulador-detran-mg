package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ResumoHistoricoTest {

	private static final Instant INICIO = Instant.parse("2026-10-20T13:00:00Z");

	@Test
	void historicoVazio() {
		ResumoHistorico resumo = new ResumoHistorico(List.of());

		assertThat(resumo.quantidade()).isZero();
		assertThat(resumo.ultima()).isEmpty();
		assertThat(resumo.variacaoDaUltima()).isEmpty();
		assertThat(resumo.mediaRecente()).isZero();
		assertThat(resumo.aprovadasRecentes()).isZero();
	}

	@Test
	void mediaEAprovacaoOlhamSoAsUltimasCincoProvas() {
		ResumoHistorico resumo = new ResumoHistorico(provas(10, 12, 14, 16, 18, 20, 22));

		assertThat(resumo.recentes()).extracting(ProvaRealizada::acertos).containsExactly(14, 16, 18, 20, 22);
		assertThat(resumo.mediaRecente()).isEqualTo(18.0);
		assertThat(resumo.aprovadasRecentes()).isEqualTo(1);
		assertThat(resumo.melhorNota()).isEqualTo(22);
		assertThat(resumo.variacaoDaUltima()).contains(2);
	}

	@Test
	void comMenosDeCincoProvasUsaTodas() {
		ResumoHistorico resumo = new ResumoHistorico(provas(21, 15));

		assertThat(resumo.recentes()).hasSize(2);
		assertThat(resumo.mediaRecente()).isEqualTo(18.0);
		assertThat(resumo.variacaoDaUltima()).contains(-6);
	}

	private static List<ProvaRealizada> provas(int... acertos) {
		return IntStream.range(0, acertos.length).mapToObj(i -> {
			Instant inicio = INICIO.plus(Duration.ofDays(i));
			return new ProvaRealizada(i + 1, i + 1L, inicio, inicio.plus(Duration.ofMinutes(30)), acertos[i],
					30 - acertos[i], 30, 21, acertos[i] >= 21);
		}).toList();
	}

}
