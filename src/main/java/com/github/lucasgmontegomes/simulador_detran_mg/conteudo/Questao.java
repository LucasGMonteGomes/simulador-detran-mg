package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	private String trechoFonte;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private OrigemQuestao origem;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private StatusQuestao status;

	@Column(nullable = false)
	private boolean ativa;

	@Column(nullable = false, updatable = false)
	private Instant criadaEm;

	@OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("ordem")
	private List<Alternativa> alternativas = new ArrayList<>();

	protected Questao() {
	}

	/**
	 * Cria uma questão em rascunho. As alternativas recebem a ordem em que aparecem no conteúdo.
	 */
	public Questao(Subtema subtema, String codigo, int versao, OrigemQuestao origem, ConteudoQuestao conteudo) {
		this.subtema = subtema;
		this.codigo = codigo;
		this.versao = versao;
		this.origem = origem;
		this.status = StatusQuestao.RASCUNHO;
		this.enunciado = conteudo.enunciado();
		this.imagem = conteudo.imagem();
		this.explicacao = conteudo.explicacao();
		this.referenciaLegal = conteudo.referenciaLegal();
		this.trechoFonte = conteudo.trechoFonte();
		int ordem = 1;
		for (ConteudoQuestao.Opcao opcao : conteudo.alternativas()) {
			alternativas.add(new Alternativa(this, ordem++, opcao.texto(), opcao.correta()));
		}
		this.ativa = true;
		this.criadaEm = Instant.now();
	}

	public ConteudoQuestao conteudo() {
		List<ConteudoQuestao.Opcao> opcoes = alternativas.stream()
			.map(alternativa -> new ConteudoQuestao.Opcao(alternativa.getTexto(), alternativa.isCorreta()))
			.toList();
		return new ConteudoQuestao(enunciado, imagem, opcoes, explicacao, referenciaLegal, trechoFonte);
	}

	public boolean temConteudo(Subtema outroSubtema, ConteudoQuestao outroConteudo) {
		return subtema.getId().equals(outroSubtema.getId()) && conteudo().equals(outroConteudo);
	}

	/**
	 * Aplica origem e revisão vindas do arquivo de questões. Uma questão reportada continua reportada até ganhar
	 * uma nova versão com o conteúdo corrigido.
	 */
	public void atualizarRevisao(OrigemQuestao origem, boolean revisada) {
		this.origem = origem;
		if (status != StatusQuestao.REPORTADA) {
			this.status = revisada ? StatusQuestao.REVISADA : StatusQuestao.RASCUNHO;
		}
	}

	public void reportar() {
		this.status = StatusQuestao.REPORTADA;
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

	public String getTrechoFonte() {
		return trechoFonte;
	}

	public OrigemQuestao getOrigem() {
		return origem;
	}

	public StatusQuestao getStatus() {
		return status;
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
