package com.github.lucasgmontegomes.simulador_detran_mg.simulado;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SimuladoRepository extends JpaRepository<Simulado, Long> {

	Optional<Simulado> findFirstByModoAndFinalizadoEmIsNullOrderByIniciadoEmDesc(ModoSimulado modo);

	@Query("""
			select new com.github.lucasgmontegomes.simulador_detran_mg.simulado.AparicoesQuestao(r.questao.id, count(r))
			from Resposta r
			group by r.questao.id
			""")
	List<AparicoesQuestao> contarAparicoesPorQuestao();

}
