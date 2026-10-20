// Gráfico de evolução: lê as linhas da tabela de provas (atributos data-*) e desenha um gráfico de linha em SVG.
// A tabela continua sendo a versão completa e acessível dos dados; o gráfico é um complemento.
(() => {
	'use strict';

	const figura = document.getElementById('grafico-evolucao');
	const linhas = document.querySelectorAll('#tabela-provas tbody tr');
	if (!figura || linhas.length === 0) {
		return;
	}

	const formatoData = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

	const provas = [...linhas]
		.map((tr) => ({
			numero: Number(tr.dataset.numero),
			acertos: Number(tr.dataset.acertos),
			total: Number(tr.dataset.total),
			corte: Number(tr.dataset.corte),
			data: formatoData.format(new Date(tr.dataset.finalizadaEm)),
			aprovado: tr.dataset.aprovado === 'true',
			url: tr.dataset.url,
		}))
		.sort((a, b) => a.numero - b.numero);

	const SVG = 'http://www.w3.org/2000/svg';
	const ALTURA = 260;
	const MARGEM = { topo: 16, direita: 72, base: 34, esquerda: 34 };
	const ALVO_MINIMO = 24; // largura mínima da área de foco de cada ponto
	const ESPACO_ROTULO_X = 36; // largura reservada para cada número no eixo x

	const area = figura.querySelector('.grafico-area');
	const dica = figura.querySelector('.grafico-dica');
	figura.hidden = false;

	function elemento(nome, atributos, pai) {
		const novo = document.createElementNS(SVG, nome);
		for (const [chave, valor] of Object.entries(atributos)) {
			novo.setAttribute(chave, String(valor));
		}
		pai.appendChild(novo);
		return novo;
	}

	function texto(conteudo, atributos, pai) {
		const novo = elemento('text', atributos, pai);
		novo.textContent = conteudo;
		return novo;
	}

	// Marcas redondas no eixo y: 0, 10, 20, 30 para uma prova de 30 questões.
	function marcas(maximo) {
		const passo = maximo <= 10 ? 2 : 10;
		const valores = [];
		for (let v = 0; v <= maximo; v += passo) {
			valores.push(v);
		}
		if (valores.at(-1) !== maximo) {
			valores.push(maximo);
		}
		return valores;
	}

	function desenhar() {
		const largura = Math.max(area.clientWidth, 280);
		const larguraUtil = largura - MARGEM.esquerda - MARGEM.direita;
		const alturaUtil = ALTURA - MARGEM.topo - MARGEM.base;
		const maximo = Math.max(...provas.map((p) => p.total));
		const corte = provas.at(-1).corte;
		const ultimaPosicao = provas.length - 1;
		const passoX = provas.length > 1 ? larguraUtil / ultimaPosicao : larguraUtil;
		const x = (i) => MARGEM.esquerda + (provas.length > 1 ? i * passoX : larguraUtil / 2);
		const y = (valor) => MARGEM.topo + alturaUtil * (1 - valor / maximo);

		const svg = document.createElementNS(SVG, 'svg');
		svg.setAttribute('width', largura);
		svg.setAttribute('height', ALTURA);
		svg.setAttribute('viewBox', `0 0 ${largura} ${ALTURA}`);
		svg.setAttribute('role', 'img');
		svg.setAttribute('aria-label', `Acertos em cada uma das ${provas.length} provas. A tabela de provas tem os mesmos dados.`);

		for (const valor of marcas(maximo)) {
			elemento('line', { x1: MARGEM.esquerda, x2: MARGEM.esquerda + larguraUtil, y1: y(valor), y2: y(valor), class: 'grade' }, svg);
			texto(valor, { x: MARGEM.esquerda - 8, y: y(valor), class: 'eixo', 'text-anchor': 'end', 'dominant-baseline': 'middle' }, svg);
		}

		// Eixo x: o número da prova, pulando alguns quando não cabem; o último sempre aparece.
		const salto = Math.max(1, Math.ceil(provas.length / Math.max(1, Math.floor(larguraUtil / ESPACO_ROTULO_X))));
		provas.forEach((prova, i) => {
			const mostrar = i === ultimaPosicao || (i % salto === 0 && ultimaPosicao - i >= salto);
			if (mostrar) {
				texto(prova.numero, { x: x(i), y: ALTURA - MARGEM.base + 20, class: 'eixo', 'text-anchor': 'middle' }, svg);
			}
		});

		// Nota de corte. O rótulo vai para o outro lado da linha se o último ponto estiver colado nela.
		const ultima = provas[ultimaPosicao];
		const pertoDoCorte = Math.abs(y(ultima.acertos) - y(corte)) < 18;
		const rotuloCorteAbaixo = pertoDoCorte && ultima.acertos >= corte;
		elemento('line', { x1: MARGEM.esquerda, x2: MARGEM.esquerda + larguraUtil, y1: y(corte), y2: y(corte), class: 'corte' }, svg);
		texto(`Corte ${corte}`, {
			x: MARGEM.esquerda + larguraUtil + 10,
			y: rotuloCorteAbaixo ? y(corte) + 18 : y(corte) - 6,
			class: 'rotulo-corte',
		}, svg);

		const cruz = elemento('line', { y1: MARGEM.topo, y2: MARGEM.topo + alturaUtil, class: 'cruz', visibility: 'hidden' }, svg);

		const caminho = provas.map((prova, i) => `${i === 0 ? 'M' : 'L'}${x(i)},${y(prova.acertos)}`).join(' ');
		elemento('path', { d: caminho, class: 'linha' }, svg);
		const pontos = provas.map((prova, i) => elemento('circle', { cx: x(i), cy: y(prova.acertos), r: 4, class: 'ponto' }, svg));

		// Rótulo só no último ponto; os demais valores ficam na dica e na tabela.
		texto(ultima.acertos, {
			x: x(ultimaPosicao) + 10,
			y: y(ultima.acertos),
			class: 'rotulo-ponto',
			'dominant-baseline': 'middle',
		}, svg);

		function destacar(i) {
			const prova = provas[i];
			cruz.setAttribute('x1', x(i));
			cruz.setAttribute('x2', x(i));
			cruz.setAttribute('visibility', 'visible');
			pontos.forEach((ponto, j) => ponto.classList.toggle('ativo', j === i));

			const valor = document.createElement('strong');
			valor.textContent = `${prova.acertos}/${prova.total} acertos`;
			const quando = document.createElement('span');
			quando.textContent = `Prova ${prova.numero} · ${prova.data}`;
			const situacao = document.createElement('span');
			situacao.textContent = prova.aprovado ? '✓ Aprovado' : '✗ Reprovado';
			dica.replaceChildren(valor, quando, situacao);
			dica.hidden = false;

			const aDireita = x(i) + 14 + dica.offsetWidth <= largura;
			dica.style.left = `${aDireita ? x(i) + 14 : Math.max(0, x(i) - 14 - dica.offsetWidth)}px`;
			dica.style.top = `${Math.max(0, y(prova.acertos) - dica.offsetHeight / 2)}px`;
		}

		function esconder() {
			cruz.setAttribute('visibility', 'hidden');
			pontos.forEach((ponto) => ponto.classList.remove('ativo'));
			dica.hidden = true;
		}

		// Áreas de foco: uma faixa vertical por prova, bem maior que o ponto. Clicar abre a correção.
		const larguraAlvo = Math.max(ALVO_MINIMO, passoX);
		provas.forEach((prova, i) => {
			const descricao = `Prova ${prova.numero}: ${prova.acertos} de ${prova.total} acertos, ${prova.aprovado ? 'aprovado' : 'reprovado'}. Abrir a correção.`;
			const link = elemento('a', { href: prova.url, class: 'alvo', 'aria-label': descricao }, svg);
			elemento('rect', { x: x(i) - larguraAlvo / 2, y: MARGEM.topo, width: larguraAlvo, height: alturaUtil, fill: 'transparent' }, link);
			link.addEventListener('pointerenter', () => destacar(i));
			link.addEventListener('focus', () => destacar(i));
			link.addEventListener('pointerleave', esconder);
			link.addEventListener('blur', esconder);
		});

		dica.hidden = true;
		area.replaceChildren(svg);
	}

	// Redesenha quando a largura muda (janela redimensionada, celular girado).
	let larguraDesenhada = 0;
	new ResizeObserver(() => {
		if (area.clientWidth !== larguraDesenhada) {
			larguraDesenhada = area.clientWidth;
			requestAnimationFrame(desenhar);
		}
	}).observe(area);
})();
