#!/bin/bash
# Claude 防直连（系统层）：用 macOS 自带 pf 拦截本机直接连 Anthropic IP 段。
# 走 Clash 代理时 Mac 只连 Vultr 服务器，不受影响；关掉代理/选了直连时 Claude 立即连不上。
# 用法：sudo bash install.sh     卸载：sudo bash uninstall.sh
set -euo pipefail
[ "$(id -u)" = 0 ] || { echo "请用 sudo 运行"; exit 1; }
ANCHOR=/etc/pf.anchors/claude-guard
PLIST=/Library/LaunchDaemons/com.yjw.claude-guard.plist
cat > "$ANCHOR" <<'RULES'
# Anthropic 官方 IP 段（claude.ai / api.anthropic.com / claude.com 等）
table <anthropic> const { 160.79.104.0/23, 2607:6bc0::/48 }
block return out quick proto { tcp udp } to <anthropic>
RULES
cat > "$PLIST" <<'PL'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
 <key>Label</key><string>com.yjw.claude-guard</string>
 <key>ProgramArguments</key><array>
  <string>/bin/sh</string><string>-c</string>
  <string>/sbin/pfctl -E; /sbin/pfctl -a com.apple/claude-guard -f /etc/pf.anchors/claude-guard</string>
 </array>
 <key>RunAtLoad</key><true/>
</dict></plist>
PL
chown root:wheel "$ANCHOR" "$PLIST"; chmod 644 "$ANCHOR" "$PLIST"
launchctl bootout system "$PLIST" 2>/dev/null || true
launchctl bootstrap system "$PLIST"
sleep 1
pfctl -a com.apple/claude-guard -sr
echo "已启用。开机自动生效。查看状态：sudo bash status.sh"
