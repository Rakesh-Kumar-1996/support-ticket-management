#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ROOT_DIR}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

if [[ -z "${JAVA_HOME:-}" && -d "${HOME}/.local/java/jdk-21.0.12.1+1" ]]; then
  export JAVA_HOME="${HOME}/.local/java/jdk-21.0.12.1+1"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

cd "$(dirname "${BASH_SOURCE[0]}")"
./mvnw spring-boot:run
