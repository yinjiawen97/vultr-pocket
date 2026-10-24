#!/bin/bash
# 下载官方未修改的 mihomo Android ARM64 二进制（GPL-3.0），放到 lib/arm64-v8a/libmihomo.so。
# 源码：https://github.com/MetaCubeX/mihomo/tree/v1.19.33
set -euo pipefail
cd "$(dirname "$0")/.."
VERSION=v1.19.33
ASSET="mihomo-android-arm64-v8-$VERSION.gz"
SHA256=2ddbd3aaa884f52b4568f406876ec81dddeef9f1bb67b90d4004d356e2c6a708   # GitHub Release 公布的 .gz 摘要
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT
curl -fL --retry 3 -o "$TMP/$ASSET" "https://github.com/MetaCubeX/mihomo/releases/download/$VERSION/$ASSET"
echo "$SHA256  $TMP/$ASSET" | shasum -a 256 -c -
mkdir -p lib/arm64-v8a
gunzip -c "$TMP/$ASSET" > lib/arm64-v8a/libmihomo.so
chmod 755 lib/arm64-v8a/libmihomo.so
echo "已就绪：lib/arm64-v8a/libmihomo.so（$(shasum -a 256 lib/arm64-v8a/libmihomo.so | cut -c1-16)…）"
