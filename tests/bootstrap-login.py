"""Execute the actual bootstrap login block with fake system operations only."""
import pathlib, tempfile, types, sys
from unittest.mock import patch
source=pathlib.Path('assets/bootstrap.sh').read_text()
python_block=source.split("python3 - <<'SERVER_PY'\n",1)[1].split('\nSERVER_PY',1)[0]
compile(python_block,'bootstrap embedded Python','exec')
block=python_block.split('# Password is passed on stdin, never as a command argument or printed.\n',1)[1].split("ip = str(ipaddress",1)[0]
with tempfile.TemporaryDirectory() as tmp:
 calls=[]
 class FakePath:
  def __init__(self, value): self.path=pathlib.Path(tmp)/'ssh.conf'
  def write_text(self,value):self.path.write_text(value)
  def chmod(self,value):self.path.chmod(value)
 def run(args,**kwargs):
  calls.append((args,kwargs))
  return types.SimpleNamespace(stdout='passwordauthentication yes\n')
 def absent(name): raise KeyError(name)
 with patch('pwd.getpwnam',absent):
  exec(block,{'payload':{'ssh_username':'vultr','ssh_password':'TestOnlyPassw0rd123'},'pathlib':types.SimpleNamespace(Path=FakePath),'subprocess':types.SimpleNamespace(run=run)})
 assert calls[0][0]==['useradd','-m','-s','/bin/bash','vultr']
 assert calls[1][0]==['usermod','-aG','sudo','vultr']
 assert calls[2][0]==['chpasswd'] and calls[2][1]['input']=='vultr:TestOnlyPassw0rd123\n'
 assert all('TestOnlyPassw0rd123' not in ' '.join(cmd) for cmd,_ in calls)
 assert (pathlib.Path(tmp)/'ssh.conf').read_text()=='Match User vultr\n    PasswordAuthentication yes\nMatch all\n'
 assert calls[-1][0]==['systemctl','reload-or-restart','ssh']
 calls.clear()
 with patch('pwd.getpwnam',lambda _:object()):
  exec(block,{'payload':{'ssh_username':'vultr','ssh_password':'TestOnlyPassw0rd123'},'pathlib':types.SimpleNamespace(Path=FakePath),'subprocess':types.SimpleNamespace(run=run)})
 assert not any(cmd[0]=='useradd' for cmd,_ in calls)
print('PASS: actual initialization block syntax, user creation, sudo group, stdin password, account-only SSH auth and validation/reload (simulated)')
