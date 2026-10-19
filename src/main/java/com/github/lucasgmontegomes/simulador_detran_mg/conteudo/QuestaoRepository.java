package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestaoRepository extends JpaRepository<Questao, Long> {

	Optional<Questao> findByCodigoAndAtivaTrue(String codigo);

	@Query("select coalesce(max(q.versao), 0) from Questao q where q.codigo = :codigo")
	int findMaiorVersao(String codigo);

	/** Questões ativas com o status pedido, já com subtema e disciplina carregados (uma consulta só). */
	@Query("""
			select q from Questao q
			join fetch q.subtema s
			join fetch s.disciplina d
			where q.ativa = true and q.status = :status and d.codigo in :disciplinas
			""")
	List<Questao> findSorteaveis(StatusQuestao status, Collection<String> disciplinas);

}
