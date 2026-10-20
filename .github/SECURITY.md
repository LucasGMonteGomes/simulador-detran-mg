# Política de segurança

## Para que o projeto foi feito

O Simulado DETRAN-MG é uma ferramenta de estudo para **uso pessoal, rodando no próprio computador**. Ele não
tem login nem separação entre usuários. Por isso:

- **Não exponha a aplicação na internet.** O [`compose.app.yaml`](../compose.app.yaml) publica a porta apenas em
  `127.0.0.1` de propósito; mudar isso deixa qualquer pessoa da rede ver e alterar o seu histórico.
- As senhas do PostgreSQL nos arquivos de compose servem só para o banco local, que não fica acessível de fora do
  Docker. Se você adaptar o projeto para outro ambiente, troque-as.

## Versões com suporte

Só a versão mais recente recebe correções: o branch `main` e a imagem `latest` no Docker Hub. Se estiver usando
uma imagem antiga, atualize com `docker compose -f compose.app.yaml pull` antes de reportar.

## Como reportar uma vulnerabilidade

**Não abra uma issue pública.** Use o reporte privado do GitHub:

1. Vá até a aba **Security** do repositório.
2. Clique em **Report a vulnerability**
   ([link direto](https://github.com/LucasGMonteGomes/simulador-detran-mg/security/advisories/new)).

Inclua, se possível:

- o que é possível fazer explorando a falha e em que versão (commit ou tag da imagem);
- os passos para reproduzir;
- uma sugestão de correção, se tiver.

Este é um projeto pessoal mantido nas horas vagas: a resposta vem assim que possível, normalmente em alguns
dias. Depois da correção, o aviso de segurança é publicado com o crédito a quem reportou, se a pessoa quiser.
