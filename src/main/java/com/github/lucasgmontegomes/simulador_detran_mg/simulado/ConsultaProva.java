package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Disciplina;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.CorrecaoProva.DesempenhoDisciplina;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.CorrecaoProva.ItemCorrigido;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.CorrecaoProva.OpcaoCorrigida;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.CorrecaoProva.Situacao;

/**
 * Leituras para as telas da prova. Nada aqui altera o simulado: mudanças passam pelo {@link ModoProvaService}.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaProva {

	private final SimuladoRepository simulados;

	private final Clock relogio;

	public ConsultaProva(SimuladoRepository simulados, Clock relogio) {
		this.simulados = simulados;
		this.relogio = relogio;
	}

	/** A prova aberta e ainda dentro do prazo, se houver. */
	public Optional<Long> provaEmAndamento() {
		Instant agora = relogio.instant();
		return simulados.findFirstByModoAndFinalizadoEmIsNullOrderByIniciadoEmDesc(ModoSimulado.PROVA)
			.filter(prova -> !prova.isExpirado(agora))
			.map(Simulado::getId);
	}

	public boolean encerrada(long simuladoId) {
		return buscar(simuladoId).isFinalizado();
	}

	public QuestaoEmProva questao(long simuladoId, int ordem) {
		Simulado simulado = buscar(simuladoId);
		Resposta resposta = simulado.respostaNaOrdem(ordem);
		Questao questao = resposta.getQuestao();
		List<QuestaoEmProva.Opcao> opcoes = questao.getAlternativas()
			.stream()
			.map(alternativa -> new QuestaoEmProva.Opcao(alternativa.getId(), letra(alternativa),
					alternativa.getTexto()))
			.toList();
		Set<Integer> respondidas = simulado.getRespostas()
			.stream()
			.filter(r -> !r.isEmBranco())
			.map(Resposta::getOrdem)
			.collect(Collectors.toSet());
		Long marcada = resposta.isEmBranco() ? null : resposta.getAlternativa().getId();
		return new QuestaoEmProva(simuladoId, ordem, simulado.getTotalQuestoes(), questao.getEnunciado(),
				questao.getImagem(), opcoes, marcada, respondidas, tempoRestante(simulado));
	}

	/** Vazio enquanto a prova não for encerrada: o gabarito só aparece depois. */
	public Optional<CorrecaoProva> correcao(long simuladoId) {
		Simulado simulado = buscar(simuladoId);
		if (!simulado.isFinalizado()) {
			return Optional.empty();
		}
		List<ItemCorrigido> itens = simulado.getRespostas().stream().map(ConsultaProva::corrigir).toList();
		return Optional.of(new CorrecaoProva(ResultadoProva.de(simulado), porDisciplina(simulado), itens));
	}

	private Duration tempoRestante(Simulado simulado) {
		if (simulado.getExpiraEm() == null) {
			return Duration.ZERO;
		}
		Duration restante = Duration.between(relogio.instant(), simulado.getExpiraEm());
		return restante.isNegative() ? Duration.ZERO : restante;
	}

	private static List<DesempenhoDisciplina> porDisciplina(Simulado simulado) {
		// TreeMap pelo id: mesma ordem das disciplinas no curso, não a ordem em que apareceram na prova.
		Map<Long, List<Resposta>> agrupadas = simulado.getRespostas()
			.stream()
			.collect(Collectors.groupingBy(r -> disciplina(r).getId(), TreeMap::new, Collectors.toList()));
		List<DesempenhoDisciplina> desempenho = new ArrayList<>();
		for (List<Resposta> respostas : agrupadas.values()) {
			int acertos = (int) respostas.stream().filter(r -> Boolean.TRUE.equals(r.getCorreta())).count();
			desempenho.add(new DesempenhoDisciplina(disciplina(respostas.getFirst()).getNome(), acertos,
					respostas.size()));
		}
		return desempenho;
	}

	private static Disciplina disciplina(Resposta resposta) {
		return resposta.getQuestao().getSubtema().getDisciplina();
	}

	private static ItemCorrigido corrigir(Resposta resposta) {
		Questao questao = resposta.getQuestao();
		Long marcadaId = resposta.isEmBranco() ? null : resposta.getAlternativa().getId();
		List<OpcaoCorrigida> opcoes = questao.getAlternativas()
			.stream()
			.map(alternativa -> new OpcaoCorrigida(letra(alternativa), alternativa.getTexto(),
					alternativa.isCorreta(), alternativa.getId().equals(marcadaId)))
			.toList();
		Situacao situacao = resposta.isEmBranco() ? Situacao.EM_BRANCO
				: Boolean.TRUE.equals(resposta.getCorreta()) ? Situacao.ACERTOU : Situacao.ERROU;
		return new ItemCorrigido(resposta.getOrdem(), questao.getSubtema().getDisciplina().getNome(),
				questao.getSubtema().getNome(), questao.getEnunciado(), questao.getImagem(), opcoes, situacao,
				questao.getExplicacao(), questao.getReferenciaLegal(), resposta.getTempoGasto());
	}

	private static String letra(Alternativa alternativa) {
		return String.valueOf((char) ('A' + alternativa.getOrdem() - 1));
	}

	private Simulado buscar(long simuladoId) {
		return simulados.findById(simuladoId).orElseThrow(() -> new SimuladoNaoEncontradoException(simuladoId));
	}

}
