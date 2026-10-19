package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

/**
 * Já existe uma prova aberta e dentro do prazo. Quem chama pode oferecer a opção de retomá-la.
 */
public class ProvaEmAndamentoException extends RuntimeException {

	private final long simuladoId;

	public ProvaEmAndamentoException(long simuladoId) {
		super("Já existe uma prova em andamento (simulado " + simuladoId + ")");
		this.simuladoId = simuladoId;
	}

	public long getSimuladoId() {
		return simuladoId;
	}

}
