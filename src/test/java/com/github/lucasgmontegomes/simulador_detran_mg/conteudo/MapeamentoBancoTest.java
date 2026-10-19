package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ModoSimulado;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.Simulado;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SimuladoRepository;

/**
 * Confere que migrations e entidades batem e que as constraints do banco funcionam.
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class MapeamentoBancoTest {

	@Autowired
	private TestEntityManager em;

	@Autowired
	private DisciplinaRepository disciplinas;

	@Autowired
	private SubtemaRepository subtemas;

	@Autowired
	private QuestaoRepository questoes;

	@Autowired
	private SimuladoRepository simulados;

	@Test
	void migrationCriaAsCincoDisciplinasComAs45HorasDoCurso() {
		List<Disciplina> todas = disciplinas.findAll();

		assertThat(todas).hasSize(5);
		assertThat(todas.stream().mapToInt(Disciplina::getCargaHoraria).sum()).isEqualTo(45);
	}

	@Test
	void subtemaPertenceADisciplina() {
		Subtema advertencia = subtemas.findByCodigo("sinalizacao-advertencia").orElseThrow();

		assertThat(advertencia.getDisciplina().getCodigo()).isEqualTo("legislacao");
	}

	@Test
	void salvaQuestaoComAlternativasEmOrdem() {
		Questao questao = novaQuestao("LEG-0001");
		questao.adicionarAlternativa(2, "Reduzir a velocidade", true);
		questao.adicionarAlternativa(1, "Manter a velocidade", false);
		questoes.save(questao);
		em.flush();
		em.clear();

		Questao salva = questoes.findByCodigoAndAtivaTrue("LEG-0001").orElseThrow();

		assertThat(salva.getAlternativas()).extracting(Alternativa::getOrdem).containsExactly(1, 2);
		assertThat(salva.alternativaCorreta()).map(Alternativa::getTexto).contains("Reduzir a velocidade");
	}

	@Test
	void bancoRejeitaDuasAlternativasCorretas() {
		Questao questao = novaQuestao("LEG-0002");
		questao.adicionarAlternativa(1, "A", true);
		questao.adicionarAlternativa(2, "B", true);

		assertThatThrownBy(() -> questoes.saveAndFlush(questao))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void salvaSimuladoComRespostasEmAndamento() {
		Questao questao = novaQuestao("LEG-0003");
		Alternativa certa = questao.adicionarAlternativa(1, "Certa", true);
		questao.adicionarAlternativa(2, "Errada", false);
		questoes.save(questao);

		Instant inicio = Instant.parse("2026-10-19T14:00:00Z");
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(questao), 1, Duration.ofMinutes(40), inicio);
		simulado.responder(1, certa, Duration.ofSeconds(25), inicio.plusSeconds(25));
		simulados.save(simulado);
		em.flush();
		em.clear();

		Simulado salvo = simulados.findById(simulado.getId()).orElseThrow();

		assertThat(salvo.isFinalizado()).isFalse();
		assertThat(salvo.getExpiraEm()).isEqualTo(inicio.plus(Duration.ofMinutes(40)));
		assertThat(salvo.getRespostas()).singleElement().satisfies(resposta -> {
			assertThat(resposta.getAlternativa().getId()).isEqualTo(certa.getId());
			assertThat(resposta.getTempoGasto()).isEqualTo(Duration.ofSeconds(25));
			assertThat(resposta.getCorreta()).isNull();
		});
	}

	@Test
	void salvaSimuladoFinalizadoComGabaritoCongelado() {
		Questao questao = novaQuestao("LEG-0006");
		questao.adicionarAlternativa(1, "Certa", true);
		Alternativa errada = questao.adicionarAlternativa(2, "Errada", false);
		questoes.save(questao);

		Instant inicio = Instant.parse("2026-10-19T14:00:00Z");
		Simulado simulado = new Simulado(ModoSimulado.PROVA, List.of(questao), 1, Duration.ofMinutes(40), inicio);
		simulado.responder(1, errada, Duration.ofSeconds(10), inicio.plusSeconds(10));
		simulado.finalizar(inicio.plusSeconds(20));
		simulados.save(simulado);
		em.flush();
		em.clear();

		Simulado salvo = simulados.findById(simulado.getId()).orElseThrow();

		assertThat(salvo.getAcertos()).isZero();
		assertThat(salvo.getAprovado()).isFalse();
		assertThat(salvo.getFinalizadoEm()).isEqualTo(inicio.plusSeconds(20));
		assertThat(salvo.getRespostas()).singleElement().extracting(r -> r.getCorreta()).isEqualTo(false);
	}

	@Test
	void naoAceitaAlternativaDeOutraQuestao() {
		Questao questao = novaQuestao("LEG-0004");
		questao.adicionarAlternativa(1, "A", true);
		Questao outra = novaQuestao("LEG-0005");
		Alternativa deOutra = outra.adicionarAlternativa(1, "X", true);

		Instant agora = Instant.now();
		Simulado simulado = new Simulado(ModoSimulado.TREINO, List.of(questao), 1, null, agora);

		assertThatThrownBy(() -> simulado.responder(1, deOutra, Duration.ZERO, agora))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private Questao novaQuestao(String codigo) {
		Subtema subtema = subtemas.findByCodigo("normas-circulacao").orElseThrow();
		return new Questao(subtema, codigo, 1, "Ao se aproximar de um cruzamento, o condutor deve:", null,
				"Explicação de teste.", "CTB art. 44");
	}

}
