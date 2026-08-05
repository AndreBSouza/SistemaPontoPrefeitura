# Prompt do Stitch — App do servidor (mobile) — conceito "Carteira de Ponto"

> Cole no Stitch (modo Mobile). A direção de arte é proposital para o app **não** ter cara de
> template/IA: estética de **registro oficial brasileiro** (cartório/carimbo/recibo), com craft
> tipográfico e microcopy humana — e mantendo acessibilidade e o padrão gov.br.

---

## PROMPT (copie a partir daqui)

Desenhe um aplicativo mobile (Android, retrato) chamado **Ponto Municipal**, para **servidores
públicos** baterem ponto. Público amplo, incluindo pessoas idosas: a interface deve ser
**acessível** (toque mínimo 56dp, texto base 18sp, títulos 26–34sp, contraste AA+), mas com uma
**identidade visual autoral**, descrita abaixo. Não use o visual padrão de Material nem clichês de
template.

**Conceito / direção de arte: "Carteira de Ponto".** O app é um documento oficial que o servidor
carrega. Bater ponto é um ato com peso — como **carimbar** um registro num livro de cartório.
Estética de **tinta sobre papel**: sóbria, precisa, confiável, com um toque analógico (selo,
recibo, fios de pauta) que humaniza sem perder a formalidade de governo.

**Paleta (use exatamente):**
- Papel (fundo): `#F5F1E8` (off-white quente).
- Tinta (texto/escuro): `#16202E` (quase-preto azulado).
- Azul institucional (ação primária, gov.br): `#1351B4`.
- Selo (carimbo — usar com MUITA parcimônia, só em confirmação/registro): terracota `#9A3324`.
- Verde-registro (status positivo, texto/etiqueta): `#1F6E5C`.
- Neutros: sépia-cinza `#6B6256` (texto secundário), fio/hairline `#DAD3C4`.

**Tipografia:** UI em **Rawline** (ou Inter como alternativa) — grotesca humanista. **Números,
horários e NSR em monoespaçada** (IBM Plex Mono) — é o elemento-assinatura: dá sensação de
relógio de ponto e de recibo impresso. Hierarquia forte (poucos tamanhos, bem contrastados).

**Motivos visuais (assinatura):**
- **Selo circular** (carimbo) para confirmações de registro, como um carimbo pressionado num
  documento — leve "vazamento de tinta" na borda.
- **Fios finos de pauta** (hairlines `#DAD3C4`) separando registros, como num livro-ponto.
- **Comprovante com borda superior serrilhada/picotada** (recibo), com horário e NSR em mono.
- Cabeçalho fino cor de tinta com um pequeno espaço para o **brasão do ente**.

**Movimento:** o botão de ponto **afunda como um carimbo** ao tocar, e um selo "registrado"
estampa por cima com leve bleed de tinta + vibração curta. Transições discretas e propositais —
nada saltitante.

**Voz/microcopy:** português claro, respeitoso e humano, como um bom servidor que respeita seu
tempo. Ex.: "Bom dia, Maria. Pronto para registrar?", "Ponto registrado às 08:03. Bom
expediente.", "Você está fora da área de trabalho — vamos avisar a chefia." Sem "✅ Sucesso!",
sem gíria, sem tom robótico.

**EVITE (importante, para não parecer template/IA):** gradientes roxo/índigo; glassmorphism/vidro
fosco; neon; emojis como ícones de interface; sombras em tudo; ilustrações 3D genéricas; "blobs"
coloridos de fundo; tudo centralizado; cantos pill em tudo. **Prefira** tinta-sobre-papel, fios
finos, dados reais, assimetria proposital e respiro estruturado.

Telas:

1. **Ativação.** Fundo papel. Título em tinta "Ative sua carteira de ponto". Texto: "Digite o
código que o RH te entregou." Campo grande em mono para o código `AAAA-AAAA`. Botão primário azul
"Ativar carteira". Rodapé discreto com brasão + "Prefeitura de [Cidade]".

2. **Início (carimbar ponto).** Saudação "Bom dia, [Nome]" + data por extenso. **Relógio grande
em monoespaçada** (HH:MM) ao centro. Abaixo, o **botão-carimbo**: um grande disco com a borda de
selo e o verbo no centro — "REGISTRAR" — que ao tocar afunda e estampa. Sob o botão, um pequeno
recibo: "Último: Entrada · 08:03 · NSR 014" em mono. Se fora da cerca, uma **tarja cor de tinta**
discreta (não vermelha berrante): "Fora da área permitida". Ícone de menu (pauta/abas) no topo.

3. **Verificação (aparece só quando o órgão exige).** Painel sóbrio "Confirme que é você". Dois
estados conforme o aparelho: (a) **bloqueio do aparelho** — ícone de digital + "Use sua digital,
PIN ou desenho"; (b) **facial** (quando o aparelho não tem bloqueio) — câmera em moldura circular
de selo, "Centralize o rosto e pisque", a moldura vira verde-registro ao reconhecer. Botão
"Cancelar".

4. **Menu.** Lista em estilo abas de dossiê (fios finos): Comprovantes, Espelho, Banco de horas,
Justificativa, Notificações, Privacidade (LGPD), Lembretes. Item ativo marcado com um traço de
tinta à esquerda.

5. **Comprovantes.** Pilha de **recibos** (borda picotada), do mais recente ao mais antigo: tipo
da batida, data/hora e NSR em mono, e um pequeno selo. Fios finos separando. Estado vazio:
"Nenhum registro ainda."

6. **Espelho do mês.** Seletor de competência no topo (mono). Resumo: "Trabalhado 160h · Esperado
168h" em mono. Lista de dias estilo livro-ponto (pauta), com etiquetas de ocorrência discretas em
tinta colorida (atraso, falta, hora extra, fora da área) — etiquetas pequenas, não badges
chamativos.

7. **Banco de horas.** Saldo gigante em monoespaçada ao centro ("+12h 30min"), verde-registro se
positivo, terracota se negativo, com um pequeno selo de saldo. Texto em mono com o total em
minutos.

8. **Justificativa.** Formulário sóbrio: tipo (Falta, Atraso, Atestado, Licença, Férias, Outro),
período (calendário), motivo (texto), "Anexar documento", botão "Enviar ao RH". Confirmação como
um carimbo "Protocolado".

9. **Notificações.** Lista com fios finos; não lidas com um pequeno selo de tinta. Lembrete,
pendência, justificativa aprovada/recusada.

10. **Privacidade (LGPD).** Seção "Meus dados" (nome, CPF, e-mail, vínculos, registros) em cartão
de documento. Seção "Consentimentos": interruptor grande "Uso de biometria facial". Nota: "Para
excluir seus dados, fale com o RH. CPF e registros são mantidos por obrigação legal."

11. **Lembretes.** Horários (mono) com adicionar/remover e um interruptor por horário. Texto:
"Você ouvirá um alarme nesses horários para registrar o ponto."

Mantenha a mesma linguagem (papel/tinta, selo, mono nos números, fios de pauta, microcopy humana)
em todas as telas, com acessibilidade e o padrão gov.br.

## (fim do prompt)
