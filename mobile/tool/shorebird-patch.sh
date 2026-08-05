#!/usr/bin/env bash
# Empurra um PATCH (OTA) sobre o último release — SEM passar pela loja.
# Use para correções de código Dart. Mudança nativa (novo plugin/permissão/som)
# exige um novo release + submissão à loja.
# Uso: tool/shorebird-patch.sh [android|ios]   (padrão: android)
set -euo pipefail
PLATAFORMA="${1:-android}"
echo ">> shorebird patch ${PLATAFORMA} (os aparelhos aplicam ao abrir o app)"
shorebird patch "${PLATAFORMA}"
