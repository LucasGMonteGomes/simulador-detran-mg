package com.github.lucasgmontegomes.simulador_detran_mg.conteudo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubtemaRepository extends JpaRepository<Subtema, Long> {

	Optional<Subtema> findByCodigo(String codigo);

}
