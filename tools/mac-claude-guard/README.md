# Mac 上的 Claude 防直连

两层保护，目的：Claude / Anthropic 只能经 Vultr 代理访问，任何直连都失败。

## 1. Clash 层（已配置）

Clash Verge「全局扩展脚本」`Script.js` 增加 `Claude` 分组：只包含代理节点，没有 DIRECT；没有节点时为 REJECT。
claude.ai、claude.com、anthropic.com、claudeusercontent.com、clau.de 及 Anthropic IP 段的规则放在最前面。
对所有订阅生效。原脚本备份为 `profiles/Script.js.before-claude-guard`。

生效方式：在 Clash Verge 的「订阅」里重新点一下当前订阅（或重启 Clash Verge）。

## 2. 系统层（需要你执行）

用 macOS 自带 pf 拦截本机发往 Anthropic IP 段（`160.79.104.0/23`、`2607:6bc0::/48`）的连接。
走代理时 Mac 只连 Vultr 服务器，不受影响；关闭系统代理、退出 Clash 或选了直连时，Claude 立即连不上。
规则放在系统已有的 `com.apple/*` 锚点下，不修改 `/etc/pf.conf`；开机自动加载。

```bash
sudo bash install.sh     # 启用
sudo bash status.sh      # 查看：直连应显示“被拦截 ✓”，经 Clash 代理应返回 HTTP 状态码
sudo bash uninstall.sh   # 移除
```

限制：Tailscale 出口节点等其他 VPN 也会被拦（它们连的同样是 Anthropic IP）。若 Anthropic 将来新增 IP 段，需要更新 `install.sh` 里的 table。
