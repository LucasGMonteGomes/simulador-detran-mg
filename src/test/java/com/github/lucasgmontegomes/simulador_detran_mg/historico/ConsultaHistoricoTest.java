package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ConfiguracaoImportacao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ImportadorQuestoes;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.LeitorArquivosQuestoes;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ConfiguracaoSimulado;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ModoProvaService;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.RelogioDeTeste;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.Simulado;

@DataJpaTest
@Import({ TestcontainersConfiguration.class, ImportadorQuestoes.class, ConfiguracaoImportacao.class,
		LeitorArquivosQuestoes.class, ModoProvaService.class, RelogioDeTeste.Configuracao.class,
		ConfiguracaoSimulado.class, ConsultaHistorico.class })
class ConsultaHistoricoTest {

	@Autowired
	private ConsultaHistorico consulta;

	@Autowired
	private ModoProvaService modoProva;

	@Autowired
	private ImportadorQuestoes importador;

	@Autowired
	private LeitorArquivosQuestoes leitor;

	@Autowired
	private QuestaoRepository questoes;

	@Autowired
	private RelogioDeTeste relogio;

	@Autowired
	private TestEntityManager em;

	@BeforeEach
	void prepararQuestoesRevisadas() {
		relogio.definir(RelogioDeTeste.INICIO);
		importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));
		List<Questao> todas = questoes.findAll();
		todas.forEach(q -> q.atualizarRevisao(q.getOrigem(), true));
	}

	@Test
	void semProvasOHistoricoFicaVazio() {
		assertThat(consulta.provas()).isEmpty();
		assertThat(consulta.desempenhoPorSubtema()).isEmpty();
	}

	@Test
	void listaAsProvasDaMaisAntigaParaAMaisRecente() {
		fazerProva(10);
		fazerProva(18);
		fazerProva(25);

		List<ProvaRealizada> provas = consulta.provas();

		assertThat(provas).extracting(ProvaRealizada::numero).containsExactly(1, 2, 3);
		assertThat(provas).extracting(ProvaRealizada::acertos).containsExactly(10, 18, 25);
		assertThat(provas).extracting(ProvaRealizada::emBranco).containsExactly(20, 12, 5);
		assertThat(provas).extracting(ProvaRealizada::aprovado).containsExactly(false, false, true);
		assertThat(provas).allSatisfy(p -> assertThat(p.tempoUsado()).isEqualTo(Duration.ofMinutes(20)));
	}

	@Test
	void provaEmAndamentoNaoEntraNoHistorico() {
		fazerProva(10);
		modoProva.iniciar();
		em.flush();

		assertThat(consulta.provas()).hasSize(1);
	}

	@Test
	void desempenhoPorSubtemaSeparaAsTresProvasRecentesDasAnteriores() {
		fazerProva(5);
		fazerProva(10);
		fazerProva(15);
		fazerProva(20);

		List<DesempenhoSubtema> subtemas = consulta.desempenhoPorSubtema();

		assertThat(subtemas.stream().mapToInt(s -> s.geral().total()).sum()).isEqualTo(120);
		assertThat(subtemas.stream().mapToInt(s -> s.geral().acertos()).sum()).isEqualTo(50);
		assertThat(subtemas.stream().mapToInt(s -> s.recente().total()).sum()).isEqualTo(90);
		assertThat(subtemas.stream().mapToInt(s -> s.recente().acertos()).sum()).isEqualTo(45);
		assertThat(subtemas.stream().mapToInt(s -> s.anterior().total()).sum()).isEqualTo(30);
		assertThat(subtemas.stream().mapToInt(s -> s.anterior().acertos()).sum()).isEqualTo(5);
	}

	@Test
	void subtemasVemDoMaisFracoParaOMaisForte() {
		fazerProva(8);
		fazerProva(22);

		assertThat(consulta.desempenhoPorSubtema()).extracting(DesempenhoSubtema::taxaSuavizada).isSorted();
	}

	/** Acerta as primeiras {@code acertos} questões, deixa o resto em branco e encerra após 20 minutos. */
	private void fazerProva(int acertos) {
		Simulado prova = modoProva.iniciar();
		for (int ordem = 1; ordem <= acertos; ordem++) {
			long correta = prova.questaoNaOrdem(ordem).alternativaCorreta().orElseThrow().getId();
			modoProva.responder(prova.getId(), ordem, correta, Duration.ofSeconds(30));
		}
		relogio.avancar(Duration.ofMinutes(20));
		modoProva.finalizar(prova.getId());
		relogio.avancar(Duration.ofDays(1));
		// O JdbcClient lê direto do banco: o que o JPA ainda não enviou não aparece na consulta.
		em.flush();
	}

}
