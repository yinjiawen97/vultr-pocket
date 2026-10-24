#!/bin/bash
set -uo pipefail
[ "$(id -u)" = 0 ] || { echo "请用 sudo 运行"; exit 1; }
PLIST=/Library/LaunchDaemons/com.yjw.claude-guard.plist
launchctl bootout system "$PLIST" 2>/dev/null
pfctl -a com.apple/claude-guard -F all 2>/dev/null
rm -f "$PLIST" /etc/pf.anchors/claude-guard
echo "已移除 Claude 防直连（pf 本身保持系统原状）。"
