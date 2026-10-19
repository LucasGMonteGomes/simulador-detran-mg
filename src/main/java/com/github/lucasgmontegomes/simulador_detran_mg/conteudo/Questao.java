package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Questão de múltipla escolha. Alternativas só são criadas e alteradas por aqui (raiz do agregado).
 * Uma questão já usada em simulados não deve ser editada: crie uma nova versão e desative esta.
 */
@Entity
public class Questao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subtema_id")
	private Subtema subtema;

	@Column(nullable = false, length = 30)
	private String codigo;

	@Column(nullable = false)
	private int versao;

	@Column(nullable = false)
	private String enunciado;

	private String imagem;

	@Column(nullable = false)
	private String explicacao;

	@Column(length = 150)
	private String referenciaLegal;

	@Column(nullable = false)
	private boolean ativa;

	@Column(nullable = false, updatable = false)
	private Instant criadaEm;

	@OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<Alternativa> alternativas = new ArrayList<>();

	protected Questao() {
	}

	public Questao(Subtema subtema, String codigo, int versao, String enunciado, String imagem, String explicacao,
			String referenciaLegal) {
		this.subtema = subtema;
		this.codigo = codigo;
		this.versao = versao;
		this.enunciado = enunciado;
		this.imagem = imagem;
		this.explicacao = explicacao;
		this.referenciaLegal = referenciaLegal;
		this.ativa = true;
		this.criadaEm = Instant.now();
	}

	public Alternativa adicionarAlternativa(int ordem, String texto, boolean correta) {
		Alternativa alternativa = new Alternativa(this, ordem, texto, correta);
		alternativas.add(alternativa);
		return alternativa;
	}

	public Optional<Alternativa> alternativaCorreta() {
		return alternativas.stream().filter(Alternativa::isCorreta).findFirst();
	}

	public void desativar() {
		this.ativa = false;
	}

	public Long getId() {
		return id;
	}

	public Subtema getSubtema() {
		return subtema;
	}

	public String getCodigo() {
		return codigo;
	}

	public int getVersao() {
		return versao;
	}

	public String getEnunciado() {
		return enunciado;
	}

	public String getImagem() {
		return imagem;
	}

	public String getExplicacao() {
		return explicacao;
	}

	public String getReferenciaLegal() {
		return referenciaLegal;
	}

	public boolean isAtiva() {
		return ativa;
	}

	public Instant getCriadaEm() {
		return criadaEm;
	}

	public List<Alternativa> getAlternativas() {
		return Collections.unmodifiableList(alternativas);
	}

}
