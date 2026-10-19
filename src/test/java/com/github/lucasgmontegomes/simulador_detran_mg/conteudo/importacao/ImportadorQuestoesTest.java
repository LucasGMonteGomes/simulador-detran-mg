package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.StatusQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.AlternativaArquivo;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.QuestaoArquivo;

@DataJpaTest
@Import({ TestcontainersConfiguration.class, ImportadorQuestoes.class, ConfiguracaoImportacao.class })
class ImportadorQuestoesTest {

	private static final String TRECHO = "quarenta quilômetros por hora, nas vias coletoras";

	@Autowired
	private ImportadorQuestoes importador;

	@Autowired
	private QuestaoRepository questoes;

	@Autowired
	private TestEntityManager em;

	@Test
	void criaQuestoesNovasComoRascunho() {
		ResultadoImportacao resultado = importar(questao("CIR-001", "40 km/h", false));

		assertThat(resultado).isEqualTo(new ResultadoImportacao(1, 0, 0));
		Questao salva = ativa("CIR-001");
		assertThat(salva.getVersao()).isEqualTo(1);
		assertThat(salva.getOrigem()).isEqualTo(OrigemQuestao.IA);
		assertThat(salva.getStatus()).isEqualTo(StatusQuestao.RASCUNHO);
		assertThat(salva.getTrechoFonte()).isEqualTo(TRECHO);
	}

	@Test
	void reimportarSemMudancasNaoCriaNovaVersao() {
		importar(questao("CIR-001", "40 km/h", false));

		ResultadoImportacao resultado = importar(questao("CIR-001", "40 km/h", false));

		assertThat(resultado).isEqualTo(new ResultadoImportacao(0, 0, 1));
		assertThat(questoes.count()).isEqualTo(1);
	}

	@Test
	void mudarConteudoCriaNovaVersaoEDesativaAAnterior() {
		importar(questao("CIR-001", "40 km/h", false));
		Long idVersao1 = ativa("CIR-001").getId();

		ResultadoImportacao resultado = importar(questao("CIR-001", "40 quilômetros por hora", false));

		assertThat(resultado).isEqualTo(new ResultadoImportacao(0, 1, 0));
		assertThat(ativa("CIR-001").getVersao()).isEqualTo(2);
		assertThat(questoes.findById(idVersao1)).hasValueSatisfying(v1 -> assertThat(v1.isAtiva()).isFalse());
	}

	@Test
	void marcarComoRevisadaMudaStatusSemCriarVersao() {
		importar(questao("CIR-001", "40 km/h", false));

		ResultadoImportacao resultado = importar(questao("CIR-001", "40 km/h", true));

		assertThat(resultado.inalteradas()).isEqualTo(1);
		assertThat(ativa("CIR-001").getStatus()).isEqualTo(StatusQuestao.REVISADA);
	}

	@Test
	void questaoReportadaSoSaiDesseEstadoComNovaVersao() {
		importar(questao("CIR-001", "40 km/h", true));
		ativa("CIR-001").reportar();
		em.flush();

		importar(questao("CIR-001", "40 km/h", true));
		assertThat(ativa("CIR-001").getStatus()).isEqualTo(StatusQuestao.REPORTADA);

		importar(questao("CIR-001", "40 km/h (corrigida)", true));
		assertThat(ativa("CIR-001").getStatus()).isEqualTo(StatusQuestao.REVISADA);
	}

	@Test
	void arquivoComErroNaoGravaNenhumaQuestao() {
		QuestaoArquivo valida = questao("CIR-001", "40 km/h", false);
		QuestaoArquivo trechoInventado = new QuestaoArquivo("CIR-002", null, false, "Enunciado", null,
				List.of(new AlternativaArquivo("A", true), new AlternativaArquivo("B", false)), "Explicação.",
				"CTB art. 61", "cento e vinte quilômetros por hora, nas vias coletoras");

		assertThatThrownBy(() -> importar(valida, trechoInventado))
			.isInstanceOf(QuestoesInvalidasException.class)
			.hasMessageContaining("CIR-002: trecho_fonte não encontrado");
		assertThat(questoes.count()).isZero();
	}

	private ResultadoImportacao importar(QuestaoArquivo... questoesArquivo) {
		ArquivoQuestoes arquivo = new ArquivoQuestoes("legislacao", "normas-circulacao", OrigemQuestao.IA,
				List.of(questoesArquivo));
		ResultadoImportacao resultado = importador.importar(List.of(new ArquivoLido("normas.yaml", arquivo)));
		em.flush();
		em.clear();
		return resultado;
	}

	private Questao ativa(String codigo) {
		return questoes.findByCodigoAndAtivaTrue(codigo).orElseThrow();
	}

	private static QuestaoArquivo questao(String codigo, String alternativaCorreta, boolean revisada) {
		return new QuestaoArquivo(codigo, null, revisada, "Velocidade máxima em via coletora sem sinalização:", null,
				List.of(new AlternativaArquivo("30 km/h", false), new AlternativaArquivo(alternativaCorreta, true)),
				"Sem sinalização, a via coletora tem limite de 40 km/h.", "CTB art. 61, § 1º, I, c", TRECHO);
	}

}
