# Onde estamos e como continuar (handoff)

> Atualizado em **2026-08-05**. Leia este arquivo primeiro ao retomar o projeto em outra máquina.

## 1. O que é o projeto

**Ponto Municipal** — SaaS multi-tenant de ponto eletrônico para servidores públicos municipais.

| Camada | Pasta | Stack |
|---|---|---|
| Backend | `backend/` | Java 21, Spring Boot 3.3.5, Maven, package-by-feature `br.gov.ponto.*` |
| Painel web | `web/` | React 18 + TypeScript + Vite (DSGov escrito à mão em `src/index.css`, sem lib de UI) |
| App do servidor | `mobile/` | Flutter |
| Infra | `infra/` | Terraform (AWS), docker-compose, Keycloak realm, scripts de backup |
| Documentação | `docs/` | Guias, runbooks e a **legislação** (`docs/legislacao/`) |

Âncoras legais: **Portaria MTP 671/2021** (REP-P), **IN 008/2021 TCM-GO**, **LGPD**.

## 2. Como rodar

```bash
# Backend — sobe a app REAL com Postgres EMBARCADO (não precisa de Docker)
cd backend && mvn test -Dtest=DevServidorManual
# fica bloqueado servindo em http://localhost:8080; o tenant é recriado a cada boot (veja o log)
```
```bash
cd web && npm install && npm run dev     # http://localhost:5173
```
```bash
cd mobile && flutter pub get && flutter run
```

### Gates (rodar antes de qualquer commit)
```bash
cd backend && mvn -o test "-Dtest=!DevServidorManual"
```
```bash
cd web && npm run build && npm test
```
```bash
cd mobile && flutter analyze
```
**Estado atual: backend 281/281 · web build + vitest 10/10 · flutter analyze limpo.**

> ⚠️ `mvn -q` **esconde erro de compilação** e deixa relatório do surefire velho — sempre confira o
> exit code real do Maven, não o do shell.

## 3. 🔴 EM ANDAMENTO — AFD e AEJ conformes (é aqui que você para e retoma)

### Contexto
O AFD que o sistema gerava estava **estruturalmente errado** (formato herdado da Portaria 1510/2009).
Conseguimos os leiautes **vigentes** e estão versionados em `docs/legislacao/`:

- `leiaute-afd-v004-vigente.pdf` — AFD **versão 004**
- `leiaute-aej-v002-vigente.pdf` — AEJ **versão 002**
- `portaria-mtp-671-2021-dou-completa.pdf` — a portaria (arts. 74–96 + Anexo IX)

> **Armadilha importante:** os Anexos V e VI da Portaria 671 foram **REVOGADOS** pela Portaria MTP
> 1.486/2022. O leiaute hoje vive em **PDFs autônomos** no portal gov.br, que o MTE atualiza **sem
> publicar nova portaria** (a última atualização é de 31/07/2026). Quem implementar pelo anexo da
> portaria gera arquivo na versão errada. Fonte oficial a monitorar:
> `https://www.gov.br/trabalho-e-emprego/pt-br/assuntos/inspecao-do-trabalho/fiscalizacao-do-trabalho/rep`
> (o gov.br devolve 403 sem User-Agent de navegador; baixe pelo Chrome ou mande um UA no curl).

### ✅ Já feito (commitado e verde)
Pacote novo `backend/src/main/java/br/gov/ponto/relatorios/rep/` — **núcleo puro, sem Spring**:

| Arquivo | O que faz |
|---|---|
| `Crc16.java` | CRC-16/KERMIT exigido nos registros tipo 1–5. Testado com o vetor oficial (`"123456789"` → `0x2189`). |
| `CampoLeiaute.java` | Formata os tipos de campo (N, A, D, DH, H), fixa **ISO 8859-1** e terminador **CRLF**. |
| `MontadorAfd.java` | Monta o AFD: cabeçalho tipo 1 (302 ch), empregado tipo 5 (118), evento tipo 6 (36), **marcação tipo 7 (137)**, trailer tipo 9 (64) e linha de assinatura (100). |
| `MontadorAej.java` | Monta o AEJ (delimitado por `\|`): tipos 01 a 08 + trailer 99 + assinatura. |

Testes: `LeiauteAfdTest` (16) e `LeiauteAejTest` (9) — conferem **posição a posição**.

**Descoberta que motivou tudo:** o REP-P grava marcação no **registro tipo 7**, não no tipo 3
(que é de REP-C/REP-A). O sistema usava tipo 3.

Também já criada: **`V48__evento_rep.sql`** — tabela ARP para as operações do REP que não são
marcação (inclusão de empregado = tipo 5, eventos sensíveis = tipo 6), com NSR vindo da **mesma
sequência** das marcações (exigência do Anexo IX) e RLS.

### ⛔ FALTA FAZER (retome exatamente por aqui)

1. **Entidade + repositório do `evento_rep`** (a migration V48 já existe, mas nada a usa ainda).
   - `EventoRep` (tenantId, nsr, tipoRegistro 5|6, dataHora, operacao, cpf, nome, cpfResponsavel, codigoEvento).
   - Gravar o evento tipo 5 ao criar/alterar/excluir servidor (`cadastro/ServidorService`), pegando
     o NSR de `registro.NsrGenerator.next(tenantId)` — **é a mesma sequência das marcações**.

2. **Reescrever `relatorios/AfdService.gerar()`** para usar o `MontadorAfd`:
   - juntar `evento_rep` (tipos 5/6) + `registro_ponto` (tipo 7) do período e **ordenar por NSR**;
   - coletor por origem: `MOBILE`→`APLICATIVO_MOBILE(01)`, `WEB`→`BROWSER(02)`,
     `TOTEM`→`DISPOSITIVO_ELETRONICO(04)`, `AJUSTE`→`OUTRO(05)`;
   - campo 7 (on-line/off-line) vem de `RegistroPonto.isOffline()`.

3. **Criar `AejService`** (é a saída do PTRP, não do REP) usando o `MontadorAej`:
   - 01 cabeçalho · 02 REP-P (nº INPI) · 03 vínculos · 04 horários (de `Jornada`+`JornadaHorario`,
     `durJornada` em **minutos**) · 05 marcações (`fonteMarc`: origem `AJUSTE` → `"I"` **com motivo
     obrigatório**; demais → `"O"`) · 06 matrícula eSocial (só quem tem >1 vínculo) ·
     07 ausências e banco de horas (de `AusenciaProgramada` e `BancoHorasLancamento`) ·
     08 identificação do PTRP · 99 trailer.
   - `tpMarc`: `ENTRADA`/`INTERVALO_FIM` → `"E"`; `SAIDA`/`INTERVALO_INICIO` → `"S"`.

4. **Configuração nova** (dados do fornecedor, não do ente) em `application.yml`:
   ```yaml
   rep:
     inpi: ${REP_INPI:}                 # nº de registro do REP-P no INPI (art. 91) — 17 dígitos
     desenvolvedor:
       cnpj: ${REP_DEV_CNPJ:}
       nome: ${REP_DEV_NOME:}
       email: ${REP_DEV_EMAIL:}
   ptrp:
     nome: Ponto Municipal
     versao: ${PTRP_VERSAO:1.0.0}
   ```
   O AFD **deve recusar a emissão** sem o nº do INPI (como já recusa sem CNPJ do ente).

5. **Endpoints**: `GET /api/relatorios/afd/arquivo` deve baixar com o **nome exigido**
   (`MontadorAfd.nomeDoArquivo(...)` → `AFD<inpi><cnpj>REP_P.txt`) e em **ISO-8859-1**.
   Criar o equivalente para o AEJ.

6. **Comprovante do trabalhador (art. 79, VIII)**: deve exibir o **hash SHA-256 da marcação**.
   `MontadorAfd.marcacao(...)` já devolve esse hash — falta persistir/expor.
   Atenção: o hash atual em `registro_ponto.hash` é a `CadeiaHash` INTERNA (fórmula diferente da
   oficial); são coisas distintas, não confundir.

7. **Apagar o método antigo** `AfdService.gerarAej()` (gera formato ad-hoc inválido).

## 4. Pendências que NÃO são código (providências suas)

| Item | Por quê | Base |
|---|---|---|
| **Registro do REP-P no INPI** | O número entra **dentro** do AFD e do AEJ. Sem ele o arquivo não fecha. | art. 91 |
| **e-CNPJ ICP-Brasil (A1 `.p12`)** | Assina o AFD/AEJ em CAdES (.p7s). Código pronto: basta configurar `assinatura.keystore`. | arts. 86–88 |
| **Atestado Técnico e Termo de Responsabilidade** | Você emite para cada prefeitura, em PDF assinado com certificado ICP-Brasil **de pessoa física**. Modelo no Anexo VII. | art. 89 |
| **DPA + Encarregado (DPO)** | Operar dados de servidores sem isso é exposição direta. | LGPD |
| **Keycloak, TLS/DNS, nuvem** | Ver `docs/passo-a-passo-implantacao.md`. | — |
| **Contas Play/Apple + política de privacidade** | O app coleta biometria, localização e áudio. | — |

## 5. Onde ler mais

- `docs/passo-a-passo-implantacao.md` — **checklist de go-live** (o mais importante).
- `docs/afd-assinatura-icp-brasil.md` — como ativar a assinatura quando o e-CNPJ chegar.
- `docs/legislacao/` — os PDFs oficiais vigentes.
- `openspec/` — especificação e backlog (§12).

## 6. Convenções do projeto

- **SOLID pragmático**: lógica de negócio em POJO puro e testável; Spring só na borda.
- **Padrão "seam"**: tudo que depende de serviço externo tem um *default* no-op seguro + uma
  implementação real que liga sozinha por configuração (IA, assinatura, push, e-mail, Keycloak,
  Redis). Ativar = definir variável de ambiente; **nunca** exige código novo.
- **Multi-tenant**: RLS no Postgres + `TenantContext`. Em produção o backend conecta com role
  **não-superusuário** (senão `FORCE RLS` não vale).
- O cabeçalho `X-Tenant-Id` **só** é aceito em requisição **não autenticada** (totem/dev). Requisição
  autenticada tira o ente do claim `tenant_id` do JWT — nunca do cabeçalho.
- Comentários e nomes em **português**; comentário explica o *porquê*, não o *o quê*.
