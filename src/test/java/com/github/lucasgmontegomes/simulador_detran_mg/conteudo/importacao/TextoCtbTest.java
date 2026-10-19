package com.github.lucasgmontegomes.simulador_detran_mg.conteudo.importacao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TextoCtbTest {

	private final TextoCtb texto = new TextoCtb("""
			Art. 61. A velocidade máxima permitida para a via será indicada por meio de sinalização.
			c) quarenta quilômetros por hora, nas vias coletoras;
			Art. 70. Os pedestres que estiverem atravessando a via sobre as faixas delimitadas para esse fim terão \
			prioridade de passagem
			""");

	@Test
	void encontraTrechoIgnorandoEspacosQuebrasDeLinhaEMaiusculas() {
		assertThat(texto.contem("Quarenta  quilômetros por hora,\n nas vias coletoras")).isTrue();
	}

	@Test
	void encontraTrechoQueAtravessaParagrafos() {
		assertThat(texto.contem("por meio de sinalização. c) quarenta")).isTrue();
	}

	@Test
	void naoEncontraTrechoAlterado() {
		assertThat(texto.contem("quarenta quilômetros por hora, nas vias locais")).isFalse();
	}

	@Test
	void trechoVazioNuncaConta() {
		assertThat(texto.contem("   ")).isFalse();
	}

	@Test
	void textoDoProjetoTemARegraDePontuacaoVigente() {
		TextoCtb ctb = TextoCtb.carregar(new ClassPathResource("fontes/ctb.txt"));

		assertThat(ctb.contem("30 (trinta) pontos, caso conste 1 (uma) infração gravíssima na pontuação")).isTrue();
	}

}
