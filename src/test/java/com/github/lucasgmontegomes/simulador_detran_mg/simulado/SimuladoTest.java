package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;

class SimuladoTest {

	private static final Instant INICIO = Instant.parse("2026-10-19T14:00:00Z");

	private static final Duration DURACAO = Duration.ofMinutes(40);

	private final Questao q1 = novaQuestao("LEG-0001");

	private final Questao q2 = novaQuestao("LEG-0002");

	private final Questao q3 = novaQuestao("LEG-0003");

	@Test
	void finalizarCorrigeRespostasEContaEmBrancoComoErro() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1, q2, q3), 1, DURACAO, INICIO);
		simulado.responder(1, correta(q1), Duration.ofSeconds(30), INICIO.plusSeconds(30));
		simulado.responder(2, errada(q2), Duration.ofSeconds(40), INICIO.plusSeconds(70));

		simulado.finalizar(INICIO.plusSeconds(90));

		assertThat(simulado.getAcertos()).isEqualTo(1);
		assertThat(simulado.getAprovado()).isTrue();
		assertThat(simulado.getFinalizadoEm()).isEqualTo(INICIO.plusSeconds(90));
		assertThat(simulado.getRespostas()).extracting(Resposta::getCorreta).containsExactly(true, false, false);
	}

	@Test
	void reprovaAbaixoDaNotaDeCorte() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1, q2, q3), 2, DURACAO, INICIO);
		simulado.responder(1, correta(q1), Duration.ofSeconds(30), INICIO.plusSeconds(30));

		simulado.finalizar(INICIO.plusSeconds(60));

		assertThat(simulado.getAcertos()).isEqualTo(1);
		assertThat(simulado.getAprovado()).isFalse();
	}

	@Test
	void aprovaExatamenteNaNotaDeCorte() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1, q2, q3), 2, DURACAO, INICIO);
		simulado.responder(1, correta(q1), Duration.ofSeconds(30), INICIO.plusSeconds(30));
		simulado.responder(2, correta(q2), Duration.ofSeconds(30), INICIO.plusSeconds(60));

		simulado.finalizar(INICIO.plusSeconds(90));

		assertThat(simulado.getAprovado()).isTrue();
	}

	@Test
	void entregaAtrasadaEncerraNoHorarioDoPrazo() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1), 1, DURACAO, INICIO);

		simulado.finalizar(INICIO.plus(DURACAO).plusSeconds(45));

		assertThat(simulado.getFinalizadoEm()).isEqualTo(INICIO.plus(DURACAO));
	}

	@Test
	void simuladoSemCronometroEncerraNoMomentoDoPedido() {
		Simulado simulado = new Simulado(ModoSimulado.TREINO, List.of(q1), 1, null, INICIO);
		Instant tresHorasDepois = INICIO.plus(Duration.ofHours(3));

		simulado.finalizar(tresHorasDepois);

		assertThat(simulado.getFinalizadoEm()).isEqualTo(tresHorasDepois);
	}

	@Test
	void naoFinalizaDuasVezes() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1), 1, DURACAO, INICIO);
		simulado.finalizar(INICIO.plusSeconds(10));

		assertThatThrownBy(() -> simulado.finalizar(INICIO.plusSeconds(20)))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void naoAceitaRespostaDepoisDeFinalizado() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1), 1, DURACAO, INICIO);
		simulado.finalizar(INICIO.plusSeconds(10));

		assertThatThrownBy(() -> simulado.responder(1, correta(q1), Duration.ZERO, INICIO.plusSeconds(20)))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void naoAceitaRespostaDepoisDoPrazo() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1), 1, DURACAO, INICIO);

		assertThatThrownBy(() -> simulado.responder(1, correta(q1), Duration.ZERO, INICIO.plus(DURACAO)))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void tempoGastoSomaAsVisitasNaMesmaQuestao() {
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(q1), 1, DURACAO, INICIO);
		simulado.responder(1, errada(q1), Duration.ofSeconds(20), INICIO.plusSeconds(20));
		simulado.responder(1, correta(q1), Duration.ofSeconds(15), INICIO.plusSeconds(120));

		Resposta resposta = simulado.getRespostas().getFirst();
		assertThat(resposta.getTempoGasto()).isEqualTo(Duration.ofSeconds(35));
		assertThat(resposta.getAlternativa().isCorreta()).isTrue();
	}

	private static Questao novaQuestao(String codigo) {
		// Subtema não participa das regras do simulado, então fica nulo neste teste sem banco.
		Questao questao = new Questao(null, codigo, 1, "Enunciado " + codigo, null, "Explicação", null);
		questao.adicionarAlternativa(1, "Certa", true);
		questao.adicionarAlternativa(2, "Errada", false);
		return questao;
	}

	private static Alternativa correta(Questao questao) {
		return questao.alternativaCorreta().orElseThrow();
	}

	private static Alternativa errada(Questao questao) {
		return questao.getAlternativas().stream().filter(a -> !a.isCorreta()).findFirst().orElseThrow();
	}

}
