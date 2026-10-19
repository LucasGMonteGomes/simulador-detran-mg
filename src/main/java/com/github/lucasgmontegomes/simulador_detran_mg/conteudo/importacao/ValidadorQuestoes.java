package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.AlternativaArquivo;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao.ArquivoQuestoes.QuestaoArquivo;

/**
 * Regras que um arquivo de questões precisa cumprir antes de qualquer coisa ir para o banco. Algumas repetem
 * constraints do banco de propósito: aqui a mensagem diz o arquivo e o código da questão.
 */
class ValidadorQuestoes {

	private static final int MIN_ALTERNATIVAS = 2;

	private static final int MAX_ALTERNATIVAS = 5;

	private static final int MAX_CODIGO = 30;

	private static final int MAX_REFERENCIA = 150;

	private final TextoCtb ctb;

	private final Map<String, String> disciplinaPorSubtema;

	ValidadorQuestoes(TextoCtb ctb, Map<String, String> disciplinaPorSubtema) {
		this.ctb = ctb;
		this.disciplinaPorSubtema = disciplinaPorSubtema;
	}

	List<String> validar(List<ArquivoLido> arquivos) {
		List<String> erros = new ArrayList<>();
		Set<String> codigosVistos = new HashSet<>();
		for (ArquivoLido lido : arquivos) {
			validarArquivo(lido, erros);
			for (QuestaoArquivo questao : lido.arquivo().questoes()) {
				validarQuestao(lido.nome(), questao, codigosVistos, erros);
			}
		}
		return erros;
	}

	private void validarArquivo(ArquivoLido lido, List<String> erros) {
		ArquivoQuestoes arquivo = lido.arquivo();
		String nome = lido.nome();
		if (arquivo.origem() == null) {
			erros.add(nome + ": falta o campo 'origem' (manual ou ia)");
		}
		String disciplinaDoSubtema = disciplinaPorSubtema.get(arquivo.subtema());
		if (disciplinaDoSubtema == null) {
			erros.add(nome + ": subtema '" + arquivo.subtema() + "' não existe");
		}
		else if (!disciplinaDoSubtema.equals(arquivo.disciplina())) {
			erros.add(nome + ": subtema '" + arquivo.subtema() + "' pertence à disciplina '" + disciplinaDoSubtema
					+ "', não a '" + arquivo.disciplina() + "'");
		}
		if (arquivo.questoes().isEmpty()) {
			erros.add(nome + ": nenhuma questão no arquivo");
		}
	}

	private void validarQuestao(String nomeArquivo, QuestaoArquivo questao, Set<String> codigosVistos,
			List<String> erros) {
		String codigo = questao.codigo();
		String local = nomeArquivo + " › " + (isVazio(codigo) ? "(sem código)" : codigo) + ": ";

		if (isVazio(codigo)) {
			erros.add(local + "falta o 'codigo'");
		}
		else if (codigo.length() > MAX_CODIGO) {
			erros.add(local + "código com mais de " + MAX_CODIGO + " caracteres");
		}
		else if (!codigosVistos.add(codigo)) {
			erros.add(local + "código repetido");
		}
		if (isVazio(questao.enunciado())) {
			erros.add(local + "falta o 'enunciado'");
		}
		if (isVazio(questao.explicacao())) {
			erros.add(local + "falta a 'explicacao'");
		}
		validarAlternativas(local, questao.alternativas(), erros);
		validarReferencia(local, questao, erros);
	}

	private void validarAlternativas(String local, List<AlternativaArquivo> alternativas, List<String> erros) {
		if (alternativas.size() < MIN_ALTERNATIVAS || alternativas.size() > MAX_ALTERNATIVAS) {
			erros.add(local + "precisa ter de " + MIN_ALTERNATIVAS + " a " + MAX_ALTERNATIVAS + " alternativas (tem "
					+ alternativas.size() + ")");
		}
		long corretas = alternativas.stream().filter(AlternativaArquivo::correta).count();
		if (corretas != 1) {
			erros.add(local + "precisa ter exatamente uma alternativa correta (tem " + corretas + ")");
		}
		Set<String> textos = new HashSet<>();
		for (AlternativaArquivo alternativa : alternativas) {
			if (isVazio(alternativa.texto())) {
				erros.add(local + "alternativa sem texto");
			}
			else if (!textos.add(TextoCtb.normalizar(alternativa.texto()))) {
				erros.add(local + "alternativa repetida: " + alternativa.texto().strip());
			}
		}
	}

	private void validarReferencia(String local, QuestaoArquivo questao, List<String> erros) {
		String referencia = questao.referencia();
		if (referencia != null && referencia.strip().length() > MAX_REFERENCIA) {
			erros.add(local + "referência com mais de " + MAX_REFERENCIA + " caracteres");
		}
		if (!citaCtb(referencia)) {
			return;
		}
		if (isVazio(questao.trechoFonte())) {
			erros.add(local + "questões que citam o CTB precisam de 'trecho_fonte'");
		}
		else if (!ctb.contem(questao.trechoFonte())) {
			erros.add(local + "trecho_fonte não encontrado no texto vigente do CTB: \""
					+ questao.trechoFonte().strip() + "\"");
		}
	}

	private static boolean citaCtb(String referencia) {
		return referencia != null && referencia.strip().toUpperCase(Locale.ROOT).startsWith("CTB");
	}

	private static boolean isVazio(String texto) {
		return texto == null || texto.isBlank();
	}

}
