#!/usr/bin/env bash
# Cria um RELEASE do Shorebird (substitui o `flutter build`). Rode a CADA versão
# que vai para a loja. O release usa a mesma assinatura do app (android/key.properties).
# Uso: tool/shorebird-release.sh [android|ios]   (padrão: android)
set -euo pipefail
PLATAFORMA="${1:-android}"
echo ">> shorebird release ${PLATAFORMA} (envie o artefato gerado à loja)"
shorebird release "${PLATAFORMA}"
