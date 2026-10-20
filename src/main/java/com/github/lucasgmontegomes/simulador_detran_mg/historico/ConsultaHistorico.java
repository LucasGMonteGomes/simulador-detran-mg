package com.github.lucasgmontegomes.simulador_detran_mg.historico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.lucasgmontegomes.simulador_detran_mg.historico.DesempenhoSubtema.Periodo;

/**
 * Relatórios do histórico em SQL direto: agregações e window functions ficam mais claras (e rápidas) no banco do
 * que montando entidades JPA. Só considera o Modo Prova: o Modo Treino tem questões escolhidas a dedo e
 * distorceria a evolução.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaHistorico {

	/** Quantas provas mais recentes formam o período "recente" na comparação por subtema. */
	public static final int PROVAS_RECENTES = 3;

	private static final String PROVAS = """
			select s.id,
			       row_number() over (order by s.finalizado_em, s.id) as numero,
			       s.iniciado_em,
			       s.finalizado_em,
			       s.acertos,
			       count(*) filter (where r.alternativa_id is null) as em_branco,
			       s.total_questoes,
			       s.nota_corte,
			       s.aprovado
			from simulado s
			join resposta r on r.simulado_id = s.id
			where s.modo = 'PROVA' and s.finalizado_em is not null
			group by s.id
			order by s.finalizado_em, s.id
			""";

	private static final String POR_SUBTEMA = """
			with provas as (
			    select id, row_number() over (order by finalizado_em desc, id desc) as posicao
			    from simulado
			    where modo = 'PROVA' and finalizado_em is not null
			)
			select st.nome as subtema,
			       d.nome as disciplina,
			       count(*) filter (where r.correta) as acertos,
			       count(*) as total,
			       count(*) filter (where r.correta and p.posicao <= :recentes) as acertos_recentes,
			       count(*) filter (where p.posicao <= :recentes) as total_recentes,
			       count(*) filter (where r.correta and p.posicao > :recentes) as acertos_anteriores,
			       count(*) filter (where p.posicao > :recentes) as total_anteriores
			from resposta r
			join provas p on p.id = r.simulado_id
			join questao q on q.id = r.questao_id
			join subtema st on st.id = q.subtema_id
			join disciplina d on d.id = st.disciplina_id
			group by st.id, st.nome, d.nome
			""";

	private final JdbcClient jdbc;

	public ConsultaHistorico(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Provas encerradas, da mais antiga para a mais recente. */
	public List<ProvaRealizada> provas() {
		return jdbc.sql(PROVAS).query(ConsultaHistorico::prova).list();
	}

	public ResumoHistorico resumo() {
		return new ResumoHistorico(provas());
	}

	/** Um item por subtema que já caiu em alguma prova, dos pontos mais fracos para os mais fortes. */
	public List<DesempenhoSubtema> desempenhoPorSubtema() {
		return jdbc.sql(POR_SUBTEMA)
			.param("recentes", PROVAS_RECENTES)
			.query(ConsultaHistorico::desempenho)
			.list()
			.stream()
			.sorted(Comparator.comparingDouble(DesempenhoSubtema::taxaSuavizada)
				.thenComparing(DesempenhoSubtema::subtema))
			.toList();
	}

	private static ProvaRealizada prova(ResultSet rs, int linha) throws SQLException {
		return new ProvaRealizada(rs.getInt("numero"), rs.getLong("id"), instante(rs, "iniciado_em"),
				instante(rs, "finalizado_em"), rs.getInt("acertos"), rs.getInt("em_branco"),
				rs.getInt("total_questoes"), rs.getInt("nota_corte"), rs.getBoolean("aprovado"));
	}

	private static DesempenhoSubtema desempenho(ResultSet rs, int linha) throws SQLException {
		return new DesempenhoSubtema(rs.getString("subtema"), rs.getString("disciplina"),
				new Periodo(rs.getInt("acertos"), rs.getInt("total")),
				new Periodo(rs.getInt("acertos_recentes"), rs.getInt("total_recentes")),
				new Periodo(rs.getInt("acertos_anteriores"), rs.getInt("total_anteriores")));
	}

	/** O driver do PostgreSQL entrega {@code timestamptz} como {@link OffsetDateTime}. */
	private static Instant instante(ResultSet rs, String coluna) throws SQLException {
		return rs.getObject(coluna, OffsetDateTime.class).toInstant();
	}

}
