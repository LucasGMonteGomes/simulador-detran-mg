package com.github.lucasgmontegomes.simulador_detran_mg.web;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.github.lucasgmontegomes.simulador_detran_mg.simulado.SimuladoNaoEncontradoException;

/**
 * Erros esperados viram uma página amigável (template {@code error}) em vez da página padrão do Spring.
 */
@ControllerAdvice
class TratamentoDeErros {

	@ExceptionHandler(SimuladoNaoEncontradoException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	String simuladoNaoEncontrado(SimuladoNaoEncontradoException e, Model model) {
		return pagina(model, HttpStatus.NOT_FOUND, "Prova não encontrada", e.getMessage());
	}

	/** Questão fora da prova, alternativa de outra questão e afins: pedido que a própria tela não geraria. */
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	String pedidoInvalido(IllegalArgumentException e, Model model) {
		return pagina(model, HttpStatus.BAD_REQUEST, "Pedido inválido", e.getMessage());
	}

	private static String pagina(Model model, HttpStatus status, String titulo, String mensagem) {
		model.addAttribute("status", status.value());
		model.addAttribute("titulo", titulo);
		model.addAttribute("mensagem", mensagem);
		return "error";
	}

}
