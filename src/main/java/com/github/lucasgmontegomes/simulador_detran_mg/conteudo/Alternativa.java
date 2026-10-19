package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Alternativa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "questao_id")
	private Questao questao;

	@Column(nullable = false)
	private int ordem;

	@Column(nullable = false)
	private String texto;

	@Column(nullable = false)
	private boolean correta;

	protected Alternativa() {
	}

	Alternativa(Questao questao, int ordem, String texto, boolean correta) {
		this.questao = questao;
		this.ordem = ordem;
		this.texto = texto;
		this.correta = correta;
	}

	public Long getId() {
		return id;
	}

	public Questao getQuestao() {
		return questao;
	}

	public int getOrdem() {
		return ordem;
	}

	public String getTexto() {
		return texto;
	}

	public boolean isCorreta() {
		return correta;
	}

}
