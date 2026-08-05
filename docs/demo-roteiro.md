# Roteiro de demonstração — Ponto Municipal

Demo de **15–20 min**, ao vivo, do problema ao "escudo jurídico". Cada bloco traz o que
falar, o que clicar e a frase de efeito. Deixe o ambiente já com um ente de exemplo e dados
de um mês fechado (use o piloto ou a massa de demonstração).

> Regra de ouro: **mostre o resultado antes de explicar o recurso.** O gestor compra desfecho
> (economia, conformidade, tranquilidade), não tela.

---

## 0. Antes de começar (2 min de preparo, fora da reunião)
- Ente de exemplo selecionado, tema/white-label com a cor e o logo da prefeitura convidada
  (Configurações → **Identidade visual**). Isso sozinho já causa efeito: "é o sistema *de vocês*".
- Um mês com movimento (batidas, hora extra, algumas faltas) para os relatórios não virem vazios.
- App instalado num celular de teste, ou o **acesso do servidor pela web** (`/servidor`) aberto.

---

## 1. A dor (1 min) — sem tela
> "Hoje, quando o TCM pede a frequência de um servidor de dois anos atrás, quanto tempo
> a prefeitura leva pra responder? E se a resposta for uma folha de papel rasurada?"

Deixe a pergunta no ar. É o problema que o resto da demo resolve.

## 2. Bater ponto (2 min) — o app do servidor
- Abra `/servidor` (ou o app). Bata um ponto.
- Mostre o **comprovante** com horário e o **hash** encadeado.
> "Cada batida entra numa cadeia encadeada por hash. Mexeu numa, quebra todas as seguintes —
> e o sistema aponta. É prova contra fraude, não promessa."
- Mostre que o app é **white-label** (logo/cor do ente) e funciona no celular do próprio servidor,
  sem catraca cara. Atualiza sozinho (OTA) — a prefeitura não reinstala nada.

## 3. O painel do RH (3 min)
- **Espelho / Fechamento**: abra o espelho de um servidor, gere o **PDF** — mostre o logo da
  prefeitura no cabeçalho do documento.
- **Banco de Horas**: saldo, teto, tudo apurado automaticamente.
- **Correções / Atestados**: mostre o fluxo de justificativa com aprovação da chefia.
> "O que o RH fazia no braço com planilha, aqui já sai apurado e assinável."

## 4. Conformidade — o escudo jurídico (3 min) — **o clímax**
- **Conformidade IN-008**: mostre o checklist verde.
- **Relatórios → AFD/AEJ**: gere o arquivo. "É o formato que o controle externo pede."
- **Dossiê de conformidade**: um pacote (IN-008 + AFD + integridade + prazo TCM) pronto pra entregar.
> "Aquela pergunta do começo? A resposta agora é um botão. Frequência, arquivo fiscal e prova
> de integridade, do período que pedirem, em PDF, com a cara da prefeitura."

## 5. Gestão e economia (3 min) — o olho do prefeito
- **BI executivo**: presença/absenteísmo e hora extra **por secretaria**.
- **Simulador / ROI**: "autorizar X horas extras custa Y e me deixa a Z% da RCL (LRF)".
> "Isso aqui não é RH, é caixa. Hora extra vira número antes de virar despesa."

## 6. Diferencial opcional — Funcionalidades ligáveis (2 min)
- Vá em Configurações → **Funcionalidades**. Mostre que tudo começa **desligado**.
- Ligue **Anomalias** na frente deles. Role até o painel que aparece, escolha o mês e clique
  **Analisar mês**: o sistema lista quem teve hora extra muito acima da média do ente.
> "Sem IA, sem caixa-preta: é uma regra explicável que o controle interno entende e defende.
> E é *opcional* — vocês ligam se quiserem."
- Aponte os recursos de **IA** (assistente, OCR de atestado, resumo executivo) como um
  próximo passo: "existem, ligam no mesmo painel, e só rodam quando vocês contratarem o provedor.
  Nada de IA ligada sem vocês saberem."

## 7. Fechamento (1 min)
> "Implantação sem obra: o servidor usa o próprio celular, vocês personalizam em minutos, e no
> primeiro fechamento já sai o dossiê pro TCM. Começamos com um piloto de uma secretaria?"

---

## Perguntas que vão aparecer (e a resposta curta)
- **"E quem não tem celular?"** → Totem de ponto por matrícula (quiosque operado pelo ente).
- **"E a LGPD?"** → Base legal de obrigação legal/exercício do poder público; RIPD/DPIA documentado;
  exportação e trilha do titular no próprio sistema.
- **"Rasteiam a localização?"** → Só cerca (geofence) opcional na batida, ligável por lotação —
  não há monitoramento contínuo.
- **"Reconhecimento facial?"** → Não fazemos antifraude facial na batida; a prova de integridade
  é a cadeia por hash + trilha de auditoria. Menos risco de LGPD, mesma robustez jurídica.
- **"Preço?"** → SaaS por ente, sem catraca/hardware. Piloto primeiro; escala depois.
