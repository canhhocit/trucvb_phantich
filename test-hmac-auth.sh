#!/bin/bash

# Test HMAC Authentication & Authorization
# Usage: ./test-hmac-auth.sh <agency_code> <api_key> <secret> <target_path>

set -e

AGENCY_CODE=${1:-AGENCY_A}
API_KEY=${2:-tvb_live_xxx}
SECRET=${3:-your_secret_here}
TARGET_PATH=${4:-/${AGENCY_CODE}/transactions/received}

BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)
NONCE=$(uuidgen || openssl rand -hex 16)

echo "========================================="
echo "HMAC Authentication Test"
echo "========================================="
echo "Agency Code: $AGENCY_CODE"
echo "API Key: $API_KEY"
echo "Target Path: $TARGET_PATH"
echo "Timestamp: $TIMESTAMP"
echo "Nonce: $NONCE"
echo ""

# Calculate canonical string
METHOD="GET"
QUERY_STRING=""
BODY_HASH=""

CANONICAL_STRING="${METHOD}
${TARGET_PATH}
${QUERY_STRING}
${API_KEY}
${TIMESTAMP}
${NONCE}
${BODY_HASH}"

echo "Canonical String:"
echo "$CANONICAL_STRING" | cat -A
echo ""

# Calculate HMAC signature
SIGNATURE=$(echo -n "$CANONICAL_STRING" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

echo "Signature: $SIGNATURE"
echo ""
echo "========================================="
echo "Making Request..."
echo "========================================="

# Make request
HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/response.json \
  "${BASE_URL}${TARGET_PATH}" \
  -H "X-Api-Key: ${API_KEY}" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Nonce: ${NONCE}" \
  -H "X-Signature: ${SIGNATURE}")

echo "HTTP Status: $HTTP_CODE"
echo ""
echo "Response:"
cat /tmp/response.json | jq . 2>/dev/null || cat /tmp/response.json
echo ""

# Interpret result
echo ""
echo "========================================="
echo "Result Analysis"
echo "========================================="

case $HTTP_CODE in
  200)
    echo "✅ SUCCESS - Request authorized"
    ;;
  401)
    echo "❌ AUTHENTICATION FAILED"
    echo "Possible causes:"
    echo "  - Invalid API key"
    echo "  - Invalid signature"
    echo "  - Timestamp skew"
    echo "  - Agency suspended"
    ;;
  403)
    echo "❌ AUTHORIZATION FAILED"
    echo "Agency '$AGENCY_CODE' (owner of API key) tried to access path: $TARGET_PATH"
    echo "This indicates cross-agency access attempt was blocked ✅"
    ;;
  *)
    echo "❌ UNEXPECTED HTTP CODE: $HTTP_CODE"
    ;;
esac

echo ""
echo "========================================="
echo "Test Cross-Agency Access"
echo "========================================="

if [ "$AGENCY_CODE" = "AGENCY_A" ]; then
  CROSS_PATH="/AGENCY_B/transactions/received"
else
  CROSS_PATH="/AGENCY_A/transactions/received"
fi

echo "Attempting to access: $CROSS_PATH"

CROSS_CANONICAL="${METHOD}
${CROSS_PATH}
${QUERY_STRING}
${API_KEY}
${TIMESTAMP}
${NONCE}
${BODY_HASH}"

CROSS_SIGNATURE=$(echo -n "$CROSS_CANONICAL" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

CROSS_HTTP_CODE=$(curl -s -w "%{http_code}" -o /tmp/cross_response.json \
  "${BASE_URL}${CROSS_PATH}" \
  -H "X-Api-Key: ${API_KEY}" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Nonce: $(uuidgen || openssl rand -hex 16)" \
  -H "X-Signature: ${CROSS_SIGNATURE}")

echo "HTTP Status: $CROSS_HTTP_CODE"
echo ""
echo "Response:"
cat /tmp/cross_response.json | jq . 2>/dev/null || cat /tmp/cross_response.json
echo ""

if [ "$CROSS_HTTP_CODE" = "403" ]; then
  echo "✅ CORRECT - Cross-agency access blocked"
else
  echo "❌ SECURITY ISSUE - Cross-agency access should return 403"
fi
