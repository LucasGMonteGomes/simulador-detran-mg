package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

/**
 * A resposta chegou depois do prazo. Quando esta exceção é lançada, a prova já foi encerrada e o resultado já está
 * gravado.
 */
public class TempoEsgotadoException extends RuntimeException {

	private final long simuladoId;

	public TempoEsgotadoException(long simuladoId) {
		super("O tempo da prova esgotou (simulado " + simuladoId + ")");
		this.simuladoId = simuladoId;
	}

	public long getSimuladoId() {
		return simuladoId;
	}

}
