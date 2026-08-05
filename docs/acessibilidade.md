# Acessibilidade — Ponto Municipal

Diretrizes de acessibilidade para **web** e **mobile**, com conformidade a
**WCAG 2.1 nível AA**, ao **eMAG 3.1** (Modelo de Acessibilidade em Governo Eletrônico)
e às boas práticas de plataforma móvel.

> Como software para o **setor público**, a acessibilidade é **obrigatória** (Lei Brasileira
> de Inclusão 13.146/2015 + Decreto 5.296/2004 + eMAG). Meta de produto: **WCAG 2.1 AA**.

---

## 1. Princípios (POUR — WCAG)

- **Perceptível:** todo conteúdo tem alternativa percebível (texto, contraste, legendas).
- **Operável:** tudo funciona por teclado e por toque; sem armadilhas de foco.
- **Compreensível:** linguagem clara em português, comportamento previsível, erros explicados.
- **Robusto:** compatível com tecnologias assistivas (leitores de tela) atuais e futuras.

---

## 2. Web (React + TypeScript) — WCAG 2.1 AA + eMAG

### 2.1 Estrutura e semântica
- [ ] HTML semântico (`<header> <nav> <main> <button> <label>`), não `div` clicável.
- [ ] **Landmarks** e **hierarquia de headings** (`h1`→`h2`→...) sem pular níveis.
- [ ] `lang="pt-BR"` no `<html>`; títulos de página únicos e descritivos.
- [ ] **Skip link** "Pular para o conteúdo principal" (eMAG recomenda atalhos de navegação).

### 2.2 Teclado e foco
- [ ] Tudo operável só com teclado (Tab/Shift+Tab/Enter/Espaço/Esc/setas).
- [ ] **Foco visível** com contraste suficiente (não remover `outline` sem substituto).
- [ ] Ordem de foco lógica; sem *keyboard trap*; modais com foco preso **dentro** e retorno ao fechar.

### 2.3 Formulários (cadastro, espelho, upload de atestado)
- [ ] Todo campo com `<label>` associado (não usar só placeholder).
- [ ] Erros descritos em texto, associados ao campo (`aria-describedby`), com instrução de correção.
- [ ] Campos obrigatórios indicados em texto, não só por cor.
- [ ] Mensagens de status/erro anunciadas a leitores de tela (`aria-live`/`role="alert"`).

### 2.4 Cor e contraste
- [ ] Contraste de texto **≥ 4,5:1** (≥ 3:1 para texto grande) e de componentes/ícones **≥ 3:1**.
- [ ] **Cor nunca é o único meio** de transmitir informação (status de marcação, erro, sucesso).
- [ ] Funciona em zoom até **200%** sem perda de conteúdo/função; layout responsivo.
- [ ] Respeitar `prefers-reduced-motion`; sem conteúdo piscando > 3x/s.

### 2.5 Imagens e mídia
- [ ] `alt` significativo em imagens informativas; `alt=""` em decorativas.
- [ ] Ícones-botão com nome acessível (`aria-label`).
- [ ] Captcha acessível (alternativa não visual), se houver.

### 2.6 Componentes dinâmicos
- [ ] Componentes ARIA seguem o padrão (combobox, tabs, dialog) com estados (`aria-expanded`, etc.).
- [ ] Tabelas de dados (espelho de ponto) com `<th scope>` e `<caption>`.
- [ ] Notificações/toasts anunciados via `aria-live`.

---

## 3. Mobile (Flutter — Android, e iOS na fase seguinte)

### 3.1 Leitores de tela e foco
- [ ] Todos os widgets interativos têm `Semantics`/`label`; ícones com rótulo.
- [ ] Funciona com **TalkBack** (Android) e **VoiceOver** (iOS); ordem de leitura lógica.
- [ ] Estados anunciados (selecionado, desabilitado, carregando).
- [ ] Conteúdo decorativo marcado como `excludeSemantics`.

### 3.2 Alvos de toque e contraste
- [ ] Alvos de toque **≥ 48x48 dp** (Android) / **≥ 44x44 pt** (iOS).
- [ ] Mesmos critérios de contraste do web (4,5:1 texto / 3:1 componentes).
- [ ] Respeita o **tamanho de fonte do sistema** (escala dinâmica) sem quebrar layout.

### 3.3 Fluxos críticos acessíveis
- [ ] **Registro de ponto** completável por leitor de tela.
- [ ] **Biometria facial:** instruções por áudio/voz e **alternativa acessível** (registro
      assistido/manual auditado) para quem não consegue usar a câmera/face — coerente com LGPD
      e antifraude (ver `docs/dpia-ripd.md`).
- [ ] **Upload de atestado** acessível (seleção de arquivo/câmera rotulada).
- [ ] Mensagens de erro em texto, não só vibração/cor.
- [ ] Funciona em **modo retrato e paisagem** e com gestos do sistema.

---

## 4. Testes e ferramentas

| Camada | Ferramentas |
|---|---|
| Web automatizado | axe-core / Lighthouse / pa11y no CI |
| Web manual | Navegação só por teclado; leitor NVDA (Win) e VoiceOver (mac) |
| Mobile | Accessibility Scanner (Android), Accessibility Inspector (iOS), `flutter test` com `Semantics` |
| Contraste | Verificador de contraste (WebAIM) |
| Validação eMAG | ASES (validador do Governo Federal) |

Diretrizes de teste:
- [ ] CI falha em regressão crítica de acessibilidade nos fluxos principais.
- [ ] **Teste manual com teclado** e **com leitor de tela** a cada release dos fluxos críticos
      (login, registro de ponto, espelho, upload).
- [ ] Incluir pessoa com deficiência no teste de usabilidade do piloto (ver `docs/plano-piloto.md`).

---

## 5. Governança

- [ ] **VPAT/declaração de conformidade** WCAG 2.1 AA mantida e publicada.
- [ ] Acessibilidade no *Definition of Done* de toda feature de UI.
- [ ] Canal para reportar barreiras de acessibilidade + SLA de correção.
- [ ] Revisão de acessibilidade no design (antes do código), não só ao final.

> Documento vivo — revisar a cada novo fluxo de UI e a cada atualização do WCAG/eMAG.
