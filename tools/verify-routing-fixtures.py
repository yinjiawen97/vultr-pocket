#!/usr/bin/env python3
"""Real Mihomo + upstream MRS, fictional node, no account login or TLS request."""
import importlib.util,pathlib,tempfile,json,subprocess,concurrent.futures,time,re
spec=importlib.util.spec_from_file_location('routing',pathlib.Path(__file__).with_name('routing-maintenance.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
c=json.loads((m.ROOT/'assets/config.yaml').read_text());c['proxies']=[{'name':'Fixture','type':'ss','server':'127.0.0.1','port':1,'cipher':'chacha20-ietf-poly1305','password':'fixture-only'}]
for g in c['proxy-groups']:g['proxies']=['REJECT']
domains={'www.baidu.com':'DIRECT','www.taobao.com':'DIRECT','weixin.qq.com':'DIRECT','github.com':'DEV','api.githubcopilot.com':'DEV','api2.cursor.sh':'DEV','chatgpt.com':'AI','api.openai.com':'AI','claude.ai':'AI','api.anthropic.com':'AI','www.google.com':'Google','www.youtube.com':'YouTube','www.apple.com.cn':'DIRECT','apple.com':'Apple','www.microsoft.com':'Microsoft','www.microsoft.com.cn':'DIRECT'}
c['hosts']={d:'192.0.2.1' for d in domains};c['dns']['use-hosts']=True
with tempfile.TemporaryDirectory(prefix='vultr-routing-fixtures-') as tmp:
 folder=pathlib.Path(tmp)
 def fetch(item):
  name,p=item;path=folder/p['path'];path.parent.mkdir(parents=True,exist_ok=True)
  subprocess.run(['/usr/bin/curl','-fsSL','--max-time','45','--retry','2',p['url'],'-o',str(path)],check=True,capture_output=True)
 with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:list(pool.map(fetch,c['rule-providers'].items()))
 proc,port,base,secret,log=m.launch(c,folder,broken=True)
 try:
  state=m.ready(proc,base,secret,len(c['rule-providers']),15);assert state,'Cached upstream MRS did not load'
  with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:list(pool.map(lambda d:m.dial(port,d),domains))
  time.sleep(12);text=(folder/'core.log').read_text(errors='replace');(m.ROOT/'build/routing-fixture.log').write_text(text);results={}
  for domain,expected in domains.items():
   lines=[line for line in text.splitlines() if domain+':443' in line]
   policies=[]
   for line in lines:policies+=re.findall(r'(?:using |dial )([A-Za-z]+)(?:\[| \(match)',line)
   results[domain]={'expected':expected,'observed':sorted(set(policies)),'pass':expected in policies}
  report={'test_type':'Real Mihomo, upstream MRS cached via system curl, fictional node/REJECT groups, documentation IP hosts; no account login','provider_counts':{k:v['ruleCount'] for k,v in state.items()},'matching':results,'cached_rules_on_update_failure':'PASS','limits':'Does not establish live node connectivity or actual DNS exit'}
  (m.ROOT/'qa/routing-fixture-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(results,ensure_ascii=False,indent=2))
  assert all(x['pass'] for x in results.values()),'Rule matches need review'
 finally:m.stop(proc,log)
 with tempfile.TemporaryDirectory(prefix='vultr-routing-fresh-') as fresh:
  proc,port,base,secret,log=m.launch(c,pathlib.Path(fresh),broken=True)
  try:assert m.ready(proc,base,secret,len(c['rule-providers']),6) is None
  finally:m.stop(proc,log)
 print('PASS: real Mihomo domain classification, cached rules on failed updates, empty-cache failure detection')
