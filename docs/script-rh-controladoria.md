# Script de conversa — RH e Controladoria

Quem decide o "sim" técnico é o RH e o controle interno. O prefeito compra o desfecho; **estes
aqui precisam confiar que a rotina fica mais fácil e mais defensável.** Fale a língua da dor deles.

---

## Abertura (para o RH)
> "Quanto do seu mês vai embora fechando ponto na planilha, atrás de atestado e recalculando
> banco de horas na mão? E quando dá divergência, quem assume?"

Deixe listar as dores. Você vai devolver cada uma como recurso.

| Dor que eles citam | O que o sistema faz |
|---|---|
| "Fecho ponto na planilha, no braço" | Espelho e banco de horas **apurados automaticamente**; PDF pronto pra assinar |
| "Atestado é um caos de papel" | Fluxo de justificativa com aprovação da chefia; **OCR opcional** pré-preenche o atestado |
| "Correção de marcação é retrabalho" | Caixa de correções da chefia/RH, em lote, com trilha |
| "Cada secretaria faz de um jeito" | Regras por lotação (tolerância, teto de banco, cerca), um padrão só |
| "Servidor reclama que não sabe o saldo" | App do servidor mostra saldo, espelho e comprovantes |

---

## Abertura (para a Controladoria / controle interno)
> "Se o TCM abrir uma diligência hoje, você consegue provar a frequência de um período **sem
> depender da boa vontade de cada secretaria**?"

O que mostrar, nesta ordem:
1. **Conformidade IN-008**: o checklist verde, item a item.
2. **AFD/AEJ**: o arquivo fiscal gerado na hora.
3. **Integridade (cadeia por hash)**: "adulterou uma batida, o sistema aponta a quebra".
4. **Dossiê de conformidade**: IN-008 + AFD + integridade + prazo TCM, empacotado.
5. **Detecção de acúmulo de cargos e "servidor fantasma"**: varredura de irregularidades.
6. **Anomalias (opcional)**: hora extra atípica por regra **explicável** — nada de caixa-preta
   que o controle não consiga justificar num relatório.

> Mensagem-chave para o controle: **"tudo é rastreável e reproduzível."** Nenhum número aparece
> sem origem auditável.

---

## Sobre a IA — desarme a desconfiança
- Os recursos de IA (assistente do servidor, OCR de atestado, resumo executivo) são **opcionais**
  e vêm **desligados**. Ligam no painel **Funcionalidades**, um a um.
- **A IA só roda quando a prefeitura contrata o provedor.** Sem provedor, o sistema responde
  "indisponível" — não há IA silenciosa processando dado de servidor.
- As **anomalias** que importam para o controle **não usam IA**: é estatística simples e
  explicável (acima de X vezes a média e de um piso). Fácil de defender num parecer.

---

## Objeções comuns e resposta curta
- **"Vai dar trabalho migrar."** → Piloto de uma secretaria primeiro; importação de servidores por
  planilha; nada de hardware. O resto entra quando vocês validarem.
- **"E quem não tem celular?"** → Totem por matrícula, operado pelo ente.
- **"Isso vira vigilância?"** → Não. Sem monitoramento contínuo, sem facial na batida. Geofence é
  opcional e por lotação. RIPD/DPIA documentado; o titular exporta os próprios dados.
- **"E se o servidor contestar uma marcação?"** → Trilha de auditoria + comprovante com hash. A
  discussão deixa de ser "sua palavra contra a minha".
- **"E a assinatura digital do AFD?"** → O ponto de plugue já existe; entra certificado ICP-Brasil
  (e-CNPJ A1/HSM) sem mexer no resto (ver `docs/afd-assinatura-icp-brasil.md`).

---

## Fechamento (o compromisso pequeno)
> "Topam rodar um piloto de uma secretaria por um ciclo de fechamento? No fim, vocês têm o dossiê
> do TCM na mão e decidem se vale expandir. Sem hardware, sem amarração."

O "sim" aqui é pequeno e reversível — é o que destrava a expansão depois.
