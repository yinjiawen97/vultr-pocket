#!/usr/bin/env python3
"""Capture only the separate offline preview; never screenshots the production app."""
import subprocess,xml.etree.ElementTree as E,json,re
from pathlib import Path
import os;b=['adb']  # set ANDROID_SERIAL to pick a device;out=Path(__file__).resolve().parent/'screens';out.mkdir(exist_ok=True)
def run(args):return subprocess.run(b+args,capture_output=True,check=True).stdout
def capture(name,screen,dark=False,**extras):
 args=['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--es','screen',screen,'--ez','dark',str(dark).lower(),'--ei','width','360']
 for k,v in extras.items():args+=['--ez' if isinstance(v,bool) else '--ef' if isinstance(v,float) else '--ei',k,str(v).lower()]
 run(args);run(['shell','uiautomator','dump','/data/local/tmp/vultr-qa.xml']);raw=run(['exec-out','cat','/data/local/tmp/vultr-qa.xml']);root=E.fromstring(raw)
 nodes=[n for n in root.iter('node') if n.get('package')=='com.yjw.vultr.preview']
 if not nodes:raise RuntimeError('预览未在前台；可能锁屏，停止截图')
 texts=[n.get('text','') for n in nodes if not n.get('class','').endswith('EditText')]
 assert all(t in texts for t in ['总览','服务器','订阅','费用','设置']), '五项导航缺失'
 (out/(name+'.png')).write_bytes(run(['exec-out','screencap','-p']+(['-d',os.environ['QA_DISPLAY']] if os.environ.get('QA_DISPLAY') else [])));(out/(name+'.xml')).write_bytes(raw)
 print(name,'PASS',flush=True);return {'name':name,'visible_text':texts,'bounds':[n.get('bounds') for n in nodes if n.get('clickable')=='true']}
if __name__=='__main__':
 reports=[]
 for dark in [False,True]:
  for i,screen in enumerate(['总览','服务器','订阅','费用','设置']):reports.append(capture(('dark-' if dark else 'light-')+str(i),screen,dark))
 for name,screen,opts in [('large-text','设置',{'font':2.0}),('long-multiple','服务器',{'long':True,'multi':True}),('empty','总览',{'empty':True}),('stale','总览',{'stale':True}),('failed','总览',{'failed':True}),('details','详情',{}),('cloud','Cloudflare',{}),('stock','库存',{}),('race','总览',{'race':True}),('landscape','总览',{'landscape':True})]:
  reports.append(capture(name,screen,**opts))
  if name=='race':assert 'STALE_CALLBACK_ERROR' not in reports[-1]['visible_text'] and '账户连接' in reports[-1]['visible_text']
 (out/'report.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2))
