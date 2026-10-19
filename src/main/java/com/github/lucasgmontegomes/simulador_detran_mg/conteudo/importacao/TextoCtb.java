package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.core.io.Resource;

/**
 * Texto vigente do Código de Trânsito Brasileiro, usado para conferir se o trecho citado por uma questão existe de
 * verdade na lei. Gerado por {@code scripts/extrair_ctb.py}.
 */
public class TextoCtb {

	private final String textoNormalizado;

	public TextoCtb(String texto) {
		this.textoNormalizado = normalizar(texto);
	}

	public static TextoCtb carregar(Resource recurso) {
		try {
			return new TextoCtb(recurso.getContentAsString(StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Não foi possível ler o texto do CTB em " + recurso, e);
		}
	}

	public boolean contem(String trecho) {
		String procurado = normalizar(trecho);
		return !procurado.isEmpty() && textoNormalizado.contains(procurado);
	}

	/**
	 * Ignora diferenças que não mudam o sentido: espaços e quebras de linha, maiúsculas e as variações tipográficas
	 * de aspas e travessões.
	 */
	static String normalizar(String texto) {
		return texto.replace(' ', ' ')
			.replaceAll("[“”]", "\"")
			.replaceAll("[‘’]", "'")
			.replaceAll("[–—]", "-")
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase(Locale.ROOT);
	}

}
