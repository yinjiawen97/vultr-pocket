"""Only operates on the separate, offline, fictional-data preview application."""
import subprocess,xml.etree.ElementTree as E,re,json
from pathlib import Path
b=['adb'];out=Path(__file__).resolve().parent/'screens';out.mkdir(exist_ok=True)
def run(args):return subprocess.run(b+args,capture_output=True,check=True).stdout
def dump():
 run(['shell','uiautomator','dump','/data/local/tmp/vultr-indicator-test.xml']);root=E.fromstring(run(['exec-out','cat','/data/local/tmp/vultr-indicator-test.xml']));assert any(n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node')),'Preview not foreground (possibly locked)';return root

def launch(**extras):
 args=['shell','am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--ei','width','360']
 for key,value in extras.items():args+=['--ez' if isinstance(value,bool) else '--ef' if isinstance(value,float) else '--es',key,str(value).lower() if isinstance(value,bool) else str(value)]
 run(args);return dump()
def tap(text):
 n=next(n for n in dump().iter('node') if n.get('package')=='com.yjw.vultr.preview' and n.get('text')==text);x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')));run(['shell','input','tap',str((x+z)//2),str((y+w)//2)])
def selected(text):return any(n.get('text')==text and n.get('checked')=='true' for n in dump().iter('node'))
launch(screen='设置',styleDialog=True,motion=True,style='pulse');assert selected('呼吸圆点');tap('微型机架');tap('应用');dump()
launch(screen='设置',styleDialog=True,motion=True);assert selected('微型机架'),'apply not persisted';tap('呼吸圆点');tap('取消')
launch(screen='设置',styleDialog=True,motion=True);assert selected('微型机架'),'cancel changed persisted style';tap('取消');print('PASS: apply persists across process restart; cancel retains rack',flush=True)
cases=[('pulse-light',{'style':'pulse','motion':True}),('rack-light',{'style':'rack','motion':True}),('rack-dark',{'style':'rack','motion':True,'dark':True}),('rack-off',{'style':'rack','motion':True,'off':True}),('rack-stale',{'style':'rack','motion':True,'stale':True}),('rack-failed',{'style':'rack','motion':True,'failed':True}),('rack-static',{'style':'rack','motion':False}),('rack-reduced',{'style':'rack','motion':True,'reduced':True}),('rack-large',{'style':'rack','motion':True,'font':2.0,'long':True}),('rack-landscape',{'style':'rack','motion':True,'landscape':True})]
reports=[]
for name,extras in cases:
 launch(screen='总览',**extras)
 if extras.get('landscape'):
  for _ in range(8):
   run(['shell','input','swipe','1100','720','1100','390','700'])
   if any(n.get('text')=='我的硅谷节点' for n in dump().iter('node')):break
 elif extras.get('long'):
  for _ in range(10):
   root=dump()
   if any(n.get('text','').startswith('这是一个用于') for n in root.iter('node')):break
   run(['shell','input','swipe','550','1700','550','1400','800'])
 else:run(['shell','input','swipe','550','1800','550','1100','1000'])
 root=dump();texts=[n.get('text','') for n in root.iter('node') if n.get('package')=='com.yjw.vultr.preview'];assert not any('STALE_CALLBACK_ERROR' in s for s in texts)
 spec=next((n.get('bounds') for n in root.iter('node') if n.get('text','').startswith('SJC ·')),None)
 name_bounds=next((n.get('bounds') for n in root.iter('node') if n.get('text','')=='我的硅谷节点'),None)
 def coords(x):return list(map(int,re.findall(r'\d+',x)))
 geometry=None
 if spec and name_bounds:
  a,c=coords(spec),coords(name_bounds);geometry=[a[0]-c[0],a[1]-c[1],a[2]-a[0],a[3]-a[1]]
 reports.append({'case':name,'spec_bounds':spec,'relative_geometry':geometry,'state_text':[s for s in texts if '运行中' in s or '已停止' in s or '等待更新' in s or '查询失败' in s]})
 (out/('indicator-'+name+'.png')).write_bytes(run(['exec-out','screencap','-p']))
 if name=='rack-light':assert reports[0]['relative_geometry']==geometry,'style changed card geometry'
 print('PASS:',name,flush=True)
(out/'indicator-report.json').write_text(json.dumps(reports,ensure_ascii=False,indent=2))
