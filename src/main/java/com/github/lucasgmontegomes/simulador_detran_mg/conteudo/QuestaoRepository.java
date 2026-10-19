package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

	Optional<Questao> findByCodigoAndAtivaTrue(String codigo);

	@Query("select coalesce(max(q.versao), 0) from Questao q where q.codigo = :codigo")
	int findMaiorVersao(String codigo);

}
