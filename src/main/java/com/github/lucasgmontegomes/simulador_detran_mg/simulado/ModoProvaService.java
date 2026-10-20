package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Alternativa;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Questao;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.QuestaoRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.StatusQuestao;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SorteioQuestoes.Candidata;

/**
 * Ciclo de vida do Modo Prova: monta a prova na distribuição oficial, recebe as respostas dentro do prazo e
 * encerra com a nota. O horário vem sempre do servidor (do {@link Clock}), nunca do navegador.
 */
@Service
@Transactional
public class ModoProvaService {

	private final QuestaoRepository questoes;

	private final SimuladoRepository simulados;

	private final RegrasProva regras;

	private final Clock relogio;

	// java.util.Random é seguro para uso concorrente, ao contrário de RandomGenerator.getDefault().
	private final SorteioQuestoes sorteio = new SorteioQuestoes(new Random());

	public ModoProvaService(QuestaoRepository questoes, SimuladoRepository simulados, RegrasProva regras,
			Clock relogio) {
		this.questoes = questoes;
		this.simulados = simulados;
		this.regras = regras;
		this.relogio = relogio;
	}

	/**
	 * Sorteia as questões revisadas e inicia o cronômetro. Uma prova antiga que ficou aberta depois do prazo é
	 * encerrada antes, com o resultado que tinha.
	 *
	 * @throws ProvaEmAndamentoException se houver uma prova aberta e ainda dentro do prazo
	 * @throws QuestoesInsuficientesException se faltarem questões revisadas em alguma disciplina
	 */
	public Simulado iniciar() {
		Instant agora = relogio.instant();
		encerrarProvaVencida(agora);

		Map<Long, Long> aparicoes = simulados.contarAparicoesPorQuestao()
			.stream()
			.collect(Collectors.toMap(AparicoesQuestao::questaoId, AparicoesQuestao::vezes));
		List<Candidata> candidatas = questoes
			.findSorteaveis(StatusQuestao.REVISADA, regras.questoesPorDisciplina().keySet())
			.stream()
			.map(questao -> new Candidata(questao, questao.getSubtema().getDisciplina().getCodigo(),
					questao.getSubtema().getId(), aparicoes.getOrDefault(questao.getId(), 0L)))
			.toList();

		List<Questao> sorteadas = sorteio.sortear(regras.questoesPorDisciplina(), candidatas);
		return simulados
			.save(new Simulado(ModoSimulado.PROVA, sorteadas, regras.notaCorte(), regras.duracao(), agora));
	}

	/**
	 * Registra (ou troca) a resposta de uma questão. {@code alternativaId} nulo deixa a questão em branco.
	 *
	 * @throws TempoEsgotadoException se o prazo já passou; nesse caso a prova é encerrada e o resultado gravado
	 */
	// Sem o noRollbackFor, a exceção desfaria o encerramento que acabou de ser feito.
	@Transactional(noRollbackFor = TempoEsgotadoException.class)
	public void responder(long simuladoId, int ordem, Long alternativaId, Duration tempoGasto) {
		Simulado simulado = buscar(simuladoId);
		Instant agora = relogio.instant();
		if (!simulado.isFinalizado() && simulado.isExpirado(agora)) {
			simulado.finalizar(agora);
			throw new TempoEsgotadoException(simuladoId);
		}
		Alternativa alternativa = alternativaId == null ? null : alternativa(simulado.questaoNaOrdem(ordem), alternativaId);
		simulado.responder(ordem, alternativa, tempoGasto, agora);
	}

	/**
	 * Encerra a prova e devolve a nota. Se ela já tiver sido encerrada (por exemplo, porque o tempo acabou),
	 * apenas devolve o resultado: o cronômetro zerando e o clique em "Encerrar" podem chegar juntos.
	 */
	public ResultadoProva finalizar(long simuladoId) {
		Simulado simulado = buscar(simuladoId);
		if (!simulado.isFinalizado()) {
			simulado.finalizar(relogio.instant());
		}
		return ResultadoProva.de(simulado);
	}

	/**
	 * Encerra a prova se o prazo já passou. Usado quando a tela é aberta depois do fim do tempo (aba esquecida,
	 * computador que hibernou), para mandar direto ao resultado.
	 *
	 * @return {@code true} se a prova está encerrada (agora ou antes)
	 */
	public boolean encerrarSeVencida(long simuladoId) {
		Simulado simulado = buscar(simuladoId);
		Instant agora = relogio.instant();
		if (!simulado.isFinalizado() && simulado.isExpirado(agora)) {
			simulado.finalizar(agora);
		}
		return simulado.isFinalizado();
	}

	private void encerrarProvaVencida(Instant agora) {
		Optional<Simulado> aberta = simulados.findFirstByModoAndFinalizadoEmIsNullOrderByIniciadoEmDesc(ModoSimulado.PROVA);
		if (aberta.isEmpty()) {
			return;
		}
		Simulado prova = aberta.get();
		if (!prova.isExpirado(agora)) {
			throw new ProvaEmAndamentoException(prova.getId());
		}
		prova.finalizar(agora);
		// O índice de "um simulado em andamento por modo" é checado a cada comando: o UPDATE que encerra a prova
		// antiga precisa chegar ao banco antes do INSERT da nova.
		simulados.flush();
	}

	private Simulado buscar(long simuladoId) {
		return simulados.findById(simuladoId).orElseThrow(() -> new SimuladoNaoEncontradoException(simuladoId));
	}

	private static Alternativa alternativa(Questao questao, long alternativaId) {
		return questao.getAlternativas()
			.stream()
			.filter(alternativa -> alternativa.getId().equals(alternativaId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException(
					"A alternativa " + alternativaId + " não pertence à questão " + questao.getCodigo()));
	}

}
