package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Assunto dentro de uma disciplina (ex.: placas de advertência). É a unidade usada na análise de desempenho.
 */
@Entity
public class Subtema {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "disciplina_id")
	private Disciplina disciplina;

	@Column(nullable = false, unique = true, length = 80)
	private String codigo;

	@Column(nullable = false, length = 150)
	private String nome;

	protected Subtema() {
	}

	public Long getId() {
		return id;
	}

	public Disciplina getDisciplina() {
		return disciplina;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNome() {
		return nome;
	}

}
