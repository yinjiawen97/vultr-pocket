#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_TEST_CLASSES=$(mktemp -d /tmp/vultr-ui-tests.XXXXXX)
TASK_JSON_JAR="${VULTR_TEST_JSON_JAR:-/tmp/vultr-json-tests.jar}"
if [ ! -f "$TASK_JSON_JAR" ]; then
  echo '需要 JVM org.json 库，请设置 VULTR_TEST_JSON_JAR（仅测试依赖，不含密钥）。' >&2
  exit 1
fi
TASK_JAVA_BIN=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home/bin
"$TASK_JAVA_BIN/javac" -cp "$TASK_JSON_JAR:vendor/snakeyaml-2.4.jar" -d "$TASK_TEST_CLASSES" src/com/yjw/vultr/{Core,Inventory,HomeState,BootState,CloudState,CloudDiagnostics,IndicatorState,ServerIdentity,NavPolicy,AccountState,SubscriptionLinks,ServerPower,ServerLogin,Configs,RoutingUpdate,Refresher,Spend}.java tests/*Test.java
for task_test in Core Inventory Time HomeState BootState CloudState CloudDiagnostics IndicatorState ServerIdentity NavPolicy AccountState SubscriptionLinks ServerPower ServerLogin Configs RoutingUpdate Refresher Spend; do
 "$TASK_JAVA_BIN/java" -cp "$TASK_TEST_CLASSES:$TASK_JSON_JAR:vendor/snakeyaml-2.4.jar" "$([ "$task_test" = Inventory ] && echo com.yjw.vultr.InventoryTest || echo "${task_test}Test")"
done
python3 tests/bootstrap-login.py
node --test cloudflare/worker.test.mjs
