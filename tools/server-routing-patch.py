# 在服务器上运行（root）：原地给 /var/www/clash-sub/config.yaml 加 Claude 专用分组、Tailscale 直连、Apple 默认直连。
# 节点和密码保持不变；写入前备份，写入后重新解析校验，失败自动还原。
import os, shutil, subprocess, sys, time
try:
    import yaml
except ImportError:
    subprocess.run(['apt-get', 'install', '-y', '-qq', 'python3-yaml'], check=True)
    import yaml
PATH = '/var/www/clash-sub/config.yaml'
CLAUDE = ['DOMAIN-SUFFIX,anthropic.com,Claude', 'DOMAIN-SUFFIX,claude.ai,Claude', 'DOMAIN-SUFFIX,claude.com,Claude',
          'DOMAIN-SUFFIX,claudeusercontent.com,Claude', 'DOMAIN-SUFFIX,clau.de,Claude',
          'IP-CIDR,160.79.104.0/23,Claude,no-resolve', 'IP-CIDR6,2607:6bc0::/48,Claude,no-resolve']
TAILNET = ['DOMAIN-SUFFIX,ts.net,DIRECT', 'IP-CIDR,100.64.0.0/10,DIRECT,no-resolve']

raw = open(PATH, encoding='utf-8').read()
c = yaml.safe_load(raw)
names = [p['name'] for p in c.get('proxies') or []]
if not names:
    sys.exit('订阅里没有节点，未修改')
if 'Claude' in names:
    sys.exit('节点名与 Claude 分组冲突，未修改')
rules = [('RULE-SET,anthropic,Claude' if r == 'RULE-SET,anthropic,AI' else r)
         for r in c.get('rules') or [] if r not in CLAUDE + TAILNET]
c['rules'] = TAILNET + CLAUDE + rules
groups = [g for g in c.get('proxy-groups') or [] if g.get('name') != 'Claude']
for g in groups:
    if g.get('name') == 'Apple':
        g['proxies'] = ['DIRECT'] + [p for p in g['proxies'] if p != 'DIRECT']
groups.append({'name': 'Claude', 'type': 'select', 'proxies': names})
c['proxy-groups'] = groups
dns = c.setdefault('dns', {})
dns['fake-ip-filter'] = [x for x in dns.get('fake-ip-filter') or [] if x != '+.ts.net'] + ['+.ts.net']
dns.setdefault('nameserver-policy', {})['+.ts.net'] = 'system'

st = os.stat(PATH)
backup = PATH + '.bak-' + time.strftime('%Y%m%d-%H%M%S')
shutil.copy2(PATH, backup)
tmp = PATH + '.new'
with open(tmp, 'w', encoding='utf-8') as f:
    yaml.safe_dump(c, f, allow_unicode=True, sort_keys=False)
check = yaml.safe_load(open(tmp, encoding='utf-8'))
if check.get('proxies') != yaml.safe_load(raw).get('proxies'):
    os.remove(tmp)
    sys.exit('校验失败：节点被改动，未写入')
os.chown(tmp, st.st_uid, st.st_gid)
os.chmod(tmp, st.st_mode & 0o777)
os.replace(tmp, PATH)
print('已更新', PATH, '（备份：' + backup + '）')
print('分组：', [g['name'] for g in c['proxy-groups']])
print('Claude 分组：', names, '｜ Apple 默认：', next((g['proxies'][0] for g in groups if g.get('name') == 'Apple'), '无'))
print('前 3 条规则：', c['rules'][:3])
