#!/usr/bin/env bash
# Pruebas de caja negra negativas para endpoints del backend
# Requisitos: curl, jq
# Uso: ./test_negative.sh

BASE_URL="http://localhost:8080"

PASSED=0
FAILED=0
RESULTS=()

echo "Iniciando suite de pruebas negativas contra $BASE_URL"
echo

ejecutar_test() {
  local id="$1"
  local desc="$2"
  local metodo="$3"
  local endpoint="$4"
  local payload="$5"
  local esperado="$6"

  echo "----------------------------------------------------------------"
  echo "Prueba $id - $desc"
  echo "Endpoint: $metodo $BASE_URL$endpoint"
  echo "Payload: $payload"

  # Ejecutar solicitud y capturar cuerpo y código de estado
  RESPONSE=$(curl -s -w "\n%{http_code}" -X "$metodo" "$BASE_URL$endpoint" \
    -H "Content-Type: application/json" \
    -d "$payload" || true)

  BODY=$(echo "$RESPONSE" | sed '$d')
  CODE=$(echo "$RESPONSE" | tail -n1)

  echo "Estado HTTP: $CODE (esperado: $esperado)"

  if [ -z "$BODY" ]; then
    echo "Cuerpo de respuesta: [vacío]"
  else
    # Si el cuerpo es JSON, intentar extraer un mensaje corto con jq; si no, imprimir crudo
    if echo "$BODY" | jq -e . >/dev/null 2>&1; then
      # Probar campos comunes para mensajes de error
      SHORT_MSG=$(echo "$BODY" | jq -r '(.message // .error // .errorMessage // .detail // .description // .errors // .) | if type=="array" then .[0] else . end')
      echo "Respuesta (procesada): $SHORT_MSG"
    else
      echo "Respuesta (texto):"
      echo "$BODY" | sed -n '1,8p'
    fi
  fi

  if [ "$CODE" = "$esperado" ]; then
    echo "Resultado: PASS (APROBADO)"
    PASSED=$((PASSED+1))
    RESULTS+=("$id|PASS|$CODE")
  else
    echo "Resultado: FAIL (FALLIDO)"
    FAILED=$((FAILED+1))
    RESULTS+=("$id|FAIL|$CODE")
  fi
  echo
}

# Generar un sufijo aleatorio corto para evitar colisiones
RAND_SUFFIX=$(date +%s%N | cut -c1-12)

## 1) Registro de Usuario (POST /users)

# 1.1 Email con formato inválido
INVALID_USERNAME="usuario-sin-arroba-$RAND_SUFFIX"
PAYLOAD_R1=$(jq -n --arg u "$INVALID_USERNAME" --arg p "Password123!" --arg n "Neg" --arg a "Test" --arg fd "1990-01-01" --argjson roles '["CLIENTE"]' '{email:$u,password:$p,roles:$roles,nombre:$n,apellido:$a,fechaNacimiento:$fd}')
ejecutar_test "R-01" "Registro: email con formato inválido ($INVALID_USERNAME)" "POST" "/users" "$PAYLOAD_R1" "400"

# 1.2 Campos obligatorios vacíos (password null, nombre vacío)
EMPTY_USER="usuario-vacio-$RAND_SUFFIX@example.com"
PAYLOAD_R2=$(jq -n --arg u "$EMPTY_USER" --arg a "Test" --arg fd "1990-01-01" --argjson roles '["CLIENTE"]' '{email:$u,password:null,roles:$roles,nombre:"",apellido:$a,fechaNacimiento:$fd}')
ejecutar_test "R-02" "Registro: campos obligatorios vacíos (password null, nombre vacío)" "POST" "/users" "$PAYLOAD_R2" "400"

# 1.3 Fecha de nacimiento en el futuro
FUTURE_USER="usuario-futuro-$RAND_SUFFIX@example.com"
PAYLOAD_R3=$(jq -n --arg u "$FUTURE_USER" --arg p "Password123!" --arg n "Futuro" --arg a "Test" --arg fd "2030-01-01" --argjson roles '["CLIENTE"]' '{email:$u,password:$p,roles:$roles,nombre:$n,apellido:$a,fechaNacimiento:$fd}')
ejecutar_test "R-03" "Registro: fechaNacimiento en el futuro (2030-01-01)" "POST" "/users" "$PAYLOAD_R3" "400"

## 2) Pruebas de Sesión (POST /sessions)

# 2.1 Login con usuario que no existe
NON_EXISTENT_USER="noexiste-$RAND_SUFFIX@example.com"
PAYLOAD_S1=$(jq -n --arg u "$NON_EXISTENT_USER" --arg p "Cualquiera123!" '{email:$u,password:$p}')
ejecutar_test "S-01" "Sesión: login con usuario inexistente" "POST" "/sessions" "$PAYLOAD_S1" "401"

# 2.2 Login con contraseña incorrecta para usuario existente
EXIST_USER="existe-$RAND_SUFFIX@example.com"
EXIST_PASS="ClaveCorrecta123!"
# Crear el usuario primero (configuración previa) - aceptar 201 o 409 silenciosamente
SETUP_PAYLOAD=$(jq -n --arg u "$EXIST_USER" --arg p "$EXIST_PASS" --arg n "Existe" --arg a "Usuario" --arg fd "1990-01-01" --argjson roles '["CLIENTE"]' '{email:$u,password:$p,roles:$roles,nombre:$n,apellido:$a,fechaNacimiento:$fd}')
echo "Configuración: creando usuario $EXIST_USER (puede devolver 201 o 409)"
SETUP_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/users" -H "Content-Type: application/json" -d "$SETUP_PAYLOAD" || true)
echo "$SETUP_RESP" | sed -n '1,2p'

# Ahora intentar login con contraseña incorrecta
PAYLOAD_S2=$(jq -n --arg u "$EXIST_USER" --arg p "ClaveIncorrecta!" '{email:$u,password:$p}')
ejecutar_test "S-02" "Sesión: login con contraseña incorrecta (usuario existente)" "POST" "/sessions" "$PAYLOAD_S2" "401"

## 3) Pruebas de Refresh (PUT /sessions)

# 3.1 Intento de refresco con refreshToken vacío
PAYLOAD_R4=$(jq -n '{refreshToken:""}')
ejecutar_test "RF-01" "Refresh: refreshToken vacío (NotBlank)" "PUT" "/sessions" "$PAYLOAD_R4" "400"

# 3.2 Intento de refresco con refreshToken aleatorio (texto plano)
RANDOM_TOKEN="token_aleatorio_$RAND_SUFFIX"
PAYLOAD_R5=$(jq -n --arg t "$RANDOM_TOKEN" '{refreshToken:$t}')
ejecutar_test "RF-02" "Refresh: refreshToken aleatorio/inválido" "PUT" "/sessions" "$PAYLOAD_R5" "401"

## Resumen Final
echo "================================================================"
echo "Pruebas negativas completadas. Aprobadas: $PASSED, Fallidas: $FAILED"
echo "Detalles:"
for r in "${RESULTS[@]}"; do
  IFS='|' read -r id estado codigo <<< "$r"
  printf "  %s - %s (HTTP %s)\n" "$id" "$estado" "$codigo"
done
echo "================================================================"

echo "Nota: Algunas expectativas de 400 asumen validación adicional (ej. formato email o fecha pasada)."
echo "Si la implementación solo obliga a que no sea vacío, esas pruebas mostrarán FAIL."
