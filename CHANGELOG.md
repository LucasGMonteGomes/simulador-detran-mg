# Changelog

As mudanças que importam para quem usa o simulador ficam registradas aqui. O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), e as versões seguem o
[Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

## [1.1.1] - 2026-10-28

### Adicionado

- Imagem publicada no GitHub Container Registry (`ghcr.io/lucasgmontegomes/simulador-detran-mg`) a cada nova
  versão, para processadores Intel/AMD e ARM.

### Alterado

- As 60 questões do banco passam a ser usadas no Modo Prova (`revisada: true`). Antes, a imagem publicada
  mostrava "Ainda não dá para montar uma prova".
- O `compose.app.yaml` passa a baixar a imagem do GitHub Container Registry.
- README com um passo a passo de instalação para Windows, macOS e Linux, comandos curtos (o arquivo é salvo como
  `compose.yaml`), configuração da porta e da versão pelo `.env` e soluções para os erros mais comuns.

## [1.1.0] - 2026-10-19

### Adicionado

- Histórico de provas: resumo com a média e a aprovação nas últimas 5 provas, gráfico de acertos por prova e
  desempenho por subtema, com tendência de melhora ou piora.
- Imagem Docker e `compose.app.yaml` para usar o simulador sem instalar o Java.
- Testes automáticos no GitHub Actions em pull requests e no `main`, incluindo o build da imagem.
- README com instruções de uso, desenvolvimento e publicação da imagem.
- Licença MIT.
- Guia de contribuição, política de segurança, código de conduta, templates de issue e de pull request, e
  atualização automática de dependências com o Dependabot.

### Alterado

- O Modo Prova passa a ter 60 minutos (antes, 40).

## [1.0.0] - 2026-10-19

### Adicionado

- Modo Prova com 30 questões na distribuição oficial por disciplina, nota de corte de 21 acertos e cronômetro
  controlado pelo servidor.
- Sorteio que dá prioridade às questões inéditas e espalha os subtemas pela prova.
- Telas de início, de questão (salvamento automático, atalhos de teclado e grade das 30 questões) e de
  resultado, com o gabarito comentado.
- Banco inicial de 60 questões em YAML, com os trechos do CTB conferidos contra o texto vigente da lei.
- Importação das questões na inicialização, com uma nova versão sempre que uma questão é alterada.

[Não lançado]: https://github.com/LucasGMonteGomes/simulador-detran-mg/compare/V1.1.1...HEAD
[1.1.1]: https://github.com/LucasGMonteGomes/simulador-detran-mg/compare/V1.1.0...V1.1.1
[1.1.0]: https://github.com/LucasGMonteGomes/simulador-detran-mg/compare/V1.0.0...V1.1.0
[1.0.0]: https://github.com/LucasGMonteGomes/simulador-detran-mg/tree/V1.0.0
