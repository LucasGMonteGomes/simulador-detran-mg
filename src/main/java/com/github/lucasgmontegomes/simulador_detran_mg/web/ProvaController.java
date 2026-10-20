package com.github.lucasgmontegomes.simulador_detran_mg.web;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ConsultaProva;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.CorrecaoProva;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ModoProvaService;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ProvaEmAndamentoException;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.QuestoesInsuficientesException;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.RegrasProva;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.TempoEsgotadoException;

/**
 * Telas do Modo Prova. Toda ação que muda algo é um POST seguido de redirect (Post/Redirect/Get): recarregar a
 * página nunca reenvia uma resposta nem encerra a prova sem querer.
 */
@Controller
@RequestMapping("/prova")
class ProvaController {

	static final String ENCERRAR = "encerrar";

	private final ModoProvaService modoProva;

	private final ConsultaProva consulta;

	private final RegrasProva regras;

	ProvaController(ModoProvaService modoProva, ConsultaProva consulta, RegrasProva regras) {
		this.modoProva = modoProva;
		this.consulta = consulta;
		this.regras = regras;
	}

	@PostMapping
	String iniciar(RedirectAttributes redirect) {
		try {
			return paraQuestao(modoProva.iniciar().getId(), 1);
		}
		catch (ProvaEmAndamentoException e) {
			redirect.addFlashAttribute("aviso", "Você já tinha uma prova em andamento. Continue de onde parou.");
			return paraQuestao(e.getSimuladoId(), 1);
		}
		catch (QuestoesInsuficientesException e) {
			redirect.addFlashAttribute("faltas", e.getFaltas());
			return "redirect:/";
		}
	}

	@GetMapping("/{id}")
	String prova(@PathVariable long id) {
		return paraQuestao(id, 1);
	}

	@GetMapping("/{id}/questoes/{ordem}")
	String questao(@PathVariable long id, @PathVariable int ordem, Model model) {
		if (modoProva.encerrarSeVencida(id)) {
			return paraResultado(id);
		}
		model.addAttribute("tela", consulta.questao(id, ordem));
		return "prova/questao";
	}

	/**
	 * Envio do formulário: grava a resposta da questão atual e vai para o {@code destino} (o número de outra
	 * questão ou {@value #ENCERRAR}).
	 */
	@PostMapping("/{id}/questoes/{ordem}")
	String responder(@PathVariable long id, @PathVariable int ordem, @RequestParam(required = false) Long alternativaId,
			@RequestParam(defaultValue = "0") long tempoGastoMs, @RequestParam String destino,
			RedirectAttributes redirect) {
		if (consulta.encerrada(id)) {
			return paraResultado(id);
		}
		try {
			modoProva.responder(id, ordem, alternativaId, tempoGasto(tempoGastoMs));
		}
		catch (TempoEsgotadoException e) {
			redirect.addFlashAttribute("tempoEsgotado", true);
			return paraResultado(id);
		}
		if (ENCERRAR.equals(destino)) {
			modoProva.finalizar(id);
			return paraResultado(id);
		}
		return paraQuestao(id, numero(destino).orElse(ordem));
	}

	/**
	 * Salvamento automático feito pelo JavaScript ao marcar uma alternativa: responde 204, ou 409 com o endereço do
	 * resultado se a prova já tiver acabado.
	 */
	@PostMapping("/{id}/questoes/{ordem}/resposta")
	ResponseEntity<Void> salvarResposta(@PathVariable long id, @PathVariable int ordem,
			@RequestParam(required = false) Long alternativaId, @RequestParam(defaultValue = "0") long tempoGastoMs) {
		if (consulta.encerrada(id)) {
			return provaEncerrada(id);
		}
		try {
			modoProva.responder(id, ordem, alternativaId, tempoGasto(tempoGastoMs));
			return ResponseEntity.noContent().build();
		}
		catch (TempoEsgotadoException e) {
			return provaEncerrada(id);
		}
	}

	@GetMapping("/{id}/resultado")
	String resultado(@PathVariable long id, Model model) {
		modoProva.encerrarSeVencida(id);
		Optional<CorrecaoProva> correcao = consulta.correcao(id);
		if (correcao.isEmpty()) {
			return paraQuestao(id, 1);
		}
		model.addAttribute("correcao", correcao.get());
		return "prova/resultado";
	}

	/** O tempo vem do navegador: valores negativos ou maiores que a prova inteira são descartados. */
	private Duration tempoGasto(long milissegundos) {
		return Duration.ofMillis(Math.clamp(milissegundos, 0L, regras.duracao().toMillis()));
	}

	private static Optional<Integer> numero(String texto) {
		try {
			return Optional.of(Integer.parseInt(texto));
		}
		catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private static ResponseEntity<Void> provaEncerrada(long id) {
		return ResponseEntity.status(HttpStatus.CONFLICT).location(URI.create("/prova/" + id + "/resultado")).build();
	}

	private static String paraQuestao(long id, int ordem) {
		return "redirect:/prova/" + id + "/questoes/" + ordem;
	}

	private static String paraResultado(long id) {
		return "redirect:/prova/" + id + "/resultado";
	}

}
