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

## Como usar

O simulador roda no Docker, então não é preciso instalar Java nem banco de dados. Funciona no Windows, no macOS e
no Linux, em processadores Intel/AMD e ARM (como Macs com Apple Silicon). A imagem fica no
[GitHub Packages](https://github.com/LucasGMonteGomes/simulador-detran-mg/pkgs/container/simulador-detran-mg)
deste repositório.

> [!TIP]
> Para copiar um comando, passe o mouse sobre o bloco e clique no botão de copiar, no canto direito.

### 1. Instale o Docker

- **Windows ou macOS:** instale o [Docker Desktop](https://docs.docker.com/desktop/) e abra o programa. Ele
  precisa estar aberto sempre que você for usar o simulador.
- **Linux:** instale o [Docker Engine](https://docs.docker.com/engine/install/) com o plugin do Docker Compose.

Para conferir, abra um terminal (no Windows, o PowerShell) e rode o comando abaixo. Ele deve mostrar a versão do
Docker Compose.

```bash
docker compose version
```

### 2. Crie uma pasta para o simulador

```bash
mkdir simulador-detran-mg
cd simulador-detran-mg
```

### 3. Baixe o arquivo de configuração

O arquivo diz ao Docker o que subir: a aplicação e um PostgreSQL só para ela. Ele é salvo com o nome
`compose.yaml`, que é o nome que o Docker Compose procura sozinho.

Linux e macOS:

```bash
curl -o compose.yaml https://raw.githubusercontent.com/LucasGMonteGomes/simulador-detran-mg/main/compose.app.yaml
```

Windows (PowerShell):

```powershell
curl.exe -o compose.yaml https://raw.githubusercontent.com/LucasGMonteGomes/simulador-detran-mg/main/compose.app.yaml
```

Prefere o navegador? Abra o [`compose.app.yaml`](compose.app.yaml), clique em **Download raw file** e salve o
arquivo na pasta do simulador com o nome `compose.yaml`.

### 4. Inicie o simulador

```bash
docker compose up -d
```

Na primeira vez, o Docker baixa cerca de 330 MB, o que pode levar alguns minutos.

### 5. Abra no navegador

Acesse **http://localhost:8080**. Se a página não abrir de primeira, espere alguns segundos: a aplicação demora
um pouco para iniciar.

> [!NOTE]
> Se a tela inicial disser **"Ainda não dá para montar uma prova"**, a imagem foi gerada antes de existirem
> questões revisadas suficientes (veja [Banco de questões](#banco-de-questões)).

### No dia a dia

Rode os comandos sempre de dentro da pasta `simulador-detran-mg`.

**Parar o simulador.** O histórico de provas continua salvo.

```bash
docker compose down
```

**Iniciar de novo.**

```bash
docker compose up -d
```

Enquanto não for parado com o `down`, o simulador volta sozinho quando o computador reinicia. No Windows e no
macOS, isso depende de o Docker Desktop abrir junto com o sistema.

**Atualizar para a versão mais nova.** Baixa a imagem nova e reinicia a aplicação. O histórico continua salvo.

```bash
docker compose up -d --pull always
```

**Ver os logs**, se algo der errado. Para sair, aperte `Ctrl+C`.

```bash
docker compose logs -f app
```

**Apagar tudo, inclusive o histórico de provas.** Não tem como desfazer.

```bash
docker compose down -v
```

### Trocar a porta ou fixar uma versão

Essas opções ficam num arquivo `.env`, na pasta do simulador. O Docker Compose lê esse arquivo sozinho, então
a configuração vale para todos os comandos acima.

| Variável | Para que serve | Padrão |
|---|---|---|
| `SIMULADOR_PORTA` | Porta do simulador no seu computador | `8080` |
| `SIMULADOR_IMAGEM` | Imagem da aplicação; troque `latest` por uma versão, como `1.1.0`, para fixá-la | `ghcr.io/lucasgmontegomes/simulador-detran-mg:latest` |

Por exemplo, para usar a porta 9090 quando a 8080 já estiver ocupada:

Linux e macOS:

```bash
echo "SIMULADOR_PORTA=9090" >> .env
```

Windows (PowerShell):

```powershell
Add-Content .env "SIMULADOR_PORTA=9090"
```

Depois, aplique a mudança com `docker compose up -d` e abra http://localhost:9090.

### Problemas comuns

- **`Cannot connect to the Docker daemon` ou `error during connect`:** o Docker não está rodando. No Windows e no
  macOS, abra o Docker Desktop e espere ele terminar de iniciar.
- **`permission denied while trying to connect to the Docker daemon socket` (Linux):** o seu usuário não está no
  grupo `docker`. Siga os [passos depois da instalação](https://docs.docker.com/engine/install/linux-postinstall/)
  ou rode os comandos com `sudo`.
- **`port is already allocated`:** outro programa já usa a porta 8080. Troque a porta no `.env`, como mostrado
  acima.
- **`no configuration file provided: not found`:** o terminal não está na pasta do simulador, ou o arquivo não
  se chama `compose.yaml`. Entre na pasta com `cd` e confira o nome do arquivo.

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
