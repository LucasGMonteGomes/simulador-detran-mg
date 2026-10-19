package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.QuestaoArquivo;

class LeitorArquivosQuestoesTest {

	private final LeitorArquivosQuestoes leitor = new LeitorArquivosQuestoes();

	@Test
	void leCamposEmSnakeCaseEnumSemDiferenciarMaiusculasEPadroesOmitidos() {
		List<ArquivoLido> lidos = leitor.ler("classpath*:importacao-teste/valido/*.yaml");

		ArquivoQuestoes arquivo = lidos.getFirst().arquivo();
		assertThat(arquivo.origem()).isEqualTo(OrigemQuestao.IA);
		QuestaoArquivo primeira = arquivo.questoes().getFirst();
		assertThat(primeira.revisada()).isTrue();
		assertThat(primeira.trechoFonte()).isEqualTo("quarenta quilômetros por hora, nas vias coletoras");
		assertThat(primeira.alternativas()).extracting(ArquivoQuestoes.AlternativaArquivo::correta)
			.containsExactly(false, true);
		QuestaoArquivo segunda = arquivo.questoes().get(1);
		assertThat(segunda.origem()).isEqualTo(OrigemQuestao.MANUAL);
		assertThat(segunda.revisada()).isFalse();
	}

	@Test
	void conteudoRemoveQuebraDeLinhaFinalDosBlocosYaml() {
		QuestaoArquivo questao = leitor.ler("classpath*:importacao-teste/valido/*.yaml")
			.getFirst()
			.arquivo()
			.questoes()
			.getFirst();

		assertThat(questao.enunciado()).endsWith("\n");
		assertThat(questao.conteudo().enunciado()).isEqualTo(
				"Em uma via urbana coletora sem sinalização de velocidade, a velocidade máxima permitida é de:");
	}

	@Test
	void campoDesconhecidoFalhaComONomeDoArquivo() {
		assertThatThrownBy(() -> leitor.ler("classpath*:importacao-teste/campo-desconhecido/*.yaml"))
			.isInstanceOf(QuestoesInvalidasException.class)
			.hasMessageContaining("erro.yaml: YAML inválido")
			.hasMessageContaining("explicaçao");
	}

}
