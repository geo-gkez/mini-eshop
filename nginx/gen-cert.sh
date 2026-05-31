#!/bin/sh
# Generates a self-signed TLS certificate for nginx.
# Run once before `docker compose up`. Requires OpenSSL 1.1.1+.
#
# Output: nginx/certs/server.crt + server.key  (both git-ignored)
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CERT_DIR="$SCRIPT_DIR/certs"

if [ -f "$CERT_DIR/server.crt" ] && [ -f "$CERT_DIR/server.key" ]; then
    echo "Certificates already exist in $CERT_DIR — delete them first to regenerate."
    exit 0
fi

openssl req -x509 \
    -newkey rsa:4096 \
    -keyout "$CERT_DIR/server.key" \
    -out    "$CERT_DIR/server.crt" \
    -sha256 -days 365 -nodes \
    -subj   "/CN=mini-eshop.local" \
    -addext "subjectAltName=DNS:mini-eshop.local,DNS:localhost,IP:127.0.0.1"

echo ""
echo "Certificate generated in $CERT_DIR (valid 365 days)."
echo ""
echo "If mini-eshop.local is not in /etc/hosts, add it:"
echo "  echo '127.0.0.1  mini-eshop.local' | sudo tee -a /etc/hosts"
echo ""
echo "Browsers will show an untrusted-cert warning — expected for a self-signed cert."
echo "In Chrome: Advanced → Proceed to mini-eshop.local (unsafe)."
