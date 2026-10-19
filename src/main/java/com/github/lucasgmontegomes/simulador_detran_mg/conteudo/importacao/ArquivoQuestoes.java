package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.util.List;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.ConteudoQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;

/**
 * Um arquivo YAML de questões: todas do mesmo subtema. Exemplo:
 *
 * <pre>
 * disciplina: legislacao
 * subtema: normas-circulacao
 * origem: ia
 * questoes:
 *   - codigo: CIR-001
 *     revisada: false
 *     enunciado: ...
 *     alternativas:
 *       - texto: ...
 *       - texto: ...
 *         correta: true
 *     explicacao: ...
 *     referencia: CTB art. 61, § 1º, I, c
 *     trecho_fonte: quarenta quilômetros por hora, nas vias coletoras
 * </pre>
 */
public record ArquivoQuestoes(String disciplina, String subtema, OrigemQuestao origem, List<QuestaoArquivo> questoes) {

	public ArquivoQuestoes {
		questoes = questoes == null ? List.of() : List.copyOf(questoes);
	}

	/**
	 * @param origem sobrescreve a origem do arquivo só para esta questão (opcional)
	 * @param revisada {@code true} depois que você conferiu gabarito e explicação
	 */
	public record QuestaoArquivo(String codigo, OrigemQuestao origem, boolean revisada, String enunciado,
			String imagem, List<AlternativaArquivo> alternativas, String explicacao, String referencia,
			String trechoFonte) {

		public QuestaoArquivo {
			alternativas = alternativas == null ? List.of() : List.copyOf(alternativas);
		}

		public ConteudoQuestao conteudo() {
			List<ConteudoQuestao.Opcao> opcoes = alternativas.stream()
				.map(alternativa -> new ConteudoQuestao.Opcao(limpar(alternativa.texto()), alternativa.correta()))
				.toList();
			return new ConteudoQuestao(limpar(enunciado), limpar(imagem), opcoes, limpar(explicacao),
					limpar(referencia), limpar(trechoFonte));
		}

		/** Blocos {@code >} do YAML terminam com quebra de linha; não queremos isso no banco. */
		private static String limpar(String texto) {
			return texto == null || texto.isBlank() ? null : texto.strip();
		}

	}

	public record AlternativaArquivo(String texto, boolean correta) {
	}

}
