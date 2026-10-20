package com.github.lucasgmontegomes.simulador_detran_mg.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ModoProvaService;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.RelogioDeTeste;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SimuladoRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, RelogioDeTeste.Configuracao.class })
class HistoricoWebTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ModoProvaService modoProva;

	@Autowired
	private QuestaoRepository questoes;

	@Autowired
	private SimuladoRepository simulados;

	@Autowired
	private RelogioDeTeste relogio;

	@Autowired
	private TransactionTemplate transacao;

	@BeforeEach
	void preparar() {
		relogio.definir(RelogioDeTeste.INICIO);
	}

	@AfterEach
	void limpar() {
		simulados.deleteAll();
		marcarQuestoesComoRevisadas(false);
	}

	@Test
	void semProvasConvidaAFazerAPrimeira() {
		assertThat(mvc.get().uri("/historico")).hasStatusOk()
			.hasViewName("historico")
			.bodyText()
			.contains("Você ainda não terminou nenhuma prova")
			.doesNotContain("tabela-provas");
	}

	@Test
	void comProvasMostraResumoTabelaParaOGraficoESubtemas() {
		marcarQuestoesComoRevisadas(true);
		long primeira = fazerProva(12);
		fazerProva(22);

		assertThat(mvc.get().uri("/historico")).hasStatusOk()
			.bodyText()
			.contains("Provas feitas", "<strong>22/30</strong>", "+10 em relação à anterior", "1 de 2")
			.contains("id=\"tabela-provas\"", "data-numero=\"2\"", "data-acertos=\"22\"", "data-aprovado=\"true\"")
			.contains("href=\"/prova/" + primeira + "/resultado\"", "✗ Reprovado", "✓ Aprovado")
			.contains("Desempenho por subtema", "/js/historico.js");
	}

	@Test
	void menuLevaAoHistoricoDeQualquerTela() {
		assertThat(mvc.get().uri("/")).bodyText().contains("href=\"/historico\"");
	}

	private long fazerProva(int acertos) {
		long id = modoProva.iniciar().getId();
		for (int ordem = 1; ordem <= acertos; ordem++) {
			modoProva.responder(id, ordem, alternativaCorreta(id, ordem), Duration.ofSeconds(30));
		}
		relogio.avancar(Duration.ofMinutes(25));
		modoProva.finalizar(id);
		relogio.avancar(Duration.ofDays(1));
		return id;
	}

	private long alternativaCorreta(long simuladoId, int ordem) {
		return transacao.execute(status -> simulados.findById(simuladoId)
			.orElseThrow()
			.questaoNaOrdem(ordem)
			.alternativaCorreta()
			.orElseThrow()
			.getId());
	}

	private void marcarQuestoesComoRevisadas(boolean revisada) {
		transacao.executeWithoutResult(
				status -> questoes.findAll().forEach(q -> q.atualizarRevisao(q.getOrigem(), revisada)));
	}

}
