# Provedor de IA (OpenAI) — como ligar

A IA do Ponto Municipal é **opcional e plugável**. O código do provedor real (OpenAI) já está
pronto: ele só entra em ação quando existe uma **chave de API** no ambiente. Sem chave, o sistema
usa um no-op e responde "indisponível" — nada quebra.

> **Nunca** coloque a chave no código, no `application.yml` versionado, nem em prints/chats.
> Ela entra **só** por variável de ambiente / secret manager. Se uma chave vazar, **revogue e gere
> outra** no painel da OpenAI.

## Passo 1 — ter a chave
Crie uma API key em `platform.openai.com` → **API keys**. Guarde num cofre de secrets
(não no repositório).

## Passo 2 — definir a variável de ambiente
| Variável | Obrigatória | Default | Para quê |
|---|---|---|---|
| `IA_API_KEY` | **sim** | — | a chave da OpenAI. Sem ela, IA fica desligada |
| `IA_MODELO` | não | `gpt-4o-mini` | modelo (ex.: `gpt-4o` p/ mais qualidade) |
| `IA_BASE_URL` | não | `https://api.openai.com` | endpoint (proxy/gateway próprio) |
| `IA_MAX_TOKENS` | não | `1024` | teto de tokens por resposta |

Exemplos:
```bash
# Linux/macOS (ou variáveis do container/secret manager)
export IA_API_KEY="sk-..."      # a NOVA chave, nunca commitada
export IA_MODELO="gpt-4o-mini"
```
```powershell
# Windows PowerShell (sessão local de teste)
$env:IA_API_KEY = "sk-..."
```
Em produção, injete via o secret manager da sua infra (não via arquivo no git). O padrão do
projeto para segredos é o mesmo do `PONTO_CRYPTO_SECRET`.

## Passo 3 — ligar por ente no painel
Ter a chave **não** liga a IA sozinha. Cada prefeitura decide no painel **Configurações →
Funcionalidades**:
- `IA_ASSISTENTE` → assistente do servidor (`POST /api/me/assistente`)
- `IA_OCR` → OCR de atestado (`POST /api/me/atestados/ocr`)
- `IA_RESUMO` → resumo executivo (`GET /api/relatorios/resumo-ia`)

Um recurso só responde quando **as duas** condições valem: a função ligada no painel **e** a chave
configurada. Caso contrário, retorna `disponivel:false` de forma graciosa.

## O que cada recurso usa
- **Assistente** e **resumo**: Chat Completions (texto).
- **OCR de atestado**: Chat Completions com **visão** — a imagem vai como `data:` URI base64 e o
  modelo devolve os campos (CID, início, dias, profissional, CRM) em JSON, que o backend estrutura
  em `OcrAtestado`; se o modelo não estruturar, devolve o texto bruto para o RH revisar.

## Custo e LGPD (importante no setor público)
- **Custo:** `gpt-4o-mini` é barato e já faz OCR; suba para `gpt-4o` só se precisar de mais
  qualidade. O teto por resposta é o `IA_MAX_TOKENS`.
- **LGPD:** ao ligar a IA, dados podem trafegar para a OpenAI (nuvem externa). Avalie contrato/DPA
  com o fornecedor, evite mandar dado pessoal identificável além do necessário, e registre a
  decisão no RIPD/DPIA (`docs/dpia-ripd.md`). Se o ente exigir dado **on-premise**, dá para trocar
  o provedor sem mexer no resto: basta outra implementação de `IaProvider` marcada `@Primary`
  (o `OpenAiIaProvider` é só uma das opções do seam).
- **⚠️ OCR de atestado = dado de saúde (categoria especial, LGPD art. 11):** o `IA_OCR` envia a
  imagem do atestado (com CID) ao provedor externo. Por isso o OCR é gated por **três** cancelas:
  (1) função ligada no painel, (2) chave configurada e (3) **consentimento específico do servidor**.
  Sem consentimento, `POST /api/me/atestados/ocr` retorna **403** com
  `{"codigo":"CONSENTIMENTO_NECESSARIO","finalidade":"IA_OCR_SAUDE"}` — o app deve então pedir o
  consentimento e repetir. O servidor concede/revoga em `POST /api/me/lgpd/consentimento`
  (`{"finalidade":"IA_OCR_SAUDE","concedido":true}`). Ainda assim, **registre a base legal no RIPD**
  (`docs/dpia-ripd.md`) e considere um provedor **on-premise** para dados de saúde.

  Fluxo do app: chamar o OCR → se `403 CONSENTIMENTO_NECESSARIO`, exibir o termo e, no aceite,
  `POST /api/me/lgpd/consentimento` com a finalidade `IA_OCR_SAUDE` → repetir o OCR.

## Desligar tudo / on-premise (controles LGPD)
- **Kill-switch do ambiente:** `IA_PERMITIR_EXTERNO=false` desliga **toda** a IA (assistente, OCR,
  resumo, sentimento) — mesmo com a chave configurada e mesmo com as funções ligadas por ente. Serve
  ao controle interno/jurídico como trava única. Com ele desligado, todos os recursos respondem
  "indisponível".
- **On-premise (dado não sai do ambiente):** aponte `IA_BASE_URL` para um **gateway interno** que
  fale o protocolo Chat Completions (ex.: um modelo self-hosted). O código do provedor não muda —
  só o endpoint. Recomendado para dado de saúde (OCR de atestado).
- **Minimização:** os prompts enviam apenas o necessário — o assistente manda a pergunta + o
  `vinculoId` (UUID, não identifica a pessoa por si); o resumo manda indicadores agregados (sem
  dado pessoal); o sentimento é limitado/truncado. O provedor **não loga** o conteúdo dos prompts
  (só o tipo de exceção em falha). Registre as bases legais e transferências no RIPD (`dpia-ripd.md`).

## Resiliência
- Timeouts: 10s de conexão, 60s de leitura.
- Falha da API (rede/4xx/5xx) vira **503** com mensagem amigável (sem stack trace); o erro é
  logado no servidor sem expor a chave.
