#!/usr/bin/env bash
# Build release APK firmati + esporta le chiavi per firme future.
# Uso: bash scripts/build-release.sh
set -euo pipefail

cd "$(dirname "$0")/.."

image=dosbox-plus-build
volume=dosbox-plus-release-signing
mkdir -p dist
dist_dir="$(realpath dist)"

docker build --target build -t "$image" .
docker volume create "$volume" >/dev/null
docker run --rm \
    --mount "type=volume,source=$volume,target=/signing" \
    --mount "type=bind,source=$dist_dir,target=/dist" \
    "$image" bash -euo pipefail -c '
        set -euo pipefail
        KEYSTORE=/signing/dosbox-plus-release.jks
        STORE_PASS_FILE=/signing/store.password
        KEY_PASS_FILE=/signing/key.password
        ALIAS_FILE=/signing/key.alias
        CERT_FILE=/signing/dosbox-plus-release-cert.pem

        if [ ! -f "$KEYSTORE" ]; then
            echo "Genero nuovo keystore release nel volume di firma..."
            openssl rand -base64 24 | tr -d "\n" > "$STORE_PASS_FILE"
            openssl rand -base64 24 | tr -d "\n" > "$KEY_PASS_FILE"
            echo -n "dosbox-plus" > "$ALIAS_FILE"
            chmod 600 "$STORE_PASS_FILE" "$KEY_PASS_FILE"
            keytool -genkeypair \
                -keystore "$KEYSTORE" \
                -storetype JKS \
                -alias "$(cat $ALIAS_FILE)" \
                -keyalg RSA -keysize 3072 -validity 10950 \
                -storepass "$(cat $STORE_PASS_FILE)" \
                -keypass "$(cat $KEY_PASS_FILE)" \
                -dname "CN=DOSBox Plus, OU=Release, O=DOSBox Plus, L=Unknown, ST=Unknown, C=IT"
            keytool -exportcert -rfc \
                -keystore "$KEYSTORE" \
                -alias "$(cat $ALIAS_FILE)" \
                -storepass "$(cat $STORE_PASS_FILE)" \
                -file "$CERT_FILE"
            chmod 600 "$KEYSTORE"
            echo "Keystore creato. CONSERVA il volume e la copia esportata in dist/release-keys/."
        else
            echo "Riutilizzo keystore esistente nel volume."
            # Rigenera il certificato pubblico se mancante (serve per verifica/backup).
            if [ ! -f "$CERT_FILE" ]; then
                keytool -exportcert -rfc \
                    -keystore "$KEYSTORE" \
                    -alias "$(cat $ALIAS_FILE)" \
                    -storepass "$(cat $STORE_PASS_FILE)" \
                    -file "$CERT_FILE"
            fi
        fi

        export RELEASE_STORE_FILE="$KEYSTORE"
        export RELEASE_STORE_PASSWORD="$(cat $STORE_PASS_FILE)"
        export RELEASE_KEY_ALIAS="$(cat $ALIAS_FILE)"
        export RELEASE_KEY_PASSWORD="$(cat $KEY_PASS_FILE)"

        gradle --no-daemon --console=plain assembleRelease
        cp app/build/outputs/apk/release/*.apk /dist/

        # Esporta chiavi per firme future: keystore + cert + password + istruzioni.
        mkdir -p /dist/release-keys
        cp "$KEYSTORE" /dist/release-keys/dosbox-plus-release.jks
        cp "$CERT_FILE" /dist/release-keys/dosbox-plus-release-cert.pem
        cp "$STORE_PASS_FILE" /dist/release-keys/store.password
        cp "$KEY_PASS_FILE" /dist/release-keys/key.password
        cp "$ALIAS_FILE" /dist/release-keys/key.alias
        chmod 600 /dist/release-keys/dosbox-plus-release.jks \
            /dist/release-keys/store.password /dist/release-keys/key.password
        ( cd /dist && sha256sum app-*-release.apk > release-keys/release-apks.sha256 )
        # Rende i file leggibili per lo host (il bind-mount crea file owned root).
        chown --reference=/dist -R /dist/app-*-release.apk /dist/release-keys || true
        cat > /dist/release-keys/README.txt <<EOF
DOSBox Plus - chiavi di firma release
=====================================
- dosbox-plus-release.jks : keystore da conservare (SEGRETO, non committare).
- store.password / key.password / key.alias : password e alias (SEGRETI).
- dosbox-plus-release-cert.pem : certificato pubblico (condivisibile).
- release-apks.sha256 : checksum degli APK appena compilati.

Per firmare di nuovo una futura build release, riesegui:
  bash scripts/build-release.sh
Lo script riusa il volume Docker dosbox-plus-release-signing.
Se ricostruisci da zero (nuova macchina), reimporta prima il backup:
  docker volume create dosbox-plus-release-signing
  docker run --rm --mount type=volume,source=dosbox-plus-release-signing,target=/signing \\
    --mount type=bind,source=\$(realpath release-keys-backup),target=/backup \\
    sh -c "cp /backup/dosbox-plus-release.jks /signing/ && cp /backup/store.password /signing/ && cp /backup/key.password /signing/ && cp /backup/key.alias /signing/ && chmod 600 /signing/dosbox-plus-release.jks /signing/*.password"
In alternativa firma manuale con apksigner:
  apksigner sign --ks dosbox-plus-release.jks --ks-pass:file store.password --key-pass:file key.password --out app-release-signed.apk app-release-unsigned.apk
EOF
        echo "--- APK release ---"
        ls -l /dist/*.apk
        echo "--- Chiavi esportate ---"
        ls -l /dist/release-keys/
    '

echo "Backup host: dist/release-keys/ (conserva una copia sicura fuori da git)."
