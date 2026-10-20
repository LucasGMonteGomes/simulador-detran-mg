# Como contribuir

Obrigado pelo interesse! Este é um projeto de estudo para a prova teórica do DETRAN-MG, e as contribuições mais
valiosas são as que deixam as questões mais corretas.

## Formas de contribuir

- **Achou um erro em uma questão?** Abra uma issue com o formulário
  [Erro em uma questão](https://github.com/LucasGMonteGomes/simulador-detran-mg/issues/new?template=erro-em-questao.yml).
  Não precisa saber programar.
- **Quer revisar ou escrever questões?** Veja [Questões](#questões) abaixo.
- **Quer mexer no código?** Veja [Código](#código).
- **Achou uma falha de segurança?** Não abra uma issue: siga a [política de segurança](SECURITY.md).

## Preparando o ambiente

Você vai precisar do **JDK 25** e do **Docker** (os testes usam um PostgreSQL de verdade via Testcontainers).

```bash
./mvnw spring-boot:run   # sobe a aplicação em http://localhost:8080 (o PostgreSQL sobe junto)
./mvnw verify            # roda todos os testes
```

## Fluxo de trabalho

1. Crie um branch a partir do `main` com um prefixo que diga o tipo da mudança: `questoes/`, `feat/`, `fix/` ou
   `docs/` (ex.: `questoes/revisa-legislacao`).
2. Escreva os commits em português, no imperativo, dizendo o que o commit faz: "Adiciona questões de sinalização
   horizontal", "Corrige gabarito da INF-002".
3. Abra um pull request para o `main` e preencha o checklist do template. O GitHub Actions roda os testes e
   confere se a imagem Docker ainda é gerada; o merge só acontece com tudo verde.
4. Se a mudança for visível para quem usa, registre no [`CHANGELOG.md`](../CHANGELOG.md), em "Não lançado".

## Questões

As questões ficam em `src/main/resources/questoes/<disciplina>/<subtema>.yaml`, uma lista por subtema. O formato
completo está no [README](../README.md#banco-de-questões).

### Regras

- Quatro alternativas, **exatamente uma correta**, explicação e referência.
- Se a referência começa com `CTB`, o `trecho_fonte` precisa ser uma cópia literal do
  [texto vigente do CTB](https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm). A importação confere
  isso contra `src/main/resources/fontes/ctb.txt` e recusa trechos que não existem ou que vêm de redação revogada.
- Varie a posição da alternativa correta: quem chuta sempre a mesma letra não pode levar vantagem (um teste
  confere que nenhuma letra passa de 35% do gabarito).
- Escreva a explicação para quem errou: diga por que a resposta certa é certa e, se for o caso, qual é a
  pegadinha.

### Códigos

Cada questão tem um código com o prefixo do subtema e um número sequencial (ex.: `INF-005`). **Nunca reaproveite
nem altere o código de uma questão existente**: para o importador, código novo é questão nova, e a antiga
continuaria ativa no banco.

| Prefixo | Arquivo | Prefixo | Arquivo |
|---|---|---|---|
| `HAB` | `legislacao/habilitacao.yaml` | `DDC` | `direcao-defensiva/conceitos-defensiva.yaml` |
| `CIR` | `legislacao/normas-circulacao.yaml` | `DDA` | `direcao-defensiva/condicoes-adversas.yaml` |
| `SRE` | `legislacao/sinalizacao-regulamentacao.yaml` | `DDE` | `direcao-defensiva/evitar-colisoes.yaml` |
| `SAD` | `legislacao/sinalizacao-advertencia.yaml` | `DDS` | `direcao-defensiva/estado-condutor.yaml` |
| `SIN` | `legislacao/sinalizacao-indicacao.yaml` | `DDV` | `direcao-defensiva/usuarios-vulneraveis.yaml` |
| `SHO` | `legislacao/sinalizacao-horizontal.yaml` | `PSS` | `primeiros-socorros/sinalizacao-acidente.yaml` |
| `SLG` | `legislacao/sinalizacao-luminosa-gestos.yaml` | `PSA` | `primeiros-socorros/avaliacao-vitima.yaml` |
| `INF` | `legislacao/infracoes-penalidades.yaml` | `PSC` | `primeiros-socorros/cuidados-vitima.yaml` |
| `CRI` | `legislacao/crimes-transito.yaml` | `MAP` | `meio-ambiente/poluicao.yaml` |
| `VEI` | `legislacao/veiculos.yaml` | `MAC` | `meio-ambiente/cidadania.yaml` |
|  |  | `MES` | `mecanica/sistemas-veiculo.yaml` |
|  |  | `MEM` | `mecanica/manutencao-painel.yaml` |

Um **subtema novo** precisa de uma migration do Flyway que o cadastre (os subtemas vêm de
`V3__inserir_disciplinas_e_subtemas.sql`) e de um prefixo novo nesta tabela.

### Revisão

Questões geradas por IA entram com `revisada: false`, e o **Modo Prova só usa questões revisadas**. Marque
`revisada: true` apenas depois de conferir o gabarito e a explicação na fonte. Para corrigir uma questão,
edite o texto no próprio arquivo: a importação cria uma nova versão automaticamente, e as provas antigas
continuam mostrando o texto da época.

## Código

- Siga o estilo do código em volta: tabulação para indentar, nomes em português e comentários que explicam o
  *porquê*, não o *quê*.
- Regras de negócio novas vêm com testes. Os testes de unidade não usam banco; os que dependem do banco usam
  `@DataJpaTest` com o PostgreSQL do Testcontainers.
- **Nunca altere uma migration que já existe**: o Flyway guarda o checksum de cada uma e a aplicação deixa de
  subir. Crie uma nova (`V6__...`, `V7__...`).
- Telas: confira no navegador, no tema claro e no escuro e na largura de um celular.

## Conduta

Ao participar, você concorda com o nosso [código de conduta](CODE_OF_CONDUCT.md).
