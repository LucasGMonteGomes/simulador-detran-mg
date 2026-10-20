// Tela da prova: cronômetro, salvamento automático da resposta e tempo gasto por questão.
// A página funciona sem este script (cada botão envia o formulário); ele só melhora a experiência.
(() => {
	'use strict';

	const form = document.getElementById('form-questao');
	if (!form) {
		return;
	}

	const cronometro = document.getElementById('cronometro');
	const campoTempo = form.querySelector('input[name="tempoGastoMs"]');
	const urlSalvar = form.dataset.urlSalvar;
	const urlResultado = form.dataset.urlResultado;
	const totalQuestoes = Number(form.dataset.total);

	// O tempo restante vem do servidor. Daqui em diante, performance.now() mede o tempo decorrido sem depender do
	// relógio do computador: mudar a hora do sistema não altera a contagem.
	const restanteNoCarregamento = Number(form.dataset.restanteMs);
	const carregadaEm = performance.now();
	let ultimoRegistro = carregadaEm;
	let saindo = false;

	function restanteMs() {
		return restanteNoCarregamento - (performance.now() - carregadaEm);
	}

	function formatar(ms) {
		const totalSegundos = Math.max(0, Math.ceil(ms / 1000));
		const minutos = Math.floor(totalSegundos / 60);
		const segundos = totalSegundos % 60;
		return `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`;
	}

	function atualizarCronometro() {
		const restante = restanteMs();
		cronometro.textContent = formatar(restante);
		cronometro.dataset.alerta = restante <= 60_000 ? 'critico' : restante <= 5 * 60_000 ? 'atencao' : '';
		if (restante <= 0) {
			irParaResultado();
			return;
		}
		setTimeout(atualizarCronometro, 250);
	}

	function irParaResultado() {
		if (saindo) {
			return;
		}
		saindo = true;
		window.location.assign(urlResultado);
	}

	// Tempo desde o último envio. Zera a contagem para que cada trecho de tempo seja somado uma vez só.
	function consumirTempo() {
		const agora = performance.now();
		const decorrido = Math.round(agora - ultimoRegistro);
		ultimoRegistro = agora;
		return decorrido;
	}

	async function salvar(alternativaId) {
		const decorrido = consumirTempo();
		try {
			const resposta = await fetch(urlSalvar, {
				method: 'POST',
				body: new URLSearchParams({ alternativaId, tempoGastoMs: String(decorrido) }),
			});
			if (resposta.status === 409) {
				irParaResultado();
				return;
			}
			if (!resposta.ok) {
				throw new Error(`HTTP ${resposta.status}`);
			}
			form.querySelector('.grade-questoes [aria-current="step"]')?.classList.add('respondida');
			atualizarResumo();
		} catch (erro) {
			// A alternativa continua marcada e será enviada com o formulário ao mudar de questão; o tempo também.
			ultimoRegistro -= decorrido;
			console.warn('Resposta não salva agora; ela vai junto ao mudar de questão.', erro);
		}
	}

	function contarRespondidas() {
		return form.querySelectorAll('.grade-questoes .respondida').length;
	}

	function atualizarResumo() {
		const resumo = document.getElementById('resumo-respostas');
		const respondidas = contarRespondidas();
		if (resumo) {
			const rotulo = respondidas === 1 ? 'respondida' : 'respondidas';
			resumo.textContent = `${respondidas} ${rotulo} · ${totalQuestoes - respondidas} em branco`;
		}
	}

	function confirmarEncerramento() {
		const emBranco = totalQuestoes - contarRespondidas();
		const mensagem = emBranco > 0
			? `Você ainda tem ${emBranco} ${emBranco === 1 ? 'questão' : 'questões'} em branco. Encerrar a prova mesmo assim?`
			: 'Encerrar a prova e ver o resultado?';
		return window.confirm(mensagem);
	}

	form.addEventListener('change', (evento) => {
		if (evento.target.name === 'alternativaId') {
			salvar(evento.target.value);
		}
	});

	form.addEventListener('submit', (evento) => {
		if (evento.submitter?.value === 'encerrar' && !confirmarEncerramento()) {
			evento.preventDefault();
			return;
		}
		campoTempo.value = String(consumirTempo());
		saindo = true;
	});

	// Atalhos: A–D (ou 1–4) marcam a alternativa correspondente.
	document.addEventListener('keydown', (evento) => {
		if (evento.ctrlKey || evento.metaKey || evento.altKey || evento.key.length !== 1) {
			return;
		}
		const tecla = evento.key.toUpperCase();
		const indice = 'ABCDE'.includes(tecla) ? 'ABCDE'.indexOf(tecla) : '12345'.indexOf(tecla);
		const opcoes = form.querySelectorAll('input[name="alternativaId"]');
		if (indice < 0 || indice >= opcoes.length) {
			return;
		}
		const opcao = opcoes[indice];
		if (!opcao.checked) {
			opcao.checked = true;
			opcao.dispatchEvent(new Event('change', { bubbles: true }));
		}
		opcao.focus();
	});

	atualizarCronometro();
})();
