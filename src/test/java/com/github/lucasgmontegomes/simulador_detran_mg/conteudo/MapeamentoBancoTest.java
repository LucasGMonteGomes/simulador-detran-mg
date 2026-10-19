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
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.ConteudoQuestao.Opcao;
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
	void salvaQuestaoComConteudoOrigemEStatus() {
		ConteudoQuestao conteudo = conteudo(new Opcao("Manter a velocidade", false),
				new Opcao("Reduzir a velocidade", true));
		Questao questao = new Questao(subtema(), "LEG-0001", 1, OrigemQuestao.IA, conteudo);
		questao.atualizarRevisao(OrigemQuestao.IA, true);
		questoes.save(questao);
		em.flush();
		em.clear();

		Questao salva = questoes.findByCodigoAndAtivaTrue("LEG-0001").orElseThrow();

		assertThat(salva.conteudo()).isEqualTo(conteudo);
		assertThat(salva.getAlternativas()).extracting(Alternativa::getOrdem).containsExactly(1, 2);
		assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.IA);
		assertThat(salva.getStatus()).isEqualTo(StatusQuestao.REVISADA);
	}

	@Test
	void bancoRejeitaDuasAlternativasCorretas() {
		Questao questao = new Questao(subtema(), "LEG-0002", 1, OrigemQuestao.MANUAL,
				conteudo(new Opcao("A", true), new Opcao("B", true)));

		assertThatThrownBy(() -> questoes.saveAndFlush(questao))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void bancoRejeitaDuasVersoesAtivasDoMesmoCodigo() {
		questoes.saveAndFlush(novaQuestao("LEG-0003", 1));

		assertThatThrownBy(() -> questoes.saveAndFlush(novaQuestao("LEG-0003", 2)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void maiorVersaoConsideraVersoesInativas() {
		Questao v1 = novaQuestao("LEG-0004", 1);
		v1.desativar();
		questoes.saveAndFlush(v1);
		questoes.saveAndFlush(novaQuestao("LEG-0004", 2));

		assertThat(questoes.findMaiorVersao("LEG-0004")).isEqualTo(2);
		assertThat(questoes.findMaiorVersao("NAO-EXISTE")).isZero();
	}

	@Test
	void salvaSimuladoComRespostasEmAndamento() {
		Questao questao = questoes.save(novaQuestao("LEG-0005", 1));
		Alternativa certa = questao.alternativaCorreta().orElseThrow();

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
		Questao questao = questoes.save(novaQuestao("LEG-0006", 1));
		Alternativa errada = questao.getAlternativas().stream().filter(a -> !a.isCorreta()).findFirst().orElseThrow();

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
		Questao questao = novaQuestao("LEG-0007", 1);
		Alternativa deOutra = novaQuestao("LEG-0008", 1).alternativaCorreta().orElseThrow();

		Instant agora = Instant.now();
		Simulado simulado = new Simulado(ModoSimulado.TREINO, List.of(questao), 1, null, agora);

		assertThatThrownBy(() -> simulado.responder(1, deOutra, Duration.ZERO, agora))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private Questao novaQuestao(String codigo, int versao) {
		return new Questao(subtema(), codigo, versao, OrigemQuestao.MANUAL,
				conteudo(new Opcao("Certa", true), new Opcao("Errada", false)));
	}

	private Subtema subtema() {
		return subtemas.findByCodigo("normas-circulacao").orElseThrow();
	}

	private static ConteudoQuestao conteudo(Opcao... opcoes) {
		return new ConteudoQuestao("Ao se aproximar de um cruzamento, o condutor deve:", null, List.of(opcoes),
				"Explicação de teste.", "CTB art. 44", null);
	}

}
