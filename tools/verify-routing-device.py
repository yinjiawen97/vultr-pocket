#!/usr/bin/env python3
"""Isolated phone-network validation, no client/VPN modification or cloud writes."""
import importlib.util,pathlib,tempfile,json,subprocess,time,secrets,concurrent.futures
spec=importlib.util.spec_from_file_location('routing',pathlib.Path(__file__).with_name('routing-maintenance.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
ADB=['adb'];DIR='/data/local/tmp/vultr-routing-live';forwards=[];pid=None

def adb(args):return subprocess.run(ADB+args,capture_output=True,check=True).stdout.decode().strip()
c=m.apply(json.loads((m.ROOT/'assets/config.yaml').read_text()),m.request(m.settings()['url']));secret=secrets.token_hex(16);c.update({'mixed-port':17789,'external-controller':'127.0.0.1:17790','secret':secret,'allow-lan':False,'bind-address':'127.0.0.1','log-level':'debug'})
for k in ['tun','listeners','port','socks-port','redir-port','tproxy-port','external-controller-tls','external-controller-unix','external-ui','external-ui-url','external-ui-name']:c.pop(k,None)
report={'type':'Isolated phone core, no VPN/account/client changes'}
try:
 adb(['shell','mkdir','-p',DIR]);adb(['shell','chmod','700',DIR])
 with tempfile.TemporaryDirectory() as tmp:
  path=pathlib.Path(tmp)/'config.json';m.private_write(path,c);adb(['push',str(path),DIR+'/config.json'])
 adb(['shell','chmod','600',DIR+'/config.json'])
 pid=adb(['shell','sh','-c',"'/data/local/tmp/vultr-routing-core -d /data/local/tmp/vultr-routing-live -f /data/local/tmp/vultr-routing-live/config.json >/data/local/tmp/vultr-routing-live/core.log 2>&1 & echo $!'"])
 if not pid.isdigit():raise RuntimeError('No isolated core PID')
 for remote in [17789,17790]:forwards.append(int(adb(['forward','tcp:0','tcp:'+str(remote)])))
 port,ctl=forwards;end=time.monotonic()+95;state=None
 while time.monotonic()<end:
  try:
   s=m.request('http://127.0.0.1:'+str(ctl)+'/providers/rules',bearer=secret,timeout=2)['providers']
   if len(s)==15 and all(v.get('ruleCount',0)>0 for v in s.values()):state=s;break
  except Exception:pass
  time.sleep(.7)
 report['all_providers_downloaded']=state is not None
 if state:report['provider_counts']={k:v['ruleCount'] for k,v in state.items()}
 p=subprocess.run(['/usr/bin/curl','-sS','--noproxy','','--socks5-hostname','127.0.0.1:'+str(port),'--max-time','15','https://api.ipify.org'],capture_output=True,text=True)
 report['proxy_exit_available']=p.returncode==0 and bool(p.stdout.strip());report['exit_curl_code']=p.returncode
 dns={}
 for name in ['www.baidu.com','www.google.com']:
  try:r=m.request('http://127.0.0.1:'+str(ctl)+'/dns/query?name='+name+'&type=A',bearer=secret,timeout=15);dns[name]=bool(r.get('Answer'))
  except Exception:dns[name]=False
 report['dns_queries']=dns
 # These are DNS outcomes; do not claim observed egress without connection evidence.
 try:
  connections=m.request('http://127.0.0.1:'+str(ctl)+'/connections',bearer=secret,timeout=5)['connections'];observed={}
  for conn in connections:
   metadata=conn.get('metadata',{});host=metadata.get('host','');ip=metadata.get('destinationIP','');chains=conn.get('chains',[])
   if host in ['dns.alidns.com','doh.pub'] or ip in ['1.1.1.1','8.8.8.8']:observed[host or ip]={'direct':'DIRECT' in chains,'proxy':'PROXY' in chains}
  report['observed_dns_connections']=observed
 except Exception:report['observed_dns_connections']={}
 print(json.dumps(report,ensure_ascii=False,indent=2),flush=True)
 (m.ROOT/'qa/routing-device-network.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
finally:
 if pid and pid.isdigit():
  try:adb(['shell','kill',pid])
  except Exception:pass
 for port in forwards:
  try:adb(['forward','--remove','tcp:'+str(port)])
  except Exception:pass
 adb(['shell','rm','-rf',DIR])
