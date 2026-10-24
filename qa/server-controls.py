"""Offline fictional-data package only. Never confirms a power mutation or touches the clipboard."""
import subprocess,xml.etree.ElementTree as E,re,shlex,json
from pathlib import Path
b=['adb'];out=Path(__file__).resolve().parent/'screens';report=[]
def r(a):return subprocess.run(b+a,capture_output=True,check=True).stdout
def dump():
 r(['shell','uiautomator','dump','/data/local/tmp/vultr-controls-qa.xml']);root=E.fromstring(r(['exec-out','cat','/data/local/tmp/vultr-controls-qa.xml']));assert any(n.get('package')=='com.yjw.vultr.preview' for n in root.iter('node')),'Preview is not foreground';return root

def find(text):
 for _ in range(18):
  root=dump();nodes=[n for n in root.iter('node') if n.get('package')=='com.yjw.vultr.preview' and (n.get('text')==text or n.get('content-desc','').startswith(text))]
  if nodes:return nodes[-1]
  view=next(n for n in root.iter('node') if n.get('class')=='android.widget.ScrollView');l,t,z,w=map(int,re.findall(r'\d+',view.get('bounds')));r(['shell','input','swipe',str((l+z)//2),str(t+int((w-t)*.8)),str((l+z)//2),str(t+int((w-t)*.55)),'600'])
 raise AssertionError('Missing control: '+text)
def tap(text):
 n=find(text);x,y,z,w=map(int,re.findall(r'\d+',n.get('bounds')));r(['shell','input','tap',str((x+z)//2),str((y+w)//2)])
def launch(**extra):
 args=['am','start','-S','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity','--ei','width','360'];values={'screen':'服务器','resetLogin':True,**extra}
 for key,value in values.items():args+=['--ez' if isinstance(value,bool) else '--ef' if isinstance(value,float) else '--es',key,str(value).lower() if isinstance(value,bool) else str(value)]
 r(['shell',shlex.join(args)]);dump()
def shot(name):
 raw=r(['exec-out','screencap','-p']);i=raw.find(b'\x89PNG\r\n\x1a\n');assert 0<=i<2048;(out/('controls-'+name+'.png')).write_bytes(raw[i:])
def passed(name):report.append({'case':name,'result':'PASS'});print('PASS:',name,flush=True)
launch();assert find('开机').get('enabled')=='false';assert find('关机').get('enabled')=='true';shot('running');tap('关机');find('确认关机？');root=dump();assert any('11111111-1111-4111-8111-111111111111' in n.get('text','') for n in root.iter('node'));shot('confirmation');tap('取消');passed('correct target confirmation and cancellation')
secret=find('主机登录密码');assert secret.get('text')=='Demo-Only-Password';assert secret.get('password')!='true';find('复制用户名');find('复制密码');shot('plaintext-login');passed('credentials visible immediately in plaintext with copy actions')
r(['shell','input','keyevent','KEYCODE_HOME']);log=r(['logcat','-d','-s','VultrControlsQA:I']).decode(errors='replace');assert 'PASS: login panel cleared on background' in log;passed('foreground plaintext removed on background');r(['shell','am','start','-n','com.yjw.vultr.preview/com.yjw.vultr.QaActivity']);assert find('主机登录密码').get('text')=='Demo-Only-Password';passed('plaintext restored automatically in foreground')
for name,extra,start,halt in [('stopped',{'off':True},'true','false'),('stale',{'stale':True},'false','false'),('dark',{'dark':True},'false','true'),('large',{'long':True,'font':2.0},'false','true'),('home',{'screen':'总览'},'false','true'),('detail',{'screen':'详情'},'false','true')]:
 launch(**extra);assert find('开机').get('enabled')==start;assert find('关机').get('enabled')==halt;shot(name);passed(name)
(out/'controls-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
