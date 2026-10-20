# Simulado DETRAN-MG

[![Testes](https://github.com/LucasGMonteGomes/simulador-detran-mg/actions/workflows/testes.yml/badge.svg)](https://github.com/LucasGMonteGomes/simulador-detran-mg/actions/workflows/testes.yml)
[![Imagem](https://github.com/LucasGMonteGomes/simulador-detran-mg/actions/workflows/publicar-imagem.yml/badge.svg)](https://github.com/LucasGMonteGomes/simulador-detran-mg/pkgs/container/simulador-detran-mg)

Sistema web para estudar para a prova teórica do DETRAN-MG com simulados no formato da prova, correção
comentada e acompanhamento da evolução.

## Funcionalidades

- **Modo Prova:** 30 questões, 60 minutos e aprovação com 21 acertos (70%). As questões são sorteadas na
  proporção da carga horária do curso teórico: 12 de legislação, 10 de direção defensiva, 3 de primeiros
  socorros, 3 de meio ambiente e 2 de mecânica.
- **Sorteio inteligente:** questões que você nunca viu têm prioridade, e a prova espalha os subtemas.
- **Cronômetro no servidor:** fechar a aba ou recarregar a página não para nem reinicia o tempo. Cada resposta é
  salva assim que você marca a alternativa.
- **Correção comentada:** nota, desempenho por disciplina e, para cada questão, a resposta certa, a sua, a
  explicação e a referência legal.
- **Histórico:** gráfico de acertos por prova, média e taxa de aprovação nas últimas provas, e desempenho por
  subtema, dos pontos mais fracos para os mais fortes, com tendência de melhora ou piora.
- **Banco de questões em YAML:** fácil de revisar e de ampliar. Toda questão que cita o CTB tem o trecho da lei
  conferido automaticamente contra o texto oficial.

## Usar com Docker (o jeito mais fácil)

Só é preciso ter o [Docker](https://docs.docker.com/get-docker/) instalado, com o Docker Compose. Baixe o
arquivo de configuração e suba a aplicação:

```bash
curl -O https://raw.githubusercontent.com/LucasGMonteGomes/simulador-detran-mg/main/compose.app.yaml
docker compose -f compose.app.yaml up -d
```

Depois, abra **http://localhost:8080**. Isso sobe dois containers: a aplicação e um PostgreSQL só para ela. A
imagem da aplicação vem do [GitHub Container Registry](https://github.com/LucasGMonteGomes/simulador-detran-mg/pkgs/container/simulador-detran-mg)
e funciona em processadores Intel/AMD e ARM (como Macs com Apple Silicon).

| Para | Comando |
|---|---|
| Parar (o histórico continua salvo) | `docker compose -f compose.app.yaml down` |
| Atualizar para a versão mais nova | `docker compose -f compose.app.yaml pull && docker compose -f compose.app.yaml up -d` |
| Usar uma versão específica (ex.: 1.1.0) | `SIMULADOR_IMAGEM=ghcr.io/lucasgmontegomes/simulador-detran-mg:1.1.0 docker compose -f compose.app.yaml up -d` |
| Usar outra porta (ex.: 9090) | `SIMULADOR_PORTA=9090 docker compose -f compose.app.yaml up -d` |
| Ver os logs | `docker compose -f compose.app.yaml logs -f app` |
| Apagar tudo, inclusive o histórico | `docker compose -f compose.app.yaml down -v` |

> Se a tela inicial disser **"Ainda não dá para montar uma prova"**, a imagem foi gerada antes de existirem
> questões revisadas suficientes (veja [Banco de questões](#banco-de-questões)).

## Desenvolvimento

Pré-requisitos: **JDK 25** e **Docker**.

```bash
./mvnw spring-boot:run
```

O Spring Boot lê o [`compose.yaml`](compose.yaml) e sobe o PostgreSQL sozinho. A aplicação fica em
http://localhost:8080. Se o Java padrão do sistema não for o 25, aponte o `JAVA_HOME` para ele:

```bash
JAVA_HOME=/caminho/do/jdk-25 ./mvnw spring-boot:run
```

### Testes

```bash
./mvnw verify
```

Os testes usam um PostgreSQL de verdade, criado pelo [Testcontainers](https://testcontainers.com/), então o
Docker precisa estar rodando. No GitHub, o workflow [`testes.yml`](.github/workflows/testes.yml) roda a suíte em
todo pull request para o `main` e em todo push no `main`, e também confere se a imagem Docker continua gerando.

## Banco de questões

As questões ficam em `src/main/resources/questoes/<disciplina>/<subtema>.yaml` e são importadas para o banco
sempre que a aplicação sobe. Questão nova é criada; questão alterada vira uma nova versão, e os simulados
antigos continuam mostrando o texto que você viu na época.

```yaml
disciplina: legislacao
subtema: normas-circulacao
origem: ia
questoes:
  - codigo: CIR-001
    revisada: false
    enunciado: >
      Em uma via urbana coletora sem sinalização de velocidade, a velocidade máxima permitida é de:
    alternativas:
      - texto: 30 km/h
      - texto: 40 km/h
        correta: true
      - texto: 50 km/h
      - texto: 60 km/h
    explicacao: >
      Sem placa de velocidade, vale o limite do tipo de via urbana...
    referencia: CTB art. 61, § 1º, I, c
    trecho_fonte: quarenta quilômetros por hora, nas vias coletoras
```

**Revisão.** As questões geradas por IA entram como `revisada: false`. O **Modo Prova só usa questões
revisadas**. Para liberar uma questão, confira o gabarito e a explicação e troque para `revisada: true`.

**Validação.** Antes de gravar qualquer coisa, a importação confere todos os arquivos: uma única alternativa
correta, códigos sem repetição, subtema compatível com a disciplina e, quando a referência cita o CTB, se o
`trecho_fonte` existe no texto vigente da lei. Se houver erro, a aplicação não sobe e o log lista todos os
problemas de uma vez.

**Atualizar o texto do CTB.** O arquivo `src/main/resources/fontes/ctb.txt` é gerado a partir do CTB compilado
do Planalto, sem a redação revogada:

```bash
curl -L -o l9503compilado.htm https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm
python3 scripts/extrair_ctb.py l9503compilado.htm src/main/resources/fontes/ctb.txt
```

## Publicar uma nova versão

Quem publica a imagem é o GitHub Actions, no GitHub Container Registry (a seção **Packages** do repositório).
Ninguém precisa rodar `docker push`: basta criar a tag da versão.

1. Num branch, troque a versão no `pom.xml` (ex.: `1.2.0`) e passe o que estiver em "Não lançado" no
   [CHANGELOG](CHANGELOG.md) para uma seção com a versão e a data. Abra o PR e faça o merge.
2. Crie a tag no commit do merge e envie. O `-s` assina a tag; sem chave de assinatura configurada, use `-a`.

   ```bash
   git switch main && git pull
   git tag -s V1.2.0 -m "Versão 1.2.0"
   git push origin V1.2.0
   ```

3. O workflow [`publicar-imagem.yml`](.github/workflows/publicar-imagem.yml) gera a imagem para amd64 e arm64 e
   publica `ghcr.io/lucasgmontegomes/simulador-detran-mg:1.2.0` e `:latest`. Se a versão do `pom.xml` não for a
   da tag, ele para antes de publicar.

Para publicar uma tag que já existia ou republicar uma versão, rode o workflow pela aba **Actions** ("Run
workflow") ou pelo `gh`. Republicar uma versão antiga não mexe na `latest`, que continua na versão mais nova.

```bash
gh workflow run publicar-imagem.yml -f tag=V1.1.0
```

### Gerar a imagem localmente

O [`Dockerfile`](Dockerfile) compila o projeto dentro da imagem, então não é preciso ter Java instalado. Para
testar uma mudança dentro do Docker, sem publicar nada:

```bash
docker build -t simulador-detran-mg:local .
SIMULADOR_IMAGEM=simulador-detran-mg:local docker compose -f compose.app.yaml up -d
```

## Configuração

As regras da prova ficam em [`application.yaml`](src/main/resources/application.yaml), em `simulador.prova`. Elas
podem ser trocadas por variáveis de ambiente, inclusive no `compose.app.yaml`:

| Propriedade | Variável de ambiente | Padrão |
|---|---|---|
| `simulador.prova.total-questoes` | `SIMULADOR_PROVA_TOTALQUESTOES` | `30` |
| `simulador.prova.nota-corte` | `SIMULADOR_PROVA_NOTACORTE` | `21` |
| `simulador.prova.duracao` | `SIMULADOR_PROVA_DURACAO` | `60m` |

A aplicação não sobe se as regras forem inconsistentes (por exemplo, se a distribuição por disciplina não somar o
total de questões).

## Estrutura

```
src/main/java/.../simulador_detran_mg
├── conteudo/      disciplinas, subtemas, questões e a importação dos YAML
├── simulado/      Modo Prova: sorteio, cronômetro, respostas e correção
├── historico/     relatórios de evolução (SQL com window functions)
└── web/           controllers das telas
src/main/resources
├── db/migration/  migrations do Flyway
├── questoes/      banco de questões em YAML
├── fontes/        texto vigente do CTB, para validar as questões
├── templates/     telas (Thymeleaf)
└── static/        CSS e JavaScript
```

## Contribuindo

- Achou um gabarito errado ou uma regra desatualizada? Abra uma issue pelo formulário
  [Erro em uma questão](https://github.com/LucasGMonteGomes/simulador-detran-mg/issues/new?template=erro-em-questao.yml).
- Para revisar ou escrever questões, ou mexer no código, veja o [guia de contribuição](.github/CONTRIBUTING.md).
- Falhas de segurança seguem a [política de segurança](.github/SECURITY.md), nunca uma issue pública.
- O que mudou em cada versão está no [CHANGELOG](CHANGELOG.md).

## Aviso

As questões foram geradas com ajuda de IA a partir do texto oficial do CTB e de materiais do curso teórico. Elas
podem conter erros, então revise antes de confiar no gabarito. Este projeto é uma ferramenta de estudo e não tem
ligação com o DETRAN-MG.

## Licença

[MIT](LICENSE)
