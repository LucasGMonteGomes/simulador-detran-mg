package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.ConteudoQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.ConteudoQuestao.Opcao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.OrigemQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SorteioQuestoes.Candidata;

/**
 * O sorteio é aleatório, então cada regra é conferida com várias sementes: ela precisa valer em todas.
 */
class SorteioQuestoesTest {

	static IntStream sementes() {
		return IntStream.range(0, 30);
	}

	@ParameterizedTest
	@MethodSource("sementes")
	void respeitaAQuantidadeDeCadaDisciplinaSemRepetir(int semente) {
		List<Candidata> candidatas = new ArrayList<>();
		candidatas.addAll(varias("LEG", "legislacao", 1L, 4, 0));
		candidatas.addAll(varias("LEX", "legislacao", 2L, 4, 0));
		candidatas.addAll(varias("DDE", "direcao-defensiva", 3L, 5, 0));

		List<Questao> sorteadas = sorteio(semente).sortear(Map.of("legislacao", 6, "direcao-defensiva", 3),
				candidatas);

		assertThat(sorteadas).hasSize(9).doesNotHaveDuplicates();
		assertThat(sorteadas).filteredOn(q -> q.getCodigo().startsWith("DDE")).hasSize(3);
	}

	@ParameterizedTest
	@MethodSource("sementes")
	void prefereQuestoesQueApareceramMenos(int semente) {
		List<Candidata> candidatas = new ArrayList<>();
		candidatas.addAll(varias("NOVA", "legislacao", 1L, 2, 0));
		candidatas.addAll(varias("VISTA", "legislacao", 1L, 3, 2));
		candidatas.addAll(varias("MUITO", "legislacao", 1L, 3, 5));

		List<Questao> sorteadas = sorteio(semente).sortear(Map.of("legislacao", 4), candidatas);

		assertThat(sorteadas).extracting(Questao::getCodigo)
			.filteredOn(codigo -> codigo.startsWith("NOVA"))
			.hasSize(2);
		assertThat(sorteadas).extracting(Questao::getCodigo).noneMatch(codigo -> codigo.startsWith("MUITO"));
	}

	@ParameterizedTest
	@MethodSource("sementes")
	void entreQuestoesIgualmenteVistasEspalhaOsSubtemas(int semente) {
		List<Candidata> candidatas = new ArrayList<>();
		candidatas.addAll(varias("SUB1", "legislacao", 1L, 6, 0));
		candidatas.addAll(varias("SUB2", "legislacao", 2L, 1, 0));
		candidatas.addAll(varias("SUB3", "legislacao", 3L, 1, 0));

		List<Questao> sorteadas = sorteio(semente).sortear(Map.of("legislacao", 3), candidatas);

		assertThat(sorteadas).extracting(q -> q.getCodigo().substring(0, 4))
			.containsExactlyInAnyOrder("SUB1", "SUB2", "SUB3");
	}

	@ParameterizedTest
	@MethodSource("sementes")
	void questaoIneditaVemAntesDeCobrirOutroSubtema(int semente) {
		List<Candidata> candidatas = new ArrayList<>();
		candidatas.addAll(varias("NOVA", "legislacao", 1L, 2, 0));
		candidatas.addAll(varias("VISTA", "legislacao", 2L, 2, 3));

		List<Questao> sorteadas = sorteio(semente).sortear(Map.of("legislacao", 2), candidatas);

		assertThat(sorteadas).extracting(Questao::getCodigo).allMatch(codigo -> codigo.startsWith("NOVA"));
	}

	@Test
	void listaTodasAsDisciplinasSemQuestoesSuficientes() {
		Map<String, Integer> pedido = new LinkedHashMap<>();
		pedido.put("legislacao", 2);
		pedido.put("direcao-defensiva", 1);

		assertThatThrownBy(() -> sorteio(0).sortear(pedido, varias("LEG", "legislacao", 1L, 1, 0)))
			.isInstanceOfSatisfying(QuestoesInsuficientesException.class, e -> assertThat(e.getFaltas())
				.containsExactly("legislacao: precisa de 2, há 1", "direcao-defensiva: precisa de 1, há 0"));
	}

	@Test
	void embaralhaAsDisciplinasNaOrdemFinal() {
		List<Candidata> candidatas = new ArrayList<>();
		candidatas.addAll(varias("LEG", "legislacao", 1L, 5, 0));
		candidatas.addAll(varias("DDE", "direcao-defensiva", 2L, 5, 0));
		Map<String, Integer> pedido = new LinkedHashMap<>();
		pedido.put("legislacao", 5);
		pedido.put("direcao-defensiva", 5);

		boolean algumaComecaPorDirecaoDefensiva = sementes().anyMatch(
				semente -> sorteio(semente).sortear(pedido, candidatas).getFirst().getCodigo().startsWith("DDE"));

		assertThat(algumaComecaPorDirecaoDefensiva).isTrue();
	}

	private static SorteioQuestoes sorteio(int semente) {
		return new SorteioQuestoes(new Random(semente));
	}

	private static List<Candidata> varias(String prefixo, String disciplina, Long subtemaId, int quantidade,
			long aparicoes) {
		return IntStream.rangeClosed(1, quantidade)
			.mapToObj(i -> new Candidata(questao(prefixo + "-" + i), disciplina, subtemaId, aparicoes))
			.toList();
	}

	private static Questao questao(String codigo) {
		ConteudoQuestao conteudo = new ConteudoQuestao("Enunciado " + codigo, null,
				List.of(new Opcao("Certa", true), new Opcao("Errada", false)), "Explicação", null, null);
		return new Questao(null, codigo, 1, OrigemQuestao.IA, conteudo);
	}

}
