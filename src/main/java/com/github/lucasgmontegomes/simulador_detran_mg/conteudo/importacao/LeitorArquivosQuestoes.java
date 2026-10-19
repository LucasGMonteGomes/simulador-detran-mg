package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.dataformat.yaml.YAMLMapper;

@Component
public class LeitorArquivosQuestoes {

	public static final String PADRAO_CLASSPATH = "classpath*:questoes/**/*.yaml";

	private static final String PASTA = "/questoes/";

	private final YAMLMapper mapper = YAMLMapper.builder()
		.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
		.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
		// O Jackson 3 ignora campos desconhecidos por padrão; aqui um "explicaçao" digitado errado deve falhar.
		.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		// Permite omitir "correta: false" e "revisada: false".
		.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.build();

	public List<ArquivoLido> ler(String padrao) {
		Resource[] recursos = buscar(padrao);
		Arrays.sort(recursos, Comparator.comparing(LeitorArquivosQuestoes::nome));
		List<ArquivoLido> lidos = new ArrayList<>();
		List<String> erros = new ArrayList<>();
		for (Resource recurso : recursos) {
			try (InputStream entrada = recurso.getInputStream()) {
				lidos.add(new ArquivoLido(nome(recurso), mapper.readValue(entrada, ArquivoQuestoes.class)));
			}
			catch (JacksonException e) {
				erros.add(nome(recurso) + ": YAML inválido - " + e.getOriginalMessage());
			}
			catch (IOException e) {
				throw new UncheckedIOException("Não foi possível ler " + recurso, e);
			}
		}
		if (!erros.isEmpty()) {
			throw new QuestoesInvalidasException(erros);
		}
		return lidos;
	}

	private static Resource[] buscar(String padrao) {
		try {
			return new PathMatchingResourcePatternResolver().getResources(padrao);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Não foi possível listar " + padrao, e);
		}
	}

	private static String nome(Resource recurso) {
		try {
			String caminho = recurso.getURI().toString();
			int inicio = caminho.lastIndexOf(PASTA);
			return inicio >= 0 ? caminho.substring(inicio + PASTA.length()) : recurso.getFilename();
		}
		catch (IOException e) {
			return recurso.getDescription();
		}
	}

}
