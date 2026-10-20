package com.github.lucasgmontegomes.simulador_detran_mg.web;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.Disciplina;
import com.github.lucasgmontegomes.simulador_detran_mg.conteudo.DisciplinaRepository;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.ConsultaProva;
import com.github.lucasgmontegomes.simulador_detran_mg.simulado.RegrasProva;

@Controller
class InicioController {

	record ItemDistribuicao(String disciplina, int questoes) {
	}

	private final ConsultaProva consulta;

	private final RegrasProva regras;

	private final DisciplinaRepository disciplinas;

	InicioController(ConsultaProva consulta, RegrasProva regras, DisciplinaRepository disciplinas) {
		this.consulta = consulta;
		this.regras = regras;
		this.disciplinas = disciplinas;
	}

	@GetMapping("/")
	String inicio(Model model) {
		model.addAttribute("regras", regras);
		model.addAttribute("distribuicao", distribuicao());
		model.addAttribute("provaEmAndamento", consulta.provaEmAndamento().orElse(null));
		return "inicio";
	}

	private List<ItemDistribuicao> distribuicao() {
		Map<String, String> nomePorCodigo = disciplinas.findAll()
			.stream()
			.collect(Collectors.toMap(Disciplina::getCodigo, Disciplina::getNome));
		return regras.questoesPorDisciplina()
			.entrySet()
			.stream()
			.map(item -> new ItemDistribuicao(nomePorCodigo.getOrDefault(item.getKey(), item.getKey()), item.getValue()))
			.toList();
	}

}
