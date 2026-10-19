package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoImportacao {

	@Bean
	TextoCtb textoCtb(@Value("classpath:fontes/ctb.txt") Resource recurso) {
		return TextoCtb.carregar(recurso);
	}

}
