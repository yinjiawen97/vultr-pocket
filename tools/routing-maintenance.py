#!/usr/bin/env python3
"""Private fixed-subscription upgrade. Uses system curl with TLS verification.
No client configuration/VPN changes. Private responses never go to stdout.
"""
import argparse,copy,hashlib,hmac,json,os,pathlib,secrets,socket,subprocess,tempfile,time,urllib.parse,concurrent.futures
ROOT=pathlib.Path(__file__).resolve().parents[1]
PRIVATE=ROOT/'cloudflare/.private'; REPORT=ROOT/'qa/routing-report.json'
CORE='/Applications/Clash Verge.app/Contents/MacOS/verge-mihomo'
def request(url,method='GET',body=None,bearer=None,timeout=35):
 cfg=['url = '+json.dumps(url),'request = '+json.dumps(method),'silent','show-error','fail','max-time = '+str(timeout),'noproxy = "127.0.0.1,localhost"']
 if bearer:cfg.append('header = '+json.dumps('Authorization: Bearer '+bearer))
 if body is not None:cfg+=['header = "Content-Type: application/json"','data-binary = '+json.dumps(json.dumps(body,separators=(',',':')))]
 p=subprocess.run(['/usr/bin/curl','--config','-'],input='\n'.join(cfg)+'\n',text=True,capture_output=True)
 if p.returncode:raise RuntimeError('HTTPS request failed (curl code '+str(p.returncode)+')')
 return json.loads(p.stdout)
def settings():return json.loads((PRIVATE/'phone-setup.json').read_text())
def private_write(path,data):
 fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
 with os.fdopen(fd,'w') as f:json.dump(data,f,ensure_ascii=False,indent=2)
def encrypt_backup(data):
 keyfile=PRIVATE/'routing-backup.key'
 if not keyfile.exists():
  fd=os.open(keyfile,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
  with os.fdopen(fd,'w') as f:f.write(secrets.token_hex(32))
 raw=json.dumps(data,ensure_ascii=False).encode();key=keyfile.read_bytes()
 p=subprocess.run(['/usr/bin/openssl','enc','-aes-256-cbc','-salt','-pbkdf2','-iter','200000','-pass','file:'+str(keyfile)],input=raw,capture_output=True,check=True)
 stamp=str(time.time_ns());path=PRIVATE/('routing-backup-'+stamp+'.enc');path.write_bytes(p.stdout);path.chmod(0o600)
 signature=hmac.new(key,p.stdout,hashlib.sha256).hexdigest()
 private_write(PRIVATE/'routing-backup-index.json',{'file':path.name,'hmac':signature})
 return path.name
def read_backup():
 index=json.loads((PRIVATE/'routing-backup-index.json').read_text());raw=(PRIVATE/index['file']).read_bytes();keyfile=PRIVATE/'routing-backup.key'
 assert hmac.compare_digest(index['hmac'],hmac.new(keyfile.read_bytes(),raw,hashlib.sha256).hexdigest())
 p=subprocess.run(['/usr/bin/openssl','enc','-d','-aes-256-cbc','-pbkdf2','-iter','200000','-pass','file:'+str(keyfile)],input=raw,capture_output=True,check=True)
 return json.loads(p.stdout)
def apply(template,old):
 nodes=old['proxies'];names=[n['name'] for n in nodes]
 if not nodes or len(names)!=len(set(names)):raise RuntimeError('Missing or duplicate nodes')
 if set(names)&{'PROXY','AI','DEV','Google','YouTube','Microsoft','Apple','Claude','DIRECT','REJECT'}:raise RuntimeError('Node/group name conflict')
 new=copy.deepcopy(old)
 for k in ['dns','rule-providers','rules']:new[k]=copy.deepcopy(template[k])
 new['proxy-groups']=[{'name':g['name'],'type':'select','proxies':names if g['name']=='Claude' else ['DIRECT']+names if g['name']=='Apple' else names+['DIRECT']} for g in template['proxy-groups']]
 new['x-vultr-routing-version']=2
 new['mode']='rule'
 assert new['proxies']==old['proxies']
 return new
def freeport():
 with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
def launch(config,folder,broken=False):
 port=freeport();ctl=freeport();secret=secrets.token_hex(16);c=copy.deepcopy(config)
 for k in ['tun','listeners','port','socks-port','redir-port','tproxy-port','external-controller-tls','external-controller-unix','external-controller-pipe','external-ui','external-ui-url','external-ui-name','authentication']:c.pop(k,None)
 c.update({'mixed-port':port,'allow-lan':False,'bind-address':'127.0.0.1','external-controller':'127.0.0.1:'+str(ctl),'secret':secret,'log-level':'debug'})
 if broken:
  for p in c['rule-providers'].values():p['url']='http://127.0.0.1:1/missing';p['proxy']='DIRECT'
 path=folder/'config.json';private_write(path,c)
 log=open(folder/'core.log','w');os.chmod(folder/'core.log',0o600)
 p=subprocess.Popen([CORE,'-d',str(folder),'-f',str(path)],stdout=log,stderr=subprocess.STDOUT)
 return p,port,'http://127.0.0.1:'+str(ctl),secret,log
def stop(proc,log):
 proc.terminate()
 try:proc.wait(timeout=4)
 except subprocess.TimeoutExpired:proc.kill();proc.wait()
 log.close()
def ready(proc,base,secret,expected,seconds=95):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  if proc.poll() is not None:break
  try:
   state=request(base+'/providers/rules',bearer=secret,timeout=2)['providers']
   if len(state)==expected and all(v.get('ruleCount',0)>0 for v in state.values()):return state
  except Exception:pass
  time.sleep(.5)
 return None
def dial(port,domain):
 # SOCKS CONNECT only: no HTTP request, TLS session or account login.
 try:
  with socket.create_connection(('127.0.0.1',port),timeout=3) as s:
   s.settimeout(8);s.sendall(bytes([5,1,0]));s.recv(2);d=domain.encode('ascii');s.sendall(bytes([5,1,0,3,len(d)])+d+bytes([1,187]));s.recv(512)
 except OSError:pass

def validate(new):
 expected=len(new['rule-providers']);out={'node_count':len(new['proxies']),'ruleset_count':expected}
 with tempfile.TemporaryDirectory(prefix='vultr-routing-') as tmp:
  folder=pathlib.Path(tmp);test=copy.deepcopy(new);test['mixed-port']=0;test.pop('external-controller',None);test.pop('tun',None);private_write(folder/'syntax.json',test)
  p=subprocess.run([CORE,'-t','-d',tmp,'-f',str(folder/'syntax.json')],capture_output=True,timeout=60)
  if p.returncode:raise RuntimeError('Mihomo syntax validation failed; fixed subscription unchanged')
  out['mihomo_syntax']='PASS';print('Mihomo full configuration syntax PASS',flush=True)
  proc,port,base,secret,log=launch(new,folder)
  try:
   state=ready(proc,base,secret,expected)
   if state is None:
    text=(folder/'core.log').read_text(errors='replace')
    # Publish only sanitized counts/classification; no private log lines.
    (PRIVATE/'routing-validation-last.log').write_text(text);(PRIVATE/'routing-validation-last.log').chmod(0o600)
    print('Provider initialization incomplete; error lines:',sum('error' in line.lower() for line in text.splitlines()),flush=True)
    print('Network categories:',{k:text.lower().count(k) for k in ['timeout','connection refused','no such host','network is unreachable','certificate','404','unsupported','connection reset']},flush=True)
    raise RuntimeError('First rule download incomplete; fixed subscription unchanged')
   out['first_download']='PASS';out['rule_counts']={k:v['ruleCount'] for k,v in state.items()};print('All '+str(expected)+' MRS providers downloaded through configured proxy',flush=True)
   domains={'www.baidu.com':'DIRECT','www.taobao.com':'DIRECT','weixin.qq.com':'DIRECT','github.com':'DEV','api.githubcopilot.com':'DEV','api2.cursor.sh':'DEV','chatgpt.com':'AI','api.openai.com':'AI','claude.ai':'Claude','api.anthropic.com':'Claude','www.google.com':'Google','www.youtube.com':'YouTube','www.apple.com.cn':'DIRECT','apple.com':'Apple','www.microsoft.com':'Microsoft','www.microsoft.com.cn':'DIRECT'}
   with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:list(pool.map(lambda d:dial(port,d),domains))
   time.sleep(3);text=(folder/'core.log').read_text(errors='replace');matches={}
   for domain,wanted in domains.items():
    lines=[line for line in text.splitlines() if domain+':443' in line and ('match ' in line or 'using ' in line)]
    good=any(('using '+wanted+'[' in line or 'using '+wanted+' ' in line or 'using '+wanted+'"' in line or 'using '+wanted+'\n' in line or 'using '+wanted in line and wanted=='DIRECT') for line in lines)
    matches[domain]={'expected':wanted,'pass':good}
   out['domain_matches']=matches
   # DNS queries use the actual running resolver, with trace-derived outbound checks.
   dns={}
   for domain in ['www.baidu.com','www.google.com']:
    try:r=request(base+'/dns/query?'+urllib.parse.urlencode({'name':domain,'type':'A'}),bearer=secret,timeout=15);dns[domain]=bool(r.get('Answer'))
    except Exception:dns[domain]=False
   out['dns_queries']=dns
   out['proxy_exit_available']=False
   for host in ['api.ipify.org','checkip.amazonaws.com']:
    p=subprocess.run(['/usr/bin/curl','-sS','--noproxy','','--socks5-hostname','127.0.0.1:'+str(port),'--max-time','10','https://'+host],capture_output=True,text=True)
    if p.returncode==0 and p.stdout.strip():out['proxy_exit_available']=True;break
   observed={}
   try:
    connections=request(base+'/connections',bearer=secret,timeout=5)['connections']
    for conn in connections:
     metadata=conn.get('metadata',{});host=metadata.get('host','');ip=metadata.get('destinationIP','');chains=conn.get('chains',[])
     if host in ['dns.alidns.com','doh.pub'] or ip in ['1.1.1.1','8.8.8.8']:observed[host or ip]={'direct':'DIRECT' in chains,'proxy':'PROXY' in chains}
   except Exception:pass
   out['observed_dns_connections']=observed
   if not all(x['pass'] for x in matches.values()):
    print('Domain checks needing review:',[k for k,v in matches.items() if not v['pass']],flush=True)
    # Extract only known domain and rule labels, never complete logs.
    for domain,v in matches.items():
     if not v['pass']:
      import re
      lines=[line for line in text.splitlines() if domain+':443' in line]
      print(domain,[(re.findall(r'match ([^ ]+)',line),re.findall(r'using ([^ \[]+)',line)) for line in lines][:2],flush=True)
    raise RuntimeError('Domain routing checks incomplete; no publication')
  finally:stop(proc,log)
  out['domain_matching']='PASS';print('Domain route checks PASS',flush=True)
  proc,port,base,secret,log=launch(new,folder,broken=True)
  try:out['cached_rules_on_update_failure']='PASS' if ready(proc,base,secret,expected,12) else 'FAIL'
  finally:stop(proc,log)
  with tempfile.TemporaryDirectory(prefix='vultr-routing-empty-') as empty:
   proc,port,base,secret,log=launch(new,pathlib.Path(empty),broken=True)
   try:out['empty_cache_failure_detected']='PASS' if ready(proc,base,secret,expected,6) is None else 'FAIL'
   finally:stop(proc,log)
 if any(out.get(k)!='PASS' for k in ['cached_rules_on_update_failure','empty_cache_failure_detected']):raise RuntimeError('Rule cache checks failed')
 return out

def synchronize_server_credentials(before):
 # Read-only comparison with the server's real subscription; never persist the response.
 import base64,re
 key=pathlib.Path(os.environ.get('VULTR_API_KEY_FILE',str(pathlib.Path.home()/'code/vultr-clash/mac-state/api-key.txt'))).read_text().strip()
 instances=request('https://api.vultr.com/v2/instances',bearer=key)['instances'];source=copy.deepcopy(before);changed=0
 for node in source['proxies']:
  matches=[x for x in instances if x.get('main_ip')==node.get('server')]
  if not matches:continue
  u=request('https://api.vultr.com/v2/instances/'+matches[0]['id']+'/user-data',bearer=key)
  script=base64.b64decode(u['user_data']['data']).decode()
  match=re.search(r"payload\s*=\s*json.loads\(base64.b64decode\(['\"]([A-Za-z0-9+/=]+)",script)
  if not match:raise RuntimeError('Initialization format not recognized; no credential guessed')
  payload=json.loads(base64.b64decode(match.group(1)))
  url='http://'+node['server']+'/sub/'+payload['token']
  cfg='url = '+json.dumps(url)+'\nsilent\nshow-error\nfail\nmax-time = 25\nnoproxy = "*"\n'
  raw=subprocess.run(['/usr/bin/curl','--config','-'],input=cfg,text=True,capture_output=True,check=True).stdout
  ruby="require 'yaml';require 'json';puts JSON.generate(YAML.safe_load(STDIN.read, aliases: true))"
  decoded=subprocess.run(['/usr/bin/ruby','-e',ruby],input=raw,text=True,capture_output=True,check=True).stdout
  actual=json.loads(decoded);matching=[n for n in actual.get('proxies',[]) if n.get('server')==node['server'] and n.get('type')==node.get('type') and n.get('port')==node.get('port')]
  if len(matching)!=1 or matching[0].get('password')!=payload.get('password'):raise RuntimeError('Current server subscription could not be verified')
  real=matching[0];name=node['name'];changed+=int(any(real.get(k)!=node.get(k) for k in ['password','cipher']))
  node.update(real);node['name']=name
 if not changed:print('Server credentials already synchronized',flush=True)
 else:print('Verified current server credentials for '+str(changed)+' existing node(s); no server changes',flush=True)
 return source

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mode',choices=['validate','publish','restore','validate-current-server','publish-current-server']);args=parser.parse_args()
 cfg=settings()
 if args.mode=='restore':
  previous=read_backup();request(cfg['base'].rstrip('/')+'/api/config','PUT',{'config':previous},cfg['admin'])
  if request(cfg['url'])!=previous:raise RuntimeError('Restore readback not confirmed; backup retained')
  print('Original fixed subscription restored and verified',flush=True);return
 before=request(cfg['url']);template=json.loads((ROOT/'assets/config.yaml').read_text());source=synchronize_server_credentials(before) if args.mode.endswith('current-server') else before;new=apply(template,source)
 result=validate(new);result['version']='3.5.0';previous=json.loads(REPORT.read_text()) if REPORT.exists() else {};result['publication']=previous.get('publication','not attempted') if args.mode=='validate' else 'not attempted';result['limits']=['No client/VPN switch; no account logins; no server resource modifications']
 REPORT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 if args.mode in ['publish','publish-current-server']:
  if request(cfg['url'])!=before:raise RuntimeError('Fixed subscription changed during validation; retry with current data')
  backup=encrypt_backup(before);assert read_backup()==before
  print('Encrypted original subscription backup verified',flush=True)
  try:
   request(cfg['base'].rstrip('/')+'/api/config','PUT',{'config':new},cfg['admin'])
   ok=False
   for _ in range(6):
    if request(cfg['url'])==new:ok=True;break
    time.sleep(2)
   if not ok:raise RuntimeError('Published readback mismatch')
  except Exception:
   try:
    if request(cfg['url'])!=before:request(cfg['base'].rstrip('/')+'/api/config','PUT',{'config':before},cfg['admin'])
    if request(cfg['url'])!=before:raise RuntimeError()
    result['publication']='FAILED; original restored and verified'
   except Exception:result['publication']='FAILED; rollback unconfirmed, encrypted backup retained'
   REPORT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');raise RuntimeError(result['publication'])
  result['publication']='PASS: fixed URL and node endpoints preserved; current server credentials synchronized; new routing readback verified' if args.mode=='publish-current-server' else 'PASS: fixed URL and complete node list unchanged; new routing readback verified';result['backup']='Encrypted and authenticated local backup verified'
  REPORT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(result['publication'],flush=True)
if __name__=='__main__':
 try:main()
 except Exception as e:
  # No URL, credential, raw network response or traceback in terminal output.
  message=str(e)
  print('FAILED:',message if '://' not in message and len(message)<200 else type(e).__name__,flush=True);raise SystemExit(1)
