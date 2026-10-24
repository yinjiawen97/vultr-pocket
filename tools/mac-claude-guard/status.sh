#!/bin/bash
# 查看规则是否加载，并实测直连/代理两种情况
[ "$(id -u)" = 0 ] && { pfctl -s info 2>/dev/null | head -1; echo "规则："; pfctl -a com.apple/claude-guard -sr 2>/dev/null; }
printf "直连 api.anthropic.com：" ; curl -s --noproxy '*' -m 5 -o /dev/null -w "%{http_code}\n" https://api.anthropic.com || echo "被拦截 ✓"
printf "经 Clash 代理：" ; curl -s -x http://127.0.0.1:7897 -m 10 -o /dev/null -w "%{http_code}\n" https://api.anthropic.com || echo "失败（检查 Clash 与 Vultr 节点）"
