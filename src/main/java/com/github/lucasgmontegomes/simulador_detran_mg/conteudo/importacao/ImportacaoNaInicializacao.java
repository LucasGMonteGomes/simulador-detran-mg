package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Importa os arquivos de {@code src/main/resources/questoes} sempre que a aplicação sobe. Se algum arquivo estiver
 * inválido, a aplicação não sobe e o log lista os problemas.
 */
@Component
@ConditionalOnBooleanProperty(name = "simulador.questoes.importar-na-inicializacao", matchIfMissing = true)
class ImportacaoNaInicializacao implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ImportacaoNaInicializacao.class);

	private final LeitorArquivosQuestoes leitor;

	private final ImportadorQuestoes importador;

	ImportacaoNaInicializacao(LeitorArquivosQuestoes leitor, ImportadorQuestoes importador) {
		this.leitor = leitor;
		this.importador = importador;
	}

	@Override
	public void run(ApplicationArguments args) {
		ResultadoImportacao resultado = importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));
		log.info("Questões importadas: {} novas, {} com nova versão, {} sem mudança ({} no total)",
				resultado.criadas(), resultado.novasVersoes(), resultado.inalteradas(), resultado.total());
	}

}
