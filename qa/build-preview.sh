#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
TASK_QA_DIR=$(mktemp -d /tmp/vultr-preview.XXXXXX)
cp -R src res assets vendor lib AndroidManifest.xml build.sh "$TASK_QA_DIR/"
if [ -d build/qa-signing ]; then cp -R build/qa-signing "$TASK_QA_DIR/signing"; fi
cp qa/QaActivity.java "$TASK_QA_DIR/src/com/yjw/vultr/"
python3 - "$TASK_QA_DIR/AndroidManifest.xml" <<'PY'
from pathlib import Path
import sys,re
p=Path(sys.argv[1]);s=p.read_text().replace('package="com.yjw.vultr"','package="com.yjw.vultr.preview"').replace('android:label="Vultr 管家"','android:label="Vultr 布局预览"').replace('android:name=".MainActivity"','android:name="com.yjw.vultr.QaActivity" android:screenOrientation="portrait"').replace('<uses-permission android:name="android.permission.INTERNET"/>','')
s=re.sub(r'<provider[^>]*/>','',s);s=re.sub(r'<service[^>]*/>','',s);p.write_text(s)
PY
bash "$TASK_QA_DIR/build.sh" > "$TASK_QA_DIR/build.log" 2>&1
if [ ! -d build/qa-signing ]; then cp -R "$TASK_QA_DIR/signing" build/qa-signing; fi
cp "$TASK_QA_DIR/dist/Vultr-Pocket.apk" build/Vultr-Preview.apk
printf '%s\n' 'Offline-only preview APK ready (separate app, fictional data, no Internet permission).'
