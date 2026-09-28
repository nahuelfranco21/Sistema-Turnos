#!/usr/bin/env bash
# Pruebas de humo funcionales automatizadas para Usuarios y Sesiones
# Uso: ./test_backend.sh
# Requisitos: curl, jq

set -euo pipefail

BASE_URL="http://localhost:8080"

USERNAME="test.user+auto@example.com"
PASSWORD="Password123!"
ROLES='["CLIENTE"]'
NOMBRE="Auto"
APELLIDO="Tester"
FECHA_NAC="1990-01-01"

echo "1) Registrando usuario..."
REG_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USERNAME\", \"password\": \"$PASSWORD\", \"roles\": $ROLES, \"nombre\": \"$NOMBRE\", \"apellido\": \"$APELLIDO\", \"fechaNacimiento\": \"$FECHA_NAC\"}")

REG_BODY=$(echo "$REG_RESPONSE" | sed '$d')
REG_CODE=$(echo "$REG_RESPONSE" | tail -n1)

echo "Código HTTP de registro: $REG_CODE"
echo "Cuerpo de la respuesta: $REG_BODY"

if [ "$REG_CODE" != "201" ] && [ "$REG_CODE" != "409" ]; then
  echo "Estado de registro inesperado: $REG_CODE"
  exit 1
fi

# Si el registro devolvió tokens, extraer el refresh token; de lo contrario, proceder al login
if echo "$REG_BODY" | jq -e .accessToken >/dev/null 2>&1; then
  ACCESS_TOKEN=$(echo "$REG_BODY" | jq -r .accessToken)
  REFRESH_TOKEN=$(echo "$REG_BODY" | jq -r .refreshToken)
  echo "Se obtuvieron los tokens de acceso y refresco desde el registro."
else
  echo "Sin tokens en el registro (el usuario ya existía). Se procederá al login."
fi

echo
echo "2) Iniciando sesión (Login)..."
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
LOGIN_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)

echo "Código HTTP de login: $LOGIN_CODE"
echo "Cuerpo de la respuesta: $LOGIN_BODY"

if [ "$LOGIN_CODE" != "201" ]; then
  echo "Fallo en el login con estado $LOGIN_CODE"
  exit 1
fi

ACCESS_TOKEN=$(echo "$LOGIN_BODY" | jq -r .accessToken)
REFRESH_TOKEN=$(echo "$LOGIN_BODY" | jq -r .refreshToken)

if [ -z "$REFRESH_TOKEN" ] || [ "$REFRESH_TOKEN" = "null" ]; then
  echo "El login no devolvió un refresh token. Saliendo."
  exit 1
fi

echo
echo "3) Refrescando token (primer uso)..."
REF_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

REF_BODY=$(echo "$REF_RESPONSE" | sed '$d')
REF_CODE=$(echo "$REF_RESPONSE" | tail -n1)

echo "Código HTTP de refresco: $REF_CODE"
echo "Cuerpo de la respuesta: $REF_BODY"

if [ "$REF_CODE" != "200" ]; then
  echo "Fallo en el refresco con estado $REF_CODE"
  exit 1
fi

NEW_ACCESS_TOKEN=$(echo "$REF_BODY" | jq -r .accessToken)
NEW_REFRESH_TOKEN=$(echo "$REF_BODY" | jq -r .refreshToken)

echo
echo "4) Intentando reutilizar el mismo refresh token (debe fallar)..."
REUSE_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/sessions" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

REUSE_BODY=$(echo "$REUSE_RESPONSE" | sed '$d')
REUSE_CODE=$(echo "$REUSE_RESPONSE" | tail -n1)

echo "Código HTTP de reutilización: $REUSE_CODE"
echo "Cuerpo de la respuesta: $REUSE_BODY"

if [ "$REUSE_CODE" != "401" ]; then
  echo "Se esperaba un 401 para el token reutilizado, pero se obtuvo $REUSE_CODE"
  exit 1
fi

echo
echo "Todos los pasos automatizados se completaron con éxito."
echo "Resumen:"
echo "  Usuario registrado (o existente): $USERNAME"
echo "  Token de refresco inicial (usado): $REFRESH_TOKEN"
echo "  Nuevo token de refresco (post-refresco): $NEW_REFRESH_TOKEN"
