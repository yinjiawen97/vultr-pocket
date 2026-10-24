#!/bin/bash
# 把 Claude 专用分组 / Tailscale 直连 / Apple 默认直连 同步到正在运行的 Vultr 服务器订阅。
# 用法：bash sync-server-routing.sh <服务器IP>   （会先后要求输入 SSH 密码和 sudo 密码；用户名用 VULTR_SSH_USER 指定，默认 vultr）
set -euo pipefail
IP="${1:?用法：bash sync-server-routing.sh <服务器IP>}"
USER_NAME="${VULTR_SSH_USER:-vultr}"
B64=$(base64 < "$(dirname "$0")/server-routing-patch.py" | tr -d '\n')
ssh -t -o StrictHostKeyChecking=accept-new "$USER_NAME@$IP" \
  "echo '$B64' | base64 -d > /tmp/vp-routing.py && sudo python3 /tmp/vp-routing.py; rc=\$?; rm -f /tmp/vp-routing.py; exit \$rc"
echo
echo "完成。请在 Clash Verge / FlClash 里“更新”这条服务器订阅，新分流才会下发到客户端。"
