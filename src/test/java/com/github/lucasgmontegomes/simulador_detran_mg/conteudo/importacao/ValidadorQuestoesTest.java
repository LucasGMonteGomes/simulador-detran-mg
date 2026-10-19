package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.AlternativaArquivo;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.QuestaoArquivo;

class ValidadorQuestoesTest {

	private static final String TRECHO = "quarenta quilômetros por hora, nas vias coletoras";

	private final ValidadorQuestoes validador = new ValidadorQuestoes(
			new TextoCtb("c) " + TRECHO + ";\nd) trinta quilômetros por hora, nas vias locais;"),
			Map.of("normas-circulacao", "legislacao", "condicoes-adversas", "direcao-defensiva"));

	@Test
	void arquivoValidoNaoTemErros() {
		assertThat(validar(arquivo(questao("CIR-001")))).isEmpty();
	}

	@Test
	void exigeExatamenteUmaAlternativaCorreta() {
		QuestaoArquivo semCorreta = questao("CIR-001",
				List.of(new AlternativaArquivo("30 km/h", false), new AlternativaArquivo("40 km/h", false)));
		QuestaoArquivo duasCorretas = questao("CIR-002",
				List.of(new AlternativaArquivo("30 km/h", true), new AlternativaArquivo("40 km/h", true)));

		assertThat(validar(arquivo(semCorreta, duasCorretas))).containsExactly(
				"normas.yaml › CIR-001: precisa ter exatamente uma alternativa correta (tem 0)",
				"normas.yaml › CIR-002: precisa ter exatamente uma alternativa correta (tem 2)");
	}

	@Test
	void rejeitaAlternativasRepetidas() {
		QuestaoArquivo questao = questao("CIR-001",
				List.of(new AlternativaArquivo("40 km/h", true), new AlternativaArquivo("40  KM/H", false)));

		assertThat(validar(arquivo(questao))).containsExactly("normas.yaml › CIR-001: alternativa repetida: 40  KM/H");
	}

	@Test
	void detectaCodigoRepetidoEntreArquivos() {
		List<String> erros = validador.validar(List.of(new ArquivoLido("a.yaml", arquivo(questao("CIR-001"))),
				new ArquivoLido("b.yaml", arquivo(questao("CIR-001")))));

		assertThat(erros).containsExactly("b.yaml › CIR-001: código repetido");
	}

	@Test
	void rejeitaSubtemaDeOutraDisciplina() {
		ArquivoQuestoes arquivo = new ArquivoQuestoes("legislacao", "condicoes-adversas", OrigemQuestao.IA,
				List.of(questao("DDA-001")));

		assertThat(validar(arquivo)).containsExactly(
				"normas.yaml: subtema 'condicoes-adversas' pertence à disciplina 'direcao-defensiva', não a 'legislacao'");
	}

	@Test
	void questaoQueCitaCtbPrecisaDeTrechoQueExistaNaLei() {
		QuestaoArquivo semTrecho = questaoComTrecho("CIR-001", "CTB art. 61", null);
		QuestaoArquivo trechoInventado = questaoComTrecho("CIR-002", "CTB art. 61", "cinquenta quilômetros por hora");

		assertThat(validar(arquivo(semTrecho, trechoInventado))).containsExactly(
				"normas.yaml › CIR-001: questões que citam o CTB precisam de 'trecho_fonte'",
				"normas.yaml › CIR-002: trecho_fonte não encontrado no texto vigente do CTB: \"cinquenta quilômetros por hora\"");
	}

	@Test
	void questaoComOutraFonteNaoPrecisaDeTrecho() {
		QuestaoArquivo questao = questaoComTrecho("CIR-001", "Manual Brasileiro de Sinalização de Trânsito", null);

		assertThat(validar(arquivo(questao))).isEmpty();
	}

	@Test
	void acumulaTodosOsErrosDeUmaVez() {
		QuestaoArquivo vazia = new QuestaoArquivo(null, null, false, " ", null, List.of(), null, null, null);

		assertThat(validar(new ArquivoQuestoes("legislacao", "nao-existe", null, List.of(vazia)))).containsExactly(
				"normas.yaml: falta o campo 'origem' (manual ou ia)", "normas.yaml: subtema 'nao-existe' não existe",
				"normas.yaml › (sem código): falta o 'codigo'", "normas.yaml › (sem código): falta o 'enunciado'",
				"normas.yaml › (sem código): falta a 'explicacao'",
				"normas.yaml › (sem código): precisa ter de 2 a 5 alternativas (tem 0)",
				"normas.yaml › (sem código): precisa ter exatamente uma alternativa correta (tem 0)");
	}

	private List<String> validar(ArquivoQuestoes arquivo) {
		return validador.validar(List.of(new ArquivoLido("normas.yaml", arquivo)));
	}

	private static ArquivoQuestoes arquivo(QuestaoArquivo... questoes) {
		return new ArquivoQuestoes("legislacao", "normas-circulacao", OrigemQuestao.IA, List.of(questoes));
	}

	private static QuestaoArquivo questao(String codigo) {
		return questao(codigo, List.of(new AlternativaArquivo("30 km/h", false), new AlternativaArquivo("40 km/h", true)));
	}

	private static QuestaoArquivo questao(String codigo, List<AlternativaArquivo> alternativas) {
		return new QuestaoArquivo(codigo, null, false, "Velocidade máxima em via coletora sem sinalização:", null,
				alternativas, "Explicação.", "CTB art. 61, § 1º, I, c", TRECHO);
	}

	private static QuestaoArquivo questaoComTrecho(String codigo, String referencia, String trecho) {
		return new QuestaoArquivo(codigo, null, false, "Enunciado", null,
				List.of(new AlternativaArquivo("A", true), new AlternativaArquivo("B", false)), "Explicação.",
				referencia, trecho);
	}

}
