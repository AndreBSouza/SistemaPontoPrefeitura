# AFD e a assinatura digital ICP-Brasil

## O que já está pronto no código
- `AfdService.gerar(competencia)` produz o **AFD** (Arquivo-Fonte de Dados) em layout de largura
  fixa: cabeçalho (tipo 1), **empregados (tipo 5: CPF + nome)**, marcações (tipo 3) e trailer
  (tipo 9 com contagens), com o **CPF** do trabalhador. **Recusa emitir sem o CNPJ do ente**
  (cadastrar no painel: Identidade visual → CNPJ, ou `PUT /api/branding/cnpj`).
- Acompanha um **hash SHA-256** do conteúdo e o **hash-chain por NSR** (`CadeiaHash`, V18) — isso dá
  *tamper-evidence* (detecta adulteração) interno.
- **A assinatura CAdES está IMPLEMENTADA** (`AssinaturaCadesService`, BouncyCastle) — ver
  "Como ativar" abaixo. Sem certificado configurado, o AFD sai com hash e `assinatura=null`.
- Endpoints: `GET /api/relatorios/afd` (JSON com conteúdo + hash + assinatura) e
  `GET /api/relatorios/afd/arquivo` (download `.txt`).

## Por que a assinatura ICP-Brasil é necessária
A Portaria MTP **671/2021** exige que o REP-P (registrador de ponto via programa) **assine
digitalmente** os dados (comprovantes e AFD) com certificado **ICP-Brasil**. A assinatura agrega o
que o hash sozinho **não** dá:
- **Autenticidade + identidade jurídica** do signatário (quem gerou o arquivo);
- **Não-repúdio** (o signatário não pode negar);
- **Validade fiscal/probatória** perante a auditoria fiscal do trabalho e, no setor público, perante
  TCM-GO/TCE.

> Resumo: o hash garante "não foi alterado"; a assinatura ICP-Brasil garante "foi gerado por *este*
> responsável e tem fé pública". Uma não substitui a outra — as duas se somam.

## Como obter o certificado
1. **Tipo:** **e-CNPJ** (pessoa jurídica).
   - **A1** — arquivo `.pfx/.p12`, validade 1 ano, fica no servidor (bom para assinatura automática
     em SaaS).
   - **A3** — token/cartão/HSM, validade até 3–5 anos. Para servidor/nuvem, usar **HSM** (AWS
     CloudHSM, ou nuvem de AC como Certisign/Soluti/BirdID, Serpro).
2. **Onde:** Autoridade Certificadora credenciada pelo **ITI/ICP-Brasil** — Serpro, Serasa, Certisign,
   Valid, Soluti, Safeweb, etc. Processo: contratar → validar identidade (presencial ou por
   videoconferência) → emitir.
3. **Custo:** ~R$ 200–600/ano (e-CNPJ A1); HSM em nuvem tem mensalidade adicional.
4. **De quem é o certificado:** no modelo REP-P costuma ser o e-CNPJ do **fornecedor/responsável
   técnico pelo programa** que assina (podendo coexistir o e-CNPJ de cada **ente**). Como aqui é um
   SaaS multi-ente, o mais comum é o **fornecedor** assinar com seu e-CNPJ. **Decidir com o jurídico.**

## Como ATIVAR a assinatura (código já implementado)
A integração está pronta: `AssinaturaCadesService` (BouncyCastle `bcpkix`) assina em **CAdES/PKCS#7
(CMS) destacado** e devolve o `.p7s` em base64 no campo `assinatura` do `AfdResponse`. O bean é
`@Primary` e **liga sozinho** quando a keystore é configurada; sem config, prevalece o no-op
(`AssinaturaIndisponivel`) e o AFD sai só com hash.

1. Receber o **e-CNPJ A1** (`.p12`/`.pfx`) + senha e guardar **fora do repositório** (secret
   manager / arquivo com permissão restrita no servidor).
2. Configurar (env → propriedade):
   ```
   ASSINATURA_KEYSTORE=/seguro/ecnpj.p12    → assinatura.keystore
   ASSINATURA_SENHA=...                     → assinatura.senha
   ASSINATURA_ALIAS=...                     → assinatura.alias (opcional; default = 1º alias com chave)
   ```
3. Reiniciar o backend. Keystore inválida → a aplicação **não sobe** (fail-fast proposital: melhor
   do que emitir AFD sem assinatura achando que assinou).
4. Verificação: `GET /api/relatorios/afd` passa a trazer `assinatura` ≠ null (CMS destacado, base64).

Coberto por teste (`AssinaturaCadesServiceTest`): gera certificado + PKCS#12 temporários e
**verifica criptograficamente** o CMS produzido.

## Evoluções (não bloqueiam o piloto)
- **CAdES-T** (carimbo de tempo de ACT credenciada) — agrega prova de tempo à assinatura.
- **HSM via PKCS#11** (A3) — hoje o suporte é keystore A1 em arquivo.
- **Keystore por tenant** — hoje é 1 keystore por instância (modelo "fornecedor assina", ou uma
  config por ente); multi-keystore dinâmica é evolução.
- Registros **tipo 6** (eventos do REP) e homologação formal do leiaute no programa de tratamento
  do MTP (tipo 5, CNPJ e trailer já estão no arquivo).
