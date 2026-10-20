package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ConfiguracaoImportacao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ImportadorQuestoes;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.LeitorArquivosQuestoes;

/**
 * Usa o banco de questões real do projeto, marcado como revisado, para montar provas como as de verdade.
 */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, ImportadorQuestoes.class, ConfiguracaoImportacao.class,
		LeitorArquivosQuestoes.class, ModoProvaService.class, ConfiguracaoSimulado.class,
		RelogioDeTeste.Configuracao.class })
class ModoProvaServiceTest {

	@Autowired
	private ModoProvaService service;

	@Autowired
	private RegrasProva regras;

	@Autowired
	private SimuladoRepository simulados;

	@Autowired
	private QuestaoRepository questoes;

	@Autowired
	private ImportadorQuestoes importador;

	@Autowired
	private LeitorArquivosQuestoes leitor;

	@Autowired
	private RelogioDeTeste relogio;

	@Autowired
	private TestEntityManager em;

	@BeforeEach
	void importarQuestoesRevisadas() {
		relogio.definir(RelogioDeTeste.INICIO);
		importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));
		marcarTodasComoRevisadas(true);
	}

	@Test
	void sorteiaTrintaQuestoesRevisadasNaDistribuicaoOficial() {
		Simulado prova = service.iniciar();

		assertThat(prova.getModo()).isEqualTo(ModoSimulado.PROVA);
		assertThat(prova.getTotalQuestoes()).isEqualTo(30);
		assertThat(prova.getNotaCorte()).isEqualTo(21);
		assertThat(prova.getExpiraEm()).isEqualTo(RelogioDeTeste.INICIO.plus(regras.duracao()));
		assertThat(questoesPorDisciplina(prova)).containsExactlyInAnyOrderEntriesOf(Map.of("legislacao", 12L,
				"direcao-defensiva", 10L, "primeiros-socorros", 3L, "meio-ambiente", 3L, "mecanica", 2L));
		assertThat(idsDasQuestoes(prova)).doesNotHaveDuplicates();
	}

	@Test
	void segundaProvaUsaSoQuestoesIneditas() {
		Simulado primeira = service.iniciar();
		service.finalizar(primeira.getId());

		Simulado segunda = service.iniciar();

		assertThat(idsDasQuestoes(segunda)).doesNotContainAnyElementsOf(idsDasQuestoes(primeira));
	}

	@Test
	void naoIniciaOutraProvaEnquantoHouverUmaDentroDoPrazo() {
		Simulado aberta = service.iniciar();
		relogio.avancar(regras.duracao().minusMinutes(1));

		assertThatThrownBy(() -> service.iniciar())
			.isInstanceOfSatisfying(ProvaEmAndamentoException.class,
					e -> assertThat(e.getSimuladoId()).isEqualTo(aberta.getId()));
	}

	@Test
	void provaEsquecidaAbertaEhEncerradaNoPrazoAoIniciarOutra() {
		Simulado esquecida = service.iniciar();
		relogio.avancar(Duration.ofHours(3));

		Simulado nova = service.iniciar();
		em.flush();
		em.clear();

		Simulado encerrada = simulados.findById(esquecida.getId()).orElseThrow();
		assertThat(encerrada.getFinalizadoEm()).isEqualTo(RelogioDeTeste.INICIO.plus(regras.duracao()));
		assertThat(encerrada.getAcertos()).isZero();
		assertThat(nova.isFinalizado()).isFalse();
	}

	@Test
	void finalizarCalculaANota() {
		Simulado prova = service.iniciar();
		for (int ordem = 1; ordem <= 21; ordem++) {
			service.responder(prova.getId(), ordem, correta(prova, ordem).getId(), Duration.ofSeconds(40));
		}
		for (int ordem = 22; ordem <= 25; ordem++) {
			service.responder(prova.getId(), ordem, errada(prova, ordem).getId(), Duration.ofSeconds(40));
		}
		relogio.avancar(Duration.ofMinutes(25));

		ResultadoProva resultado = service.finalizar(prova.getId());

		assertThat(resultado.acertos()).isEqualTo(21);
		assertThat(resultado.erros()).isEqualTo(4);
		assertThat(resultado.emBranco()).isEqualTo(5);
		assertThat(resultado.aprovado()).isTrue();
		assertThat(resultado.percentualAcertos()).isEqualTo(70.0);
		assertThat(resultado.tempoUsado()).isEqualTo(Duration.ofMinutes(25));
	}

	@Test
	void vinteAcertosReprova() {
		Simulado prova = service.iniciar();
		for (int ordem = 1; ordem <= 20; ordem++) {
			service.responder(prova.getId(), ordem, correta(prova, ordem).getId(), Duration.ofSeconds(30));
		}

		assertThat(service.finalizar(prova.getId()).aprovado()).isFalse();
	}

	@Test
	void trocarDeIdeiaSubstituiARespostaEDeixarEmBrancoContaComoErro() {
		Simulado prova = service.iniciar();
		service.responder(prova.getId(), 1, correta(prova, 1).getId(), Duration.ofSeconds(20));
		service.responder(prova.getId(), 1, errada(prova, 1).getId(), Duration.ofSeconds(10));
		service.responder(prova.getId(), 2, correta(prova, 2).getId(), Duration.ofSeconds(20));
		service.responder(prova.getId(), 2, null, Duration.ofSeconds(5));

		ResultadoProva resultado = service.finalizar(prova.getId());

		assertThat(resultado.acertos()).isZero();
		assertThat(resultado.erros()).isEqualTo(1);
		assertThat(resultado.emBranco()).isEqualTo(29);
	}

	@Test
	void finalizarDeNovoDevolveOMesmoResultado() {
		Simulado prova = service.iniciar();
		relogio.avancar(Duration.ofMinutes(10));
		ResultadoProva primeiro = service.finalizar(prova.getId());
		relogio.avancar(Duration.ofMinutes(5));

		assertThat(service.finalizar(prova.getId())).isEqualTo(primeiro);
	}

	@Test
	void rejeitaAlternativaDeOutraQuestao() {
		Simulado prova = service.iniciar();
		long daQuestao2 = correta(prova, 2).getId();

		assertThatThrownBy(() -> service.responder(prova.getId(), 1, daQuestao2, Duration.ZERO))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void naoMontaProvaComQuestoesNaoRevisadas() {
		marcarTodasComoRevisadas(false);

		assertThatThrownBy(() -> service.iniciar())
			.isInstanceOfSatisfying(QuestoesInsuficientesException.class,
					e -> assertThat(e.getFaltas()).contains("legislacao: precisa de 12, há 0"));
	}

	@Test
	void simuladoInexistente() {
		assertThatThrownBy(() -> service.finalizar(999_999L)).isInstanceOf(SimuladoNaoEncontradoException.class);
	}

	/**
	 * Roda sem a transação do teste para provar que o encerramento é gravado mesmo com a exceção: com uma
	 * transação externa, o rollback (ou a falta dele) não seria observável.
	 */
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void respostaDepoisDoPrazoEncerraAProvaEGravaOResultado() {
		long id = service.iniciar().getId();
		try {
			service.responder(id, 1, null, Duration.ofSeconds(30));
			relogio.avancar(regras.duracao().plusSeconds(1));

			assertThatThrownBy(() -> service.responder(id, 2, null, Duration.ofSeconds(5)))
				.isInstanceOf(TempoEsgotadoException.class);

			Simulado salvo = simulados.findById(id).orElseThrow();
			assertThat(salvo.isFinalizado()).isTrue();
			assertThat(salvo.getFinalizadoEm()).isEqualTo(RelogioDeTeste.INICIO.plus(regras.duracao()));
			assertThat(salvo.getAcertos()).isZero();
		}
		finally {
			simulados.deleteById(id);
		}
	}

	private void marcarTodasComoRevisadas(boolean revisada) {
		List<Questao> todas = questoes.findAll();
		todas.forEach(questao -> questao.atualizarRevisao(questao.getOrigem(), revisada));
		questoes.saveAll(todas);
	}

	private static Map<String, Long> questoesPorDisciplina(Simulado prova) {
		return prova.getRespostas()
			.stream()
			.collect(Collectors.groupingBy(r -> r.getQuestao().getSubtema().getDisciplina().getCodigo(),
					Collectors.counting()));
	}

	private static List<Long> idsDasQuestoes(Simulado prova) {
		return prova.getRespostas().stream().map(r -> r.getQuestao().getId()).toList();
	}

	private static Alternativa correta(Simulado prova, int ordem) {
		return prova.questaoNaOrdem(ordem).alternativaCorreta().orElseThrow();
	}

	private static Alternativa errada(Simulado prova, int ordem) {
		return prova.questaoNaOrdem(ordem)
			.getAlternativas()
			.stream()
			.filter(alternativa -> !alternativa.isCorreta())
			.findFirst()
			.orElseThrow();
	}

}
