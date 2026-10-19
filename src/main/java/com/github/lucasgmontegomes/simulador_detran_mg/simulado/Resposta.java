package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;
import java.time.Instant;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Uma questão dentro de um simulado. Criada em branco quando o simulado começa e preenchida conforme você responde.
 */
@Entity
public class Resposta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "simulado_id")
	private Simulado simulado;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "questao_id")
	private Questao questao;

	@Column(nullable = false)
	private int ordem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alternativa_id")
	private Alternativa alternativa;

	private Boolean correta;

	@Column(nullable = false)
	private long tempoGastoMs;

	private Instant respondidaEm;

	protected Resposta() {
	}

	Resposta(Simulado simulado, Questao questao, int ordem) {
		this.simulado = simulado;
		this.questao = questao;
		this.ordem = ordem;
	}

	void registrar(Alternativa alternativa, Duration tempoGasto, Instant agora) {
		if (alternativa != null && !pertenceAQuestao(alternativa)) {
			throw new IllegalArgumentException("A alternativa não pertence à questão " + ordem);
		}
		this.alternativa = alternativa;
		// Soma porque dá para voltar à mesma questão várias vezes durante a prova.
		this.tempoGastoMs += tempoGasto.toMillis();
		this.respondidaEm = agora;
	}

	/** Congela o gabarito desta resposta. Questão em branco conta como erro. */
	boolean corrigir() {
		this.correta = alternativa != null && alternativa.isCorreta();
		return correta;
	}

	private boolean pertenceAQuestao(Alternativa alternativa) {
		Questao dona = alternativa.getQuestao();
		return dona == questao || (dona.getId() != null && dona.getId().equals(questao.getId()));
	}

	public Long getId() {
		return id;
	}

	public Simulado getSimulado() {
		return simulado;
	}

	public Questao getQuestao() {
		return questao;
	}

	public int getOrdem() {
		return ordem;
	}

	public Alternativa getAlternativa() {
		return alternativa;
	}

	public boolean isEmBranco() {
		return alternativa == null;
	}

	public Boolean getCorreta() {
		return correta;
	}

	public Duration getTempoGasto() {
		return Duration.ofMillis(tempoGastoMs);
	}

	public Instant getRespondidaEm() {
		return respondidaEm;
	}

}
