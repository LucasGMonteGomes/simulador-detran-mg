package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Raiz do agregado: toda alteração em respostas passa por aqui, para que as regras de prazo e finalização
 * fiquem num lugar só.
 */
@Entity
public class Simulado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ModoSimulado modo;

	@Column(nullable = false)
	private int totalQuestoes;

	@Column(nullable = false)
	private int notaCorte;

	@Column(nullable = false, updatable = false)
	private Instant iniciadoEm;

	private Instant expiraEm;

	private Instant finalizadoEm;

	private Integer acertos;

	private Boolean aprovado;

	@OneToMany(mappedBy = "simulado", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<Resposta> respostas = new ArrayList<>();

	protected Simulado() {
	}

	/**
	 * @param duracao tempo de prova; {@code null} para simulado sem cronômetro
	 */
	public Simulado(ModoSimulado modo, List<Questao> questoes, int notaCorte, Duration duracao, Instant iniciadoEm) {
		if (questoes.isEmpty()) {
			throw new IllegalArgumentException("O simulado precisa de pelo menos uma questão");
		}
		if (notaCorte < 0 || notaCorte > questoes.size()) {
			throw new IllegalArgumentException("Nota de corte fora do intervalo: " + notaCorte);
		}
		this.modo = modo;
		this.totalQuestoes = questoes.size();
		this.notaCorte = notaCorte;
		this.iniciadoEm = iniciadoEm;
		this.expiraEm = duracao == null ? null : iniciadoEm.plus(duracao);
		for (int i = 0; i < questoes.size(); i++) {
			respostas.add(new Resposta(this, questoes.get(i), i + 1));
		}
	}

	public void responder(int ordem, Alternativa alternativa, Duration tempoGasto, Instant agora) {
		if (isFinalizado()) {
			throw new IllegalStateException("O simulado já foi finalizado");
		}
		if (isExpirado(agora)) {
			throw new IllegalStateException("O tempo do simulado esgotou");
		}
		respostaNaOrdem(ordem).registrar(alternativa, tempoGasto, agora);
	}

	/**
	 * Corrige todas as respostas, calcula acertos e aprovação e encerra o simulado. Questões em branco contam como
	 * erro. Se o pedido chegar depois do prazo, o simulado é encerrado no horário em que o tempo acabou.
	 *
	 * @throws IllegalStateException se o simulado já foi finalizado
	 */
	public void finalizar(Instant agora) {
		if (isFinalizado()) {
			throw new IllegalStateException("O simulado já foi finalizado");
		}
		int corretas = 0;
		for (Resposta resposta : respostas) {
			if (resposta.corrigir()) {
				corretas++;
			}
		}
		this.acertos = corretas;
		this.aprovado = corretas >= notaCorte;
		this.finalizadoEm = isExpirado(agora) ? expiraEm : agora;
	}

	public boolean isFinalizado() {
		return finalizadoEm != null;
	}

	public boolean isExpirado(Instant agora) {
		return expiraEm != null && !agora.isBefore(expiraEm);
	}

	private Resposta respostaNaOrdem(int ordem) {
		if (ordem < 1 || ordem > respostas.size()) {
			throw new IllegalArgumentException("Questão inexistente no simulado: " + ordem);
		}
		return respostas.get(ordem - 1);
	}

	public Long getId() {
		return id;
	}

	public ModoSimulado getModo() {
		return modo;
	}

	public int getTotalQuestoes() {
		return totalQuestoes;
	}

	public int getNotaCorte() {
		return notaCorte;
	}

	public Instant getIniciadoEm() {
		return iniciadoEm;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public Instant getFinalizadoEm() {
		return finalizadoEm;
	}

	public Integer getAcertos() {
		return acertos;
	}

	public Boolean getAprovado() {
		return aprovado;
	}

	public List<Resposta> getRespostas() {
		return Collections.unmodifiableList(respostas);
	}

}
