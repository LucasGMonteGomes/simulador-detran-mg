package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

public class SimuladoNaoEncontradoException extends RuntimeException {

	public SimuladoNaoEncontradoException(long simuladoId) {
		super("Simulado " + simuladoId + " não encontrado");
	}

}
