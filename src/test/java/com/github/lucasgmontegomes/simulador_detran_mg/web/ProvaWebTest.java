package com.github.lucasgmontegomes.simulador_detran_mg.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.RelogioDeTeste;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SimuladoRepository;

/**
 * Percorre as telas como o navegador faria, com os templates renderizados de verdade.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, RelogioDeTeste.Configuracao.class })
class ProvaWebTest {

	private static final Pattern URL_DA_PROVA = Pattern.compile("/prova/(\\d+)/");

	private static final Duration DEPOIS_DO_PRAZO = Duration.ofMinutes(41);

	@Autowired
	private MockMvcTester mvc;

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
	void inicioMostraAsRegrasEOBotaoDeIniciar() {
		assertThat(mvc.get().uri("/")).hasStatusOk()
			.hasViewName("inicio")
			.bodyText()
			.contains("Iniciar prova", "40 minutos", "Legislação de trânsito", "21 acertos");
	}

	@Test
	void semQuestoesRevisadasVoltaAoInicioExplicandoOQueFalta() {
		MvcTestResult tentativa = mvc.post().uri("/prova").exchange();
		assertThat(tentativa).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/").flash().containsKey("faltas");

		List<?> faltas = (List<?>) tentativa.getMvcResult().getFlashMap().get("faltas");
		assertThat(mvc.get().uri("/").flashAttr("faltas", faltas)).hasStatusOk()
			.bodyText()
			.contains("Ainda não dá para montar uma prova", "legislacao: precisa de 12, há 0");
	}

	@Test
	void fluxoCompletoDaProva() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();

		assertThat(mvc.get().uri("/prova/{id}/questoes/1", id)).hasStatusOk()
			.hasViewName("prova/questao")
			.bodyText()
			.contains("Questão <strong>1</strong> de <span>30</span>", "data-restante-ms=\"2400000\"", "40:00");

		assertThat(mvc.post()
			.uri("/prova/{id}/questoes/1", id)
			.param("alternativaId", String.valueOf(alternativa(id, 1, true)))
			.param("tempoGastoMs", "30000")
			.param("destino", "2")).hasRedirectedUrl("/prova/" + id + "/questoes/2");

		assertThat(mvc.post()
			.uri("/prova/{id}/questoes/2/resposta", id)
			.param("alternativaId", String.valueOf(alternativa(id, 2, false)))
			.param("tempoGastoMs", "15000")).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/")).bodyText().contains("Continuar prova").doesNotContain("Iniciar prova");

		relogio.avancar(Duration.ofMinutes(12));
		assertThat(mvc.post().uri("/prova/{id}/questoes/3", id).param("destino", "encerrar"))
			.hasRedirectedUrl("/prova/" + id + "/resultado");

		assertThat(mvc.get().uri("/prova/{id}/resultado", id)).hasStatusOk()
			.hasViewName("prova/resultado")
			.bodyText()
			.contains("Reprovado", "<strong>1</strong><span>/30</span>", "12:00", "Gabarito comentado",
					"Resposta correta", "Sua resposta");
	}

	@Test
	void iniciarDeNovoLevaAProvaQueJaEstaAberta() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();

		assertThat(mvc.post().uri("/prova")).hasRedirectedUrl("/prova/" + id + "/questoes/1")
			.flash()
			.containsKey("aviso");
	}

	@Test
	void salvamentoAutomaticoDepoisDoPrazoResponde409ComOEnderecoDoResultado() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();
		relogio.avancar(DEPOIS_DO_PRAZO);

		assertThat(mvc.post()
			.uri("/prova/{id}/questoes/1/resposta", id)
			.param("alternativaId", String.valueOf(alternativa(id, 1, true)))).hasStatus(HttpStatus.CONFLICT)
			.hasHeader("Location", "/prova/" + id + "/resultado");
	}

	@Test
	void formularioDepoisDoPrazoLevaAoResultadoComAviso() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();
		relogio.avancar(DEPOIS_DO_PRAZO);

		assertThat(mvc.post().uri("/prova/{id}/questoes/1", id).param("destino", "2"))
			.hasRedirectedUrl("/prova/" + id + "/resultado")
			.flash()
			.containsEntry("tempoEsgotado", true);
		assertThat(mvc.get().uri("/prova/{id}/resultado", id).flashAttr("tempoEsgotado", true)).bodyText()
			.contains("O tempo acabou", "<span>40:00</span>", "<small>de 40:00</small>");
	}

	@Test
	void abrirUmaQuestaoDepoisDoPrazoEncerraAProva() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();
		relogio.avancar(DEPOIS_DO_PRAZO);

		assertThat(mvc.get().uri("/prova/{id}/questoes/5", id)).hasRedirectedUrl("/prova/" + id + "/resultado");
	}

	@Test
	void resultadoAntesDeEncerrarVoltaParaAProva() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();

		assertThat(mvc.get().uri("/prova/{id}/resultado", id)).hasRedirectedUrl("/prova/" + id + "/questoes/1");
	}

	@Test
	void provaInexistenteMostraPaginaDeErro() {
		assertThat(mvc.get().uri("/prova/999999/questoes/1")).hasStatus(HttpStatus.NOT_FOUND)
			.bodyText()
			.contains("Prova não encontrada", "Voltar ao início");
	}

	@Test
	void questaoForaDaProvaEhPedidoInvalido() {
		marcarQuestoesComoRevisadas(true);
		long id = iniciarProva();

		assertThat(mvc.get().uri("/prova/{id}/questoes/31", id)).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyText()
			.contains("Pedido inválido");
	}

	private long iniciarProva() {
		MvcTestResult resultado = mvc.post().uri("/prova").exchange();
		String destino = resultado.getResponse().getRedirectedUrl();
		assertThat(destino).as("redirect depois de iniciar").matches("/prova/\\d+/questoes/1");
		Matcher matcher = URL_DA_PROVA.matcher(destino);
		assertThat(matcher.find()).isTrue();
		return Long.parseLong(matcher.group(1));
	}

	private long alternativa(long simuladoId, int ordem, boolean correta) {
		return transacao.execute(status -> simulados.findById(simuladoId)
			.orElseThrow()
			.questaoNaOrdem(ordem)
			.getAlternativas()
			.stream()
			.filter(alternativa -> alternativa.isCorreta() == correta)
			.map(Alternativa::getId)
			.findFirst()
			.orElseThrow());
	}

	private void marcarQuestoesComoRevisadas(boolean revisada) {
		transacao.executeWithoutResult(
				status -> questoes.findAll().forEach(q -> q.atualizarRevisao(q.getOrigem(), revisada)));
	}

}
