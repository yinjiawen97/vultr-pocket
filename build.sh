#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
# d8 needs JDK 11+; VULTR_JAVA_HOME overrides, then Homebrew openjdk, then JAVA_HOME.
BREW_JDK=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
if [ -n "${VULTR_JAVA_HOME:-}" ]; then export JAVA_HOME="$VULTR_JAVA_HOME"; elif [ -d "$BREW_JDK" ]; then export JAVA_HOME="$BREW_JDK"; fi
export PATH="$JAVA_HOME/bin:$PATH"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
[ -f lib/arm64-v8a/libmihomo.so ] || { echo "缺少 lib/arm64-v8a/libmihomo.so，请先运行 tools/fetch-mihomo.sh" >&2; exit 1; }
KS_PASS="${VULTR_KEYSTORE_PASS:-android}"
BT="$SDK/build-tools/35.0.0"
ANDROID="$SDK/platforms/android-35/android.jar"
rm -rf build/classes build/dex
mkdir -p build/classes build/dex dist signing
chmod 700 signing
"$BT/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$ANDROID" -F build/resources.apk
javac -cp vendor/snakeyaml-2.4.jar -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID:$BT/core-lambda-stubs.jar" -d build/classes src/com/yjw/vultr/*.java
jar cf build/classes.jar -C build/classes .
"$BT/d8" --lib "$ANDROID" --min-api 26 --output build/dex build/classes.jar vendor/snakeyaml-2.4.jar
cp build/resources.apk build/unsigned.apk
zip -q -r build/unsigned.apk lib
(cd build/dex && zip -q -j ../unsigned.apk classes*.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f signing/release.jks ]; then
  keytool -genkeypair -keystore signing/release.jks -storepass "$KS_PASS" -keypass "$KS_PASS" -alias vultr-pocket -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Vultr Pocket Personal'
  chmod 600 signing/release.jks
fi
"$BT/apksigner" sign --ks signing/release.jks --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" --out dist/Vultr-Pocket.apk build/aligned.apk
"$BT/apksigner" verify --verbose dist/Vultr-Pocket.apk
shasum -a 256 dist/Vultr-Pocket.apk > dist/SHA256.txt
