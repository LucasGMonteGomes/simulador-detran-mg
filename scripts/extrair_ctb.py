"""Converte o CTB compilado do Planalto (HTML) em texto puro para validar os trechos citados nas questões.

Uso:
    curl -L -o l9503compilado.htm https://www.planalto.gov.br/ccivil_03/leis/l9503compilado.htm
    python3 scripts/extrair_ctb.py l9503compilado.htm src/main/resources/fontes/ctb.txt

Texto riscado (redação revogada) é descartado: um trecho copiado de regra antiga não deve ser aceito.
Notas como "(Redação dada pela Lei ...)" e "(Vigência)" também saem, para não quebrar trechos ao meio.
"""

import html.parser
import re
import sys

BLOCOS = {"p", "br", "div", "tr", "li", "h1", "h2", "h3", "h4", "table"}
IGNORADOS = {"script", "style", "head"}
RISCADOS = {"strike", "s", "del"}

NOTAS = re.compile(
    r"\((?:Reda[çc][ãa]o dada|Inclu[íi]d[oa]|Acrescid[oa]|Renumerad[oa]|Revogad[oa]|Vide|Vig[êe]ncia|"
    r"Regulamento|Promulga[çc][ãa]o|Produ[çc][ãa]o de efeito|Convers[ãa]o)[^()]*\)",
    re.IGNORECASE,
)


class ExtratorCtb(html.parser.HTMLParser):

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.partes = []
        self.ignorando = 0
        # Pilha de spans: True quando o span (ou um ancestral) marca texto riscado.
        self.spans = []

    def _riscado(self):
        return self.ignorando > 0 or any(self.spans)

    def handle_starttag(self, tag, attrs):
        if tag in IGNORADOS or tag in RISCADOS:
            self.ignorando += 1
        elif tag == "span":
            estilo = (dict(attrs).get("style") or "").replace(" ", "").lower()
            self.spans.append("line-through" in estilo)
        if tag in BLOCOS:
            self.partes.append("\n")

    def handle_startendtag(self, tag, attrs):
        if tag in BLOCOS:
            self.partes.append("\n")

    def handle_endtag(self, tag):
        if tag in IGNORADOS or tag in RISCADOS:
            self.ignorando = max(0, self.ignorando - 1)
        elif tag == "span" and self.spans:
            self.spans.pop()
        if tag in BLOCOS:
            self.partes.append("\n")

    def handle_data(self, data):
        if not self._riscado():
            # Quebras de linha no HTML são só formatação do código-fonte; parágrafos vêm das tags de bloco.
            self.partes.append(data.replace("\r", " ").replace("\n", " "))


def extrair(conteudo_html):
    extrator = ExtratorCtb()
    extrator.feed(conteudo_html)
    texto = "".join(extrator.partes).replace(" ", " ")
    linhas = []
    for linha in texto.split("\n"):
        # Normaliza espaços antes e depois: as notas podem vir com espaços duplos no meio.
        linha = re.sub(r"\s+", " ", linha)
        linha = re.sub(r"\s+", " ", NOTAS.sub(" ", linha)).strip()
        if linha:
            linhas.append(linha)
    return "\n".join(linhas) + "\n"


def main():
    if len(sys.argv) != 3:
        sys.exit("uso: extrair_ctb.py <entrada.htm> <saida.txt>")
    entrada, saida = sys.argv[1], sys.argv[2]
    with open(entrada, "rb") as arquivo:
        bruto = arquivo.read()
    # O Planalto publica em windows-1252; cai para UTF-8 se o arquivo já vier convertido.
    try:
        conteudo = bruto.decode("utf-8")
    except UnicodeDecodeError:
        conteudo = bruto.decode("windows-1252")
    with open(saida, "w", encoding="utf-8") as arquivo:
        arquivo.write(extrair(conteudo))


if __name__ == "__main__":
    main()
