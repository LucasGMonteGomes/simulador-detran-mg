package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

	Optional<Questao> findByCodigoAndAtivaTrue(String codigo);

}
