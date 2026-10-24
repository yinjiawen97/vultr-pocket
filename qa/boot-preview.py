"""Uses fictional records in the separate offline preview package only."""
import subprocess,xml.etree.ElementTree as E,re,shlex,json
from pathlib import Path
b=['adb'];out=Path(__file__).resolve().parent/'screens';report=[]
def r(a):return subprocess.run(b+a,capture_output=True,check=True).stdout
def dump():
 r(['shell','uiautomator','dump','/data/local/tmp/vultr-boot-qa.xml']);root=E.fromstring(r(['exec-out','cat','/data/local/tmp/vultr-boot-qa.xml']));assert any(n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node')),'Preview not foreground';return root
cases=[('creating','正在创建 · 等待开机',{'boot':'creating'}),('booting','正在开机 / 初始化',{'boot':'booting'}),('pulse','正在开机 / 初始化',{'boot':'booting','style':'pulse'}),('dark','正在开机 / 初始化',{'boot':'booting','dark':True}),('static','正在开机 / 初始化',{'boot':'booting','motion':False}),('reduced','正在开机 / 初始化',{'boot':'booting','reduced':True}),('running','已开机 · 订阅待核验',{'boot':'running'}),('stopped','已关机',{'boot':'stopped'}),('stale','等待更新',{'boot':'booting','stale':True}),('failed','等待更新',{'boot':'booting','failed':True}),('uncertain','请求结果待核对',{'boot':'uncertain'}),('large','正在开机 / 初始化',{'boot':'booting','font':2.0,'long':True})]
for name,expected,extras in cases:
 args=['am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--ei','width','360'];values={'screen':'详情','style':'rack','motion':True,**extras}
 for key,value in values.items():args+=['--ez' if isinstance(value,bool) else '--ef' if isinstance(value,float) else '--es',key,str(value).lower() if isinstance(value,bool) else str(value)]
 r(['shell',shlex.join(args)])
 for i in range(12):
  root=dump();matches=[n for n in root.iter('node') if n.get('text')==expected]
  if matches:break
  r(['shell','input','swipe','550','1800','550','1450','600'])
 else:raise AssertionError(name+' state not found')
 texts=[n.get('text','') for n in root.iter('node')]
 assert not any('STALE_CALLBACK_ERROR' in s for s in texts)
 raw=r(['exec-out','screencap','-p']);offset=raw.find(b'\x89PNG\r\n\x1a\n');assert 0<=offset<2048,'Invalid ADB screenshot stream'
 (out/('boot-'+name+'.png')).write_bytes(raw[offset:])
 report.append({'case':name,'expected':expected,'result':'PASS'});print('PASS:',name,flush=True)
(out/'boot-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
