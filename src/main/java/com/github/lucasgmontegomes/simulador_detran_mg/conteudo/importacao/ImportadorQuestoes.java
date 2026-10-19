package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.ConteudoQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Subtema;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.SubtemaRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.QuestaoArquivo;

/**
 * Sincroniza os arquivos YAML (fonte da verdade do conteúdo) com o banco. Pode rodar quantas vezes quiser: só
 * cria o que é novo e só versiona o que mudou.
 */
@Service
public class ImportadorQuestoes {

	private enum Situacao {
		CRIADA, NOVA_VERSAO, INALTERADA
	}

	private final SubtemaRepository subtemas;

	private final QuestaoRepository questoes;

	private final TextoCtb ctb;

	public ImportadorQuestoes(SubtemaRepository subtemas, QuestaoRepository questoes, TextoCtb ctb) {
		this.subtemas = subtemas;
		this.questoes = questoes;
		this.ctb = ctb;
	}

	/**
	 * Valida tudo antes de gravar qualquer coisa: com um erro em qualquer arquivo, nada é importado.
	 *
	 * @throws QuestoesInvalidasException com a lista completa de problemas encontrados
	 */
	@Transactional
	public ResultadoImportacao importar(List<ArquivoLido> arquivos) {
		Map<String, Subtema> subtemaPorCodigo = subtemas.findAll()
			.stream()
			.collect(Collectors.toMap(Subtema::getCodigo, Function.identity()));
		Map<String, String> disciplinaPorSubtema = subtemaPorCodigo.values()
			.stream()
			.collect(Collectors.toMap(Subtema::getCodigo, subtema -> subtema.getDisciplina().getCodigo()));

		List<String> erros = new ValidadorQuestoes(ctb, disciplinaPorSubtema).validar(arquivos);
		if (!erros.isEmpty()) {
			throw new QuestoesInvalidasException(erros);
		}

		int criadas = 0;
		int novasVersoes = 0;
		int inalteradas = 0;
		for (ArquivoLido lido : arquivos) {
			Subtema subtema = subtemaPorCodigo.get(lido.arquivo().subtema());
			for (QuestaoArquivo questao : lido.arquivo().questoes()) {
				OrigemQuestao origem = questao.origem() != null ? questao.origem() : lido.arquivo().origem();
				switch (sincronizar(questao, subtema, origem)) {
					case CRIADA -> criadas++;
					case NOVA_VERSAO -> novasVersoes++;
					case INALTERADA -> inalteradas++;
				}
			}
		}
		return new ResultadoImportacao(criadas, novasVersoes, inalteradas);
	}

	private Situacao sincronizar(QuestaoArquivo questaoArquivo, Subtema subtema, OrigemQuestao origem) {
		ConteudoQuestao conteudo = questaoArquivo.conteudo();
		Optional<Questao> atual = questoes.findByCodigoAndAtivaTrue(questaoArquivo.codigo());

		if (atual.isPresent() && atual.get().temConteudo(subtema, conteudo)) {
			atual.get().atualizarRevisao(origem, questaoArquivo.revisada());
			return Situacao.INALTERADA;
		}

		atual.ifPresent(antiga -> {
			antiga.desativar();
			// O índice único parcial (uma versão ativa por código) é checado a cada comando SQL. Sem este flush,
			// o INSERT da nova versão iria ao banco antes do UPDATE que desativa a antiga.
			questoes.flush();
		});
		int versao = questoes.findMaiorVersao(questaoArquivo.codigo()) + 1;
		Questao nova = new Questao(subtema, questaoArquivo.codigo(), versao, origem, conteudo);
		nova.atualizarRevisao(origem, questaoArquivo.revisada());
		questoes.save(nova);
		return atual.isPresent() ? Situacao.NOVA_VERSAO : Situacao.CRIADA;
	}

}
