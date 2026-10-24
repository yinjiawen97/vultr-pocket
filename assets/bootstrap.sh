#!/bin/bash
set -euo pipefail
umask 077
trap 'echo "Bootstrap failed at line $LINENO; inspect /var/log/cloud-init-output.log" >&2' ERR
[ "$(id -u)" = 0 ] || exit 1
# Install this Mac's SSH public key first, so a failed install below can still be inspected.
SSH_KEY_B64=''
if [ -n "$SSH_KEY_B64" ]; then
  install -d -m 700 /root/.ssh
  echo "$SSH_KEY_B64" | base64 -d >> /root/.ssh/authorized_keys
  chmod 600 /root/.ssh/authorized_keys
fi
# This installer is for a NEW instance only; never overwrite an existing SS installation.
if [ -e /etc/shadowsocks-libev/config.json ] || [ -e /etc/clash-sub ]; then
  echo 'Existing Shadowsocks/config detected; refusing to overwrite.' >&2
  exit 1
fi
export DEBIAN_FRONTEND=noninteractive
apt-get -o Acquire::Retries=3 update -qq
apt-get -o Acquire::Retries=3 install -y shadowsocks-libev nginx curl python3 sudo openssh-server
SERVER_IP=$(curl --noproxy '*' -4fsS --retry 4 --max-time 20 https://api.ipify.org)
export SERVER_IP
python3 - <<'SERVER_PY'
import base64, grp, ipaddress, json, os, pathlib, subprocess
payload = json.loads(base64.b64decode('__PAYLOAD__'))
# Password is passed on stdin, never as a command argument or printed.
import pwd
login = payload['ssh_username']
import re
if not re.fullmatch(r'[a-z_][a-z0-9_-]{0,31}', login) or login == 'root' or not re.fullmatch(r'[A-Za-z0-9]{16,64}', payload['ssh_password']):
    raise ValueError('Invalid configured SSH login')
try:
    pwd.getpwnam(login)
except KeyError:
    subprocess.run(['useradd', '-m', '-s', '/bin/bash', login], check=True)
subprocess.run(['usermod', '-aG', 'sudo', login], check=True)
subprocess.run(['chpasswd'], input=login + ':' + payload['ssh_password'] + '\n', text=True, check=True)
# Restrict the password-authentication override to the requested account.
ssh_config = pathlib.Path('/etc/ssh/sshd_config.d/00-vultr-pocket-login.conf')
ssh_config.write_text('Match User ' + login + '\n    PasswordAuthentication yes\nMatch all\n')
ssh_config.chmod(0o600)
subprocess.run(['/usr/sbin/sshd', '-t'], check=True)
effective = subprocess.run(['/usr/sbin/sshd', '-T', '-C', 'user=' + login + ',host=localhost,addr=127.0.0.1'], check=True, capture_output=True, text=True).stdout
if 'passwordauthentication yes' not in effective.splitlines():
    raise RuntimeError('SSH password authentication was not enabled')
subprocess.run(['systemctl', 'reload-or-restart', 'ssh'], check=True)
ip = str(ipaddress.IPv4Address(os.environ['SERVER_IP']))
folder = pathlib.Path('/etc/clash-sub')
folder.mkdir(mode=0o750)
folder.chmod(0o750)
os.chown(folder, 0, grp.getgrnam('nogroup').gr_gid)
ss = folder / 'shadowsocks.json'
ss.write_text(json.dumps(dict(server='0.0.0.0', server_port=8388, password=payload['password'],
                             method='chacha20-ietf-poly1305', mode='tcp_and_udp', timeout=300)))
os.chown(ss, 0, grp.getgrnam('nogroup').gr_gid)
ss.chmod(0o640)
www = pathlib.Path('/var/www/clash-sub')
www.mkdir(mode=0o750)
www.chmod(0o750)
os.chown(www, 0, grp.getgrnam('www-data').gr_gid)
config = www / 'config.yaml'
config.write_text(payload['yaml'].replace('REPLACE_SERVER_IP', ip))
os.chown(config, 0, grp.getgrnam('www-data').gr_gid)
config.chmod(0o640)
nginx = pathlib.Path('/etc/nginx/sites-available/clash-sub')
nginx.write_text('server {\n listen 80;\n server_name ' + ip + ';\n'
 ' location = /sub/' + payload['token'] + ' {\n'
 '  alias /var/www/clash-sub/config.yaml;\n  default_type text/yaml;\n'
 '  access_log off;\n  add_header Cache-Control "no-store";\n'
 '  add_header profile-update-interval "24";\n }\n location / { return 404; }\n}\n')
nginx.chmod(0o644)
pathlib.Path('/root/clash-subscription-url.txt').write_text('http://' + ip + '/sub/' + payload['token'] + '\n')
unit = pathlib.Path('/etc/systemd/system/clash-ss.service')
unit.write_text('[Unit]\nDescription=Clash subscription Shadowsocks server\nAfter=network-online.target\n'
 '[Service]\nType=simple\nUser=nobody\nGroup=nogroup\n'
 'ExecStart=/usr/bin/ss-server -c /etc/clash-sub/shadowsocks.json\nRestart=on-failure\n'
 'NoNewPrivileges=true\nPrivateTmp=true\nProtectHome=true\nProtectSystem=strict\n'
 '[Install]\nWantedBy=multi-user.target\n')
unit.chmod(0o644)
SERVER_PY
ln -s /etc/nginx/sites-available/clash-sub /etc/nginx/sites-enabled/clash-sub
nginx -t
# Only the distribution's newly installed default service is replaced on this fresh instance.
systemctl disable --now shadowsocks-libev
systemctl daemon-reload
systemctl enable --now clash-ss
systemctl enable nginx
systemctl reload-or-restart nginx
# Respect an already enabled local firewall; do not enable it or change SSH access.
if command -v ufw >/dev/null 2>&1 && ufw status | grep -q '^Status: active'; then
  ufw allow 80/tcp
  ufw allow 8388/tcp
  ufw allow 8388/udp
fi
sleep 2
systemctl is-active --quiet clash-ss
systemctl is-active --quiet nginx
curl --noproxy '*' -fsS -H "Host: $SERVER_IP" "$(cat /root/clash-subscription-url.txt | sed "s|http://$SERVER_IP/|http://127.0.0.1/|")" -o /run/clash-check.yaml
cmp /run/clash-check.yaml /var/www/clash-sub/config.yaml
rm -f /run/clash-check.yaml
touch /etc/clash-sub/ready
echo 'CLASH_DEPLOY_SUCCESS'
