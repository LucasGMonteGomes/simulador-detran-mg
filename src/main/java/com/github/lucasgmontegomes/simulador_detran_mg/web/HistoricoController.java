package com.github.lucasgmontegomes.simulador_detran_mg.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.github.lucasgmontegomes.simulador_detran_mg.historico.ConsultaHistorico;
import com.github.lucasgmontegomes.simulador_detran_mg.historico.ResumoHistorico;

@Controller
class HistoricoController {

	private final ConsultaHistorico consulta;

	HistoricoController(ConsultaHistorico consulta) {
		this.consulta = consulta;
	}

	@GetMapping("/historico")
	String historico(Model model) {
		ResumoHistorico resumo = consulta.resumo();
		model.addAttribute("resumo", resumo);
		// Na tabela, a prova mais recente vem primeiro; o gráfico reordena pelo número.
		model.addAttribute("provasMaisRecentesPrimeiro", resumo.provas().reversed());
		model.addAttribute("subtemas", resumo.quantidade() == 0 ? List.of() : consulta.desempenhoPorSubtema());
		model.addAttribute("provasRecentes", ConsultaHistorico.PROVAS_RECENTES);
		return "historico";
	}

}
