package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.github.lucasgmontegomes.simulador_detran_mg.TestcontainersConfiguration;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;

/**
 * Importa os arquivos reais de {@code src/main/resources/questoes}. Se algum trecho citado não existir no CTB ou
 * algum arquivo estiver malformado, este teste aponta exatamente onde.
 */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, ImportadorQuestoes.class, ConfiguracaoImportacao.class,
		LeitorArquivosQuestoes.class })
class BancoDeQuestoesTest {

	/** Questões por disciplina em uma prova de 30 (proporcional à carga horária do curso). */
	private static final Map<String, Integer> POR_PROVA = Map.of("legislacao", 12, "direcao-defensiva", 10,
			"primeiros-socorros", 3, "meio-ambiente", 3, "mecanica", 2);

	private static final int SIMULADOS_SEM_REPETIR = 2;

	@Autowired
	private LeitorArquivosQuestoes leitor;

	@Autowired
	private ImportadorQuestoes importador;

	@Autowired
	private QuestaoRepository questoes;

	@Test
	void todosOsArquivosSaoValidosEImportados() {
		ResultadoImportacao resultado = importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));

		assertThat(resultado.criadas()).isEqualTo(resultado.total()).isPositive();
	}

	@Test
	void haQuestoesSuficientesParaDoisSimuladosSemRepetir() {
		importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));

		Map<String, Long> porDisciplina = questoes.findAll()
			.stream()
			.collect(Collectors.groupingBy(q -> q.getSubtema().getDisciplina().getCodigo(), Collectors.counting()));

		POR_PROVA.forEach((disciplina, porProva) -> assertThat(porDisciplina.getOrDefault(disciplina, 0L))
			.as("questões de %s", disciplina)
			.isGreaterThanOrEqualTo((long) porProva * SIMULADOS_SEM_REPETIR));
	}

	@Test
	void gabaritoNaoSeConcentraEmUmaMesmaLetra() {
		importador.importar(leitor.ler(LeitorArquivosQuestoes.PADRAO_CLASSPATH));
		List<Questao> todas = questoes.findAll();

		Map<Integer, Long> porPosicao = todas.stream()
			.map(q -> q.alternativaCorreta().map(Alternativa::getOrdem).orElseThrow())
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		// Quem chuta sempre a mesma letra não pode ter vantagem: nenhuma posição passa de 35% do gabarito.
		assertThat(porPosicao.values()).allSatisfy(total -> assertThat(total).isLessThanOrEqualTo(todas.size() * 35 / 100));
	}

}
